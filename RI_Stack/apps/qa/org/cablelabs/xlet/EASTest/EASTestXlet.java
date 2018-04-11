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

/**
 * @author Cherie Kuo
 */
package org.cablelabs.xlet.EASTest;

import java.util.*;
import java.io.IOException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import javax.tv.service.selection.*;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEventAvailableEvent;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;
import org.havi.ui.HContainer;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;
import org.ocap.net.OcapLocator;
import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.SystemEventManager;
import org.ocap.ui.event.OCRcEvent;

import org.ocap.system.EASEvent;
import org.ocap.system.EASHandler;
import org.ocap.system.EASManager;
import org.ocap.system.EASListener;
import org.ocap.system.EASModuleRegistrar;
import org.ocap.hardware.Host;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.XletLogger;
import org.cablelabs.test.autoxlet.Monitor;

/*
 */
public class EASTestXlet implements EASListener, EASHandler, ServiceContextListener, KeyListener, Xlet
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    private String m_appName = "EASTestXlet";

    private static Logger m_log = null;

    boolean m_initialShown= false;

    private boolean m_TuneSequenceRunning = false;


    private String m_lastInput = "";
    private ServiceContextFactory m_scf = null;
    private ServiceContext m_svcCtxt = null;
    private Vector m_svcCtxtList = new Vector();
    private Vector m_svcList = new Vector();
    private int m_svcIndex = 0;
    private Monitor m_monitor = null;
   
    private int m_attributeCt = 7;

    private EASManager m_easMgr = null;
    private EASModuleRegistrar m_easModReg = null;
    private Host m_host = null;
    private int m_powerMode = Host.LOW_POWER;

    // keep track of when forced TuneEAS is active
    private boolean m_inEASMode = false;

    private HashMap m_fontColors = new HashMap();
    private HashMap m_backColors = new HashMap();
    private Vector m_fontSizes = new Vector();
    private Vector m_fontStyles = new Vector();
    private Vector m_fontFaces = new Vector();

    private String m_fontColor = "White";
    private String m_backColor = "Red";
    private String m_fontFace = "Tiresias";
    private String m_fontStyle = "PLAIN";
    private Float m_fontOpacity = new Float(1.0);
    private Float m_backOpacity = new Float(1.0);

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.microedition.xlet.Xlet#initXlet(javax.microedition.xlet.XletContext
     * )
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" +m_appName +"] : initXlet() - begin");

        m_ctx = ctx; // save my context

        m_log = new XletLogger();

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 10, 530, 240, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        m_scene.addComponentListener(new ComponentListener()
        {
            public void componentResized(ComponentEvent e)
            {
            }
            public void componentMoved(ComponentEvent e)
            {
            }
            public void componentShown(ComponentEvent e)
            {
                //only handle componentshown after we've been shown initially
                if (!m_initialShown)
                {
                    m_initialShown = true;
                }
                else
                {
                    showApp();
                }
            }
            public void componentHidden(ComponentEvent e)
            {
            }
        });


        SIManager siMgr = (SIManager)SIManager.createInstance();
        try 
        {
            m_svcList.add(0, siMgr.getService(new OcapLocator(0x44c)));
            m_svcList.add(1, siMgr.getService(new OcapLocator(0x45a)));

            //The following FPQ should be equivalent to srcID 0x5e9
            //but for some reason service selection with 0x5e9 always fails.
            m_svcList.add(2, siMgr.getService(new OcapLocator(651000000, 1, 16)));

        }
        catch (Exception ile)
        {
            print(" Failure - caught exception while populating serviceList: " +ile);
        }

        m_scf = ServiceContextFactory.getInstance();


        m_host = Host.getInstance();
        m_powerMode = m_host.getPowerMode();

        if (m_powerMode != Host.LOW_POWER)
        {
            debugLog(" initXlet() - host not in expected LOWER_POWER mode at startup");
        }

        // read the xlet arguments 
        try
        {
            ArgParser args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));
            m_fontColor = args.getStringArg("font_color").toUpperCase().replace(' ', '_');
            m_backColor = args.getStringArg("back_color").toUpperCase().replace(' ', '_');
            debugLog( " initXlet - user preferred font color is "+m_fontColor);
            debugLog( " initXlet - user preferred back color is "+m_backColor);

            m_fontOpacity = new Float(args.getStringArg("font_opacity"));
            m_backOpacity = new Float(args.getStringArg("back_opacity"));
            debugLog( " initXlet - user preferred font opacity is "+m_fontOpacity);
            debugLog( " initXlet - user preferred back opacity is "+m_backOpacity);

            m_fontStyle = args.getStringArg("font_style").toUpperCase();
            debugLog( " initXlet - user preferred font style is "+m_fontStyle);

            m_fontFace = args.getStringArg("font_face");
            if (m_fontFace.equalsIgnoreCase("SansSerif"))
            {
                m_fontFace = "SansSerif";
            }
            if (m_fontFace.equalsIgnoreCase("Serif"))
            {
                m_fontFace = "Serif";
            }
            if (m_fontFace.equalsIgnoreCase("Monospaced"))
            {
                m_fontFace = "Monospaced";
            }
            if (m_fontFace.equalsIgnoreCase("Dialog"))
            {
                m_fontFace = "Dialog";
            }
            if (m_fontFace.equalsIgnoreCase("DialogInput"))
            {
                m_fontFace = "DialogInput";
            }
            if (m_fontFace.equalsIgnoreCase("Tiresias"))
            {
                m_fontFace = "Tiresias";
            }
            debugLog( " initXlet - user preferred font face is "+m_fontFace);
        }
        catch (IOException ioe)
        {
            throw new XletStateChangeException("Error creating ArgParser!");
        }
        catch (Exception e)
        {
            debugLog("caught exception trying to read xlet arguments: "+e);
        }

        m_monitor = new Monitor();
        debugLog(" initXlet() - end");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.microedition.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        // get an instane of the EASManager
        try
        {
            m_easMgr = EASManager.getInstance();
            m_easModReg = EASModuleRegistrar.getInstance();
        }
        catch (Exception e)
        {
            print(" Error: caught exception trying to create easManager and/or easModuleRegistrar: " + e);
            throw new XletStateChangeException(m_appName + " could not get an instance of EASManager and/or EASModuleRegistrar");
        }

        m_easMgr.addListener(this);
        m_easModReg.registerEASHandler(this);

        print(m_appName); 

        showApp();

        debugLog(" startXlet() - end");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        debugLog(" pauseXlet - begin");

        m_scene.setVisible(false);

        debugLog(" pauseXlet - end");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.microedition.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        debugLog(" destroyXlet - begin");
        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
        HSceneFactory.getInstance().dispose(m_scene);
    }


    /*
     * EASListener
     */
    public void notify(EASEvent event)
    {
        print("(" +translatePower(m_host.getPowerMode()) +") notify() - "+translateState(m_easMgr.getState()) +":" +translateReason(event.getReason()));

        m_inEASMode = false;
        if (event.getReason() == EASEvent.EAS_DETAILS_CHANNEL &&
            m_easMgr.getState() != EASManager.EAS_NOT_IN_PROGRESS_STATE)
        { 
            m_inEASMode = true;
        } 

        if (event.getReason() == EASEvent.EAS_COMPLETE &&
            m_easMgr.getState() != EASManager.EAS_NOT_IN_PROGRESS_STATE)
        {
            print("FAILURE - EASListener.notify(): EAS is complete but EASManager is not in EAS_MESSAGE_NOT_IN_PROGRESS_STATE");
        }
    }

    public void warn(EASEvent event)
    {
        print("! (" +translatePower(m_host.getPowerMode()) +") warn() - "+translateState(m_easMgr.getState()) +":" +translateReason(event.getReason()));

        m_inEASMode = false;
        if (event.getReason() == EASEvent.EAS_DETAILS_CHANNEL &&
            m_easMgr.getState() != EASManager.EAS_MESSAGE_RECEIVED_STATE)
        { 
            m_inEASMode = true;
        } 

        if (m_host.getPowerMode() != Host.FULL_POWER)
        {
            print("FAILURE - in EASListener.warn() when NOT in Full_Power mode");
        }
        if (event.getReason() != EASEvent.EAS_DETAILS_CHANNEL)
        {
            print("Is the EAS an Audio alert?");
        }

        if (m_easMgr.getState() != EASManager.EAS_MESSAGE_RECEIVED_STATE)
        {
            print("FAILURE - in EASListener.warn() when not in EAS_MESSAGE_RECEIVED_STATE");
        }

    }


    /*
     * EASHandler
     */
    public boolean notifyPrivateDescriptor(byte[] descriptor)
    {
        print("In notifyPrivateDescriptor()");
        if (descriptor == null)
        {
            print("In notifyPrivateDescriptor, descriptor is null");
        }
        else if (descriptor.length <= 0 )
        {
            print("In notifyPrivateDescriptor, descriptor empty");
        }
        else
        {
            StringBuffer descriptorHexStr = new StringBuffer("0x");
            for (int i = 0; i < descriptor.length; i++)
            {
                descriptorHexStr.append(Integer.toHexString(descriptor[i]+0x200).substring(1));
            }
            print("In notifyPrivateDescriptor, descriptor = "+descriptorHexStr.toString());
        }
        
        return false;
    }

    public void stopAudio()
    {
        print("In stopAudio()");
    }
   

    /*
     * ServiceContextListener
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        debugLog("receiveServiceContextEvent: "+event.toString());

        if (event instanceof SelectionFailedEvent)
        {
            debugLog("SelectionFailedEvent reason="+((SelectionFailedEvent)event).getReason());
        }
        if (event instanceof PresentationTerminatedEvent)
        {
            debugLog("PresentationTerminatedEvent reason="+((PresentationTerminatedEvent)event).getReason());
        }

        if (event instanceof ServiceContextDestroyedEvent)
        {
            if (m_lastInput.equals("destroy")) 
            {
                m_svcCtxt = null;
                m_monitor.notifyReady();
            }
        }

        if (event instanceof PresentationChangedEvent)
        {
            if (m_lastInput.equals("select"))
            {
                m_monitor.notifyReady(); 
            }
        }
    }


    /*
     * KeyListener
     */
    public void keyReleased(KeyEvent ke)
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        debugLog("keyPressed: " +e.getKeyText(key) +"(" +key +")");
 
        switch(key)
        {
            case HRcEvent.VK_GUIDE:

                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);

                            for (int i = 0; i < 5; i++)
                            {
                                debugLog("continuous tune #" +i);
                                selectSvc(false);
                                pause(15000);
                            }
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_GUIDE key press");
                }

                break;

            case OCRcEvent.VK_MENU:

                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);

                            for (int i = 0; i < 5; i++)
                            {
                                debugLog("continuous tune #" +i);
                                selectSvc(true);
                                pause(15000);
                            }
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_GUIDE key press");
                }

                break;

            case HRcEvent.VK_ENTER:
                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);
                            selectEASSvc();
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_CHANNEL key press");
                }
                break;

            case HRcEvent.VK_CHANNEL_UP:
            case HRcEvent.VK_CHANNEL_DOWN:
                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);
                            selectSvc(false);
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_CHANNEL key press");
                }
                break;

            case HRcEvent.VK_VOLUME_UP:
            case HRcEvent.VK_VOLUME_DOWN:
                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);
                            selectSvc(true);
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_VOLUME key press");
                }
                break;

            case HRcEvent.VK_0: 
                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);
                            destroySvcCtxts();
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_0 key press");
                }
                break;

            case HRcEvent.VK_8:
                if (!m_TuneSequenceRunning)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            m_TuneSequenceRunning = true;
                            pause(10000);
                            stopSvcCtxt();
                            m_TuneSequenceRunning = false;
                        }
                    }.start();
                }
                else
                {
                    debugLog("Tune sequence already running -- ignoring VK_8 key press");
                }
                break;

            case HRcEvent.VK_1:
                changeEASColor();
                break;

            case HRcEvent.VK_2:
                changeEASFont();
                break;

            case HRcEvent.VK_4:
                changeEASOpacity();
                break;

            case HRcEvent.VK_3:
                changeEASSize(true);
                break;

            case HRcEvent.VK_6:
                changeEASSize(false);
                break;

            case HRcEvent.VK_INFO:
                printEASCaps();
                break;

            case HRcEvent.VK_POWER:
                m_powerMode = m_host.getPowerMode();

                print(" powerMode=" +translatePower(m_host.getPowerMode()) +" : EAS state="+translateState(m_easMgr.getState())); 

                printEASAttributes();

                break;

            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent arg0)
    {
        // NO-Op
    }


    public void stopSvcCtxt() 
    {
        debugLog(" stopSvcCtxt() - begin");
        m_svcCtxt.stop();
    }

    public void destroySvcCtxts() 
    {
        debugLog("destroySvcCtxts() - begin");
        m_lastInput = "destroy";
        
        for (int i = 0; i < m_svcCtxtList.size(); i++)
        {
            ServiceContext sc = (ServiceContext)m_svcCtxtList.get(i);
            try
            {
                synchronized(m_monitor) 
                {
                    sc.destroy();
                    m_monitor.waitForReady();
                }
            }
            catch(IllegalStateException ile) 
            {
                print((m_inEASMode? 
                   " ServiceContext.destroy() is correctly blocked while EAS_DETAILS_CHANNEL is in progress: " : 
                   " Failure - EAS_DETAILS_CHANNEL NOT in progress yet caught IllegalStateException with ServiceContext.destroy(): " ) +
                  ile.getMessage());
            }
            catch(Exception e) 
            {
                print(" Caught exception while trying to destroy SvcContext: " +e);
            }
        }
    }

    public void createSvcCtxt() 
    {
        try
        {
            m_svcCtxt = m_scf.createServiceContext();
            m_svcCtxt.addListener(this);
            if (!m_svcCtxtList.contains(m_svcCtxt))
            { 
                m_svcCtxtList.add(m_svcCtxt);
            }
        }
        catch (Exception e)
        {
            print("Error creating service context: "+e);
        }
    }

    public void selectSvc(boolean useNewSvcCtxt) 
    {
        debugLog("selectSvc() - begin");
        if (useNewSvcCtxt)
        {
            destroySvcCtxts();
            debugLog("selectSvc, about to create new SvcCtxt as requested");
            createSvcCtxt();
        }

        if (m_svcCtxt == null)
        {
            debugLog(" selectSvc, about to create SvcCtxt because there isn't one yet");
            createSvcCtxt();
        }

        try
        {
            m_lastInput = "select";
            synchronized(m_monitor)
            {
                m_svcCtxt.select((Service)m_svcList.get(m_svcIndex));
                m_monitor.waitForReady();
            }
        }
        catch (IllegalStateException ile)
        {
            print((m_inEASMode? 
                   " Service selection is correctly blocked while EAS_DETAILS_CHANNEL is in progress: " : 
                   " Failure - EAS_DETAILS_CHANNEL NOT in progress yet caught IllegalStateException with ServiceContext.select(): " ) +
                  ile.getMessage());
        }
        catch (Exception e)
        {
            print("selectSvcContext trew exception "+e);
            print(" Failure - caught exception while selecting service with " +(useNewSvcCtxt? "new" : "existing") +" ServiceContext: "+e);
        }

        m_svcIndex = (m_svcIndex == 2 ? 0 : 2);
    }


    public void selectEASSvc() 
    {
        if (m_svcCtxt == null)
        {
            createSvcCtxt();
        }
        
        try
        {
            m_lastInput = "select";
            synchronized(m_monitor)
            {
                m_svcCtxt.select((Service)m_svcList.get(1));
                m_monitor.waitForReady();
            }
        }
        catch (Exception e)
        {
            print(" Failure - caught exception while selecting service with new ServiceContext: "+e);
        }

        m_svcIndex = 2;
    }


    public void changeEASColor()
    {
        getEASCaps();

        Color fontColor = (Color)m_fontColors.get(m_fontColor);
        Color backColor = (Color)m_backColors.get(m_backColor);
 
        if (fontColor == null || backColor == null)
        {
            print("requested color is not supported, no changes made");
            return;
        }

        int[] attrsToChange = {
            EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR,
            EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR
        };
        Object[] values = {fontColor, backColor};

        print("  changing EAS font color to " +translateVal(values[0].toString()));
        print("  changing EAS back color to " +translateVal(values[1].toString()));
      
        int colorDiff = Math.abs(fontColor.getRed() - backColor.getRed()) +
            Math.abs(fontColor.getGreen() - backColor.getGreen()) +
            Math.abs(fontColor.getBlue() - backColor.getBlue());

        int brightnessDiff = ((fontColor.getRed()*299 + fontColor.getGreen()*587 + fontColor.getBlue()*114) / 1000) - 
            ((backColor.getRed()*299 + backColor.getGreen()*587 +backColor.getBlue()*114) / 1000);

        try 
        {
            m_easModReg.setEASAttribute(attrsToChange, values);
            if (colorDiff <= 500 || brightnessDiff <= 125)
            {
                print("    FAILURE!!! - IllegalArgumentException should have been thrown because the new colors does not meet accessibility code");
            }
        }
        catch (IllegalArgumentException iae) 
        {
            if (colorDiff > 500 && brightnessDiff > 125)
            {
                print("    FAILURE!!! - caught unexpected IllegalArgumentException: "+iae.getMessage());
            }
        }
        catch (Exception e) 
        {
            print("    FAILURE!!! - caught unexpected Exception: "+e);
        }

        printEASAttributes();
    }

    public void changeEASFont()
    {
        getEASCaps();

        int[] attrsToChange = {
            EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE,
            EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE
        };
        Object[] values = {m_fontStyle, m_fontFace};

        print("  changing EAS font to " +values[0] +" " +values[1]);
      
        try 
        {
            m_easModReg.setEASAttribute(attrsToChange, values);
            if (!m_fontStyles.contains(values[0]) || !m_fontFaces.contains(values[1])) 
            { 
                print("    FAILURE!!! - IllegalArgumentException should have been thrown because the font style/face is unavailable");
            } 
        }
        catch (IllegalArgumentException iae) 
        {
            if (m_fontStyles.contains(values[0]) && m_fontFaces.contains(values[1])) 
            {
                print("    FAILURE!!! - caught unexpected IllegalArgumentException: "+iae.getMessage());
            }
        }
        catch (Exception e) 
        {
            print("    FAILURE!!! - caught unexpected Exception: "+e);
        }

        printEASAttributes();
    }

    public void changeEASOpacity()
    {
        int[] attrsToChange = {
            EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY,
            EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY
        };
        Object[] values = {m_fontOpacity, m_backOpacity};

        print("  changing EAS font opacity to " +values[0] +" (user requested " +m_fontOpacity +")  and back opacity to " +values[1] +" (user requested " +m_backOpacity);
      
        try 
        {
            m_easModReg.setEASAttribute(attrsToChange, values);
        }
        catch (Exception e) 
        {
            print("    FAILURE!!! - caught unexpected Exception: "+e);
        }

        printEASAttributes();
    }

    public void changeEASSize(boolean enlarge)
    {
        getEASCaps();


        int[] attrsToChange = {EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE};

        Integer currentSize = (Integer)m_easModReg.getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE);
        int index = m_fontSizes.indexOf(currentSize);
        if (enlarge)
        {
            index++;
            if (index >= m_fontSizes.size())
            {
                index = m_fontSizes.size() - 1;
            }
        }
        else
        {
            index--;
            if (index <= 0)
            {
                index = 0;
            }
        }
        Object[] values = {m_fontSizes.get(index)};

        print("  changing EAS font size from " +currentSize.intValue() +" to " +values[0]);
      
        try 
        {
            m_easModReg.setEASAttribute(attrsToChange, values);
        }
        catch (Exception e) 
        {
            print("    FAILURE!!! - caught unexpected Exception: "+e);
        }

        printEASAttributes();
    }


    public void printEASAttributes()
    {
        String attrStr = null; 

        print("    EAS Attributes:");

        for (int i = 1; i <= m_attributeCt; i++)
        {
            attrStr = translateAttr(i);
            try
            {
                String attrVal = translateVal(m_easModReg.getEASAttribute(i));
                print("      " +attrStr +" = " +attrVal);
            }
            catch(Exception e)
            {
                debugLog(" printEASAttributes() - caught exception trying to get attributeof " +attrStr +":" +e);
            }
        }
    }


    public void getEASCaps()
    {
        m_fontSizes.removeAllElements();
        m_fontStyles.removeAllElements();
        m_fontFaces.removeAllElements();

        for (int i = 1; i <= m_attributeCt; i++)
        {
            if (i == EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY ||
                i == EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY)
            {
                continue;
            }

            Object[] attrObjs = null;
            try
            {
                attrObjs = m_easModReg.getEASCapability(i);
            }
            catch(Exception e)
            {
                debugLog(" getEASCaps() - caught exception getting the capabilities of " +translateAttr(i)+":" +e);
            }


            for (int j = 0; j < attrObjs.length; j++)
            {
                switch (i)
                {
                    case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR:
                        m_fontColors.put(translateVal(attrObjs[j]), attrObjs[j]);
                        break;

                    case EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR:
                        m_backColors.put(translateVal(attrObjs[j]), attrObjs[j]);
                        break;

                    case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE:
                        m_fontFaces.add(attrObjs[j]);
                        break;

                    case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE:
                        m_fontStyles.add(attrObjs[j]);
                        break;

                    case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE:
                        m_fontSizes.add(attrObjs[j]);
                        break;
                }
            }
        }
    }

    public void printEASCaps()
    {
        getEASCaps();

        print("=============================================");

        print("(" +translatePower(m_host.getPowerMode()) +") - " +translateState(m_easMgr.getState()));

        print(" EAS Capabilities:");
        for (int i = 1; i <= m_attributeCt; i++)
        {
            String availableVals = "        ";
            switch (i)
            {
                case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR:
                    availableVals += m_fontColors.size() +" available values: ";
                    Iterator iter = (m_fontColors.keySet()).iterator();
                    availableVals += (String)iter.next() +", ";
                    while (iter.hasNext())
                    {
                        availableVals += (String)iter.next() +", ";
                    }
                    break;

                case EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR:
                    availableVals += m_backColors.size() +" available values: ";
                    Iterator bIter = (m_backColors.keySet()).iterator();
                    availableVals += (String)bIter.next() +", ";
                    while (bIter.hasNext())
                    {
                        availableVals += (String)bIter.next() +", ";
                    }
                    break;

                case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE:
                    availableVals += m_fontFaces.size() +" available values: ";
                    for (int j = 0; j < m_fontFaces.size(); j++)
                    {
                        availableVals += m_fontFaces.get(j) +", ";
                    } 
                    break;

                case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE:
                    availableVals += m_fontStyles.size() +" available values: ";
                    for (int j = 0; j < m_fontStyles.size(); j++)
                    {
                        availableVals += m_fontStyles.get(j) +", ";
                    } 
                    break;

                case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE:
                    availableVals += m_fontSizes.size() +" available values: ";
                    for (int j = 0; j < m_fontSizes.size(); j++)
                    {
                        availableVals += m_fontSizes.get(j) +", ";
                    } 
                    break;
            }
            print(availableVals);
        }
        print("=============================================");
    }


    public String translateVal(Object val)
    {
        if (!(val instanceof java.awt.Color))
        {
            return val.toString();
        }

        int redComp = ((Color)val).getRed();
        int greenComp = ((Color)val).getGreen();
        int blueComp = ((Color)val).getBlue();
        if (greenComp == 0 && blueComp == 0 && redComp != 0)
        {
            return "RED";
        }
        if (greenComp != 0 && blueComp == 0 && redComp == 0)
        {
            return "GREEN";
        }
        if (greenComp == 0 && blueComp != 0 && redComp == 0)
        {
            return "BLUE";
        }
        if (greenComp == blueComp && greenComp != 0 && redComp == 0)
        {
            return "CYAN";
        }
        if (redComp == blueComp && redComp != 0 && greenComp == 0)
        {
            return "MAGENTA";
        }
        if (redComp == greenComp && redComp != 0 && blueComp == 0)
        {
            return "YELLOW";
        }
        if (redComp > greenComp && blueComp == 0)
        {
            return "ORANGE";
        }
        if (redComp > greenComp && greenComp == blueComp)
        {
            return "PINK";
        }
        if (redComp == 255 && greenComp == 255 && blueComp == 255)
        {
            return "WHITE";
        }
        if (redComp == 192 && greenComp == 192 && blueComp == 192)
        {
            return "LIGHT_GRAY";
        }
        if (redComp == greenComp && greenComp == blueComp)
        {
            if (redComp == 0) return "BLACK";
            if (redComp < 65) return "DARK_GRAY";
            if (redComp < 130) return "GRAY";
            if (redComp > 175) return "WHITE";
            return "LIGHT_GRAY";
        }

        return val.toString();
    }


    public String translateState(int event)
    {
        if (event == 0)
        {
            return "EAS_MSG_RECEIVED";
        } 
        if (event == 1)
        {
            return "EAS_MSG_IN_PROGRESS";
        } 
        return "EAS_NOT_IN_PROGRESS";
    }

    public String translateReason(int event)
    {
        if (event == 1)
        {
            return "EAS_DETAILS_CHANNEL";
        } 
        if (event == 2)
        {
            return "EAS_TEXT_DISPLAY";
        } 
        return "EAS_COMPLETE";
    }

    public String translatePower(int mode)
    {
        if (mode == 2)
        {
            return "Low_power";
        }
        return "Full_power";
    }

    public String translateAttr(int attr)
    {
        switch (attr)
        {
            case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR:
                return "EAS_ATTRIBUTE_FONT_COLOR";

            case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE:
                return "EAS_ATTRIBUTE_FONT_STYLE";

            case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE:
                return "EAS_ATTRIBUTE_FONT_FACE";

            case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE:
                return "EAS_ATTRIBUTE_FONT_SIZE";

            case EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR:
                return "EAS_ATTRIBUTE_BACK_COLOR";

            case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY:
                return "EAS_ATTRIBUTE_FONT_OPACITY";

            case EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY:
                return "EAS_ATTRIBUTE_BACK_OPACITY";

            default:
                return "UNKNOWN attribute "+attr;
        }
    }


    // logging function - allow messages to post to teraterm and autoxlet logs
    //
    private void debugLog(String msg)
    {
        m_log.log("[" + m_appName + "] :" + msg);
    }

    //
    // printing function - allow messages to post in screen and log
    //
    private void print(String msg)
    {
        m_log.log("\t" + msg);
        m_vbox.write("    " + msg);
    }

    private void pause(int interval)
    {
        try
        {
            Thread.sleep(interval);
        }
        catch (Exception e)
        {
        }
    }

    void showApp()
    {
        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();
    }
}
