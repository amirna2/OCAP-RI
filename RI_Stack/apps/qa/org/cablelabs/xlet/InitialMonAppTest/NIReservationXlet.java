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

package org.cablelabs.xlet.InitialMonAppTest;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.NormalContentEvent;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import org.dvb.application.*;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.OcapTuner;
import org.cablelabs.lib.utils.XaitGen.*;

import org.ocap.net.OcapLocator;
import org.ocap.OcapSystem;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.AppManagerProxy;

public class NIReservationXlet implements Xlet, NetworkInterfaceListener, ServiceContextListener, ResourceClient,
        ResourceContentionHandler, KeyListener
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    private HScene m_scene;

    private static VidTextBox m_vbox;

    private static String SECTION_DIVIDER = "==================================";

    NetworkInterfaceController m_niCtrl;

    private ServiceContext m_serviceContext;

    private OcapTuner m_tuner;

    private int m_absSvcId = 0x2cafe1;

    private String m_absSvcName = "AutoSelect Service";

    private AppID m_unboundAppId;

    private String m_unboundAppClass = "org.cablelabs.xlet.InitialMonAppTest.InitialMonAppTestXlet";

    private String m_unboundAppName;

    Object NItuningSync = new Object();

    /**
     * Initializes the OCAP Xlet.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("NIResrvation : initXlet() - begin");

        // store off our xlet context
        m_ctx = ctx;

        ArgParser argParser = null;
        try
        {
            argParser = new ArgParser((String[]) (m_ctx.getXletProperty(XletContext.ARGS)));
        }
        catch (Exception e)
        {
            System.out.println("Failure to get ArgParser: " + e);
        }
        try
        {
            m_absSvcName = argParser.getStringArg("AbstractServiceName");
        }
        catch (Exception svcNameE)
        {
            System.out.println("Failure to get Abstract Service name: " + svcNameE);
        }
        try
        {
            m_absSvcId = argParser.getIntArg("AbstractServiceId");
        }
        catch (Exception svcIdE)
        {
            System.out.println("Failure to get Abstract Service Id: " + svcIdE);
        }
        try
        {
            m_unboundAppClass = argParser.getStringArg("UnboundAppClass");
        }
        catch (Exception appClassE)
        {
            System.out.println("Failure to get Unbound App Class: " + appClassE);
        }
        try
        {
            m_unboundAppName = argParser.getStringArg("UnboundAppName");
        }
        catch (Exception appNameE)
        {
            System.out.println("Failure to get Unbound App Name: " + appNameE);
            m_unboundAppName = m_unboundAppClass.substring(m_unboundAppClass.lastIndexOf("."));
        }
        try
        {
            String id = argParser.getStringArg("UnboundAppId");
            long orgIDappID = Long.parseLong(id.substring(2), 16);
            int oID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
            int aID = (int) (orgIDappID & 0xFFFF);
            m_unboundAppId = new AppID(oID, aID);
        }
        catch (Exception appIdE)
        {
            System.out.println("Failure to get unboundAppId: " + appIdE);
        }

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 280, 450, 200, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        print("abstractSvcName = " + m_absSvcName + " (" + m_absSvcId + ")");
        print("unbound app " + m_unboundAppName + ", id = " + m_unboundAppId.toString() + " (" + m_unboundAppClass
                + ")");

        System.out.println("[NIReservationXlet] : initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("[NIReservationXlet] : startXlet() - start");

        int niCount = reserveNIs();
        print(" Number of reserved NetworkInterfaces is " + niCount);
        m_niCtrl.getNetworkInterface().addNetworkInterfaceListener(this);

        ResourceContentionManager rcMgr = ResourceContentionManager.getInstance();
        rcMgr.setResourceContentionHandler(this);

        // service context
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            m_serviceContext = scf.createServiceContext();
        }
        catch (Exception svcCtxtE)
        {
            System.out.println("Caught exception trying to create servicecontext");
        }
        m_tuner = new OcapTuner(m_serviceContext);

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();

        System.out.println("[NIReservationXlet] : startXlet() - begin");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);
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
        m_scene.setVisible(false);
        m_scene.removeKeyListener(this);

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private int reserveNIs()
    {
        System.out.println("NIResrvation : reserveNIs() - begin");

        int reservedNICount = 0;
        NetworkInterfaceManager niMgr = NetworkInterfaceManager.getInstance();
        NetworkInterface nis[] = niMgr.getNetworkInterfaces();
        m_niCtrl = new NetworkInterfaceController(this);

        print("NIResrvation : reserveNIs() - number of NetworkInterfaces is " + nis.length);

        for (int i = 0; i < nis.length; i++)
        {
            print("Network Interface [" + i + "] is " + nis[i].toString());
            if (!nis[i].isReserved())
            {
                try
                {
                    m_niCtrl.reserve(nis[i], null);
                }
                catch (NetworkInterfaceException e)
                {
                    System.out.println("caught NetworkInterfaceException when attempting to to reserve interface " + i
                            + ".  Exception: " + e);
                    continue;
                }
                reservedNICount++;
            }
        }
        return reservedNICount;
    }

    /*
     * Implements NetworkInterfaceListener
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        print("Got a ServiceContextEvent");

        if (event instanceof NormalContentEvent)
        {
            print("\nReceived NormalContentEvent");
        }
        else if (event instanceof SelectionFailedEvent)
        {
            print("\nReceived SelectionFailedEvent");
        }
    }

    /*
     * Implements NetworkInterfaceListener
     */
    public void receiveNIEvent(NetworkInterfaceEvent evt)
    {
        print("Got a networkInterfaceEvent");
        if (evt instanceof NetworkInterfaceTuningOverEvent)
        {
            print("Received NetworkInterfaceTuningOverEvent");

            if (((NetworkInterfaceTuningOverEvent) evt).getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
            {
                print("\nReceived NetworkInterfaceTuningOverEvent.FAILED status");
            }
            else
            {
                print("Received NetworkInterfaceTuningOverEvent.SUCCEEDED status");
            }

            synchronized (NItuningSync)
            {
                NItuningSync.notifyAll();
            }
        }
    }

    /*
     * Implements ResourceClient
     */
    public void notifyRelease(ResourceProxy proxy)
    {
    }

    public void release(ResourceProxy proxy)
    {
    }

    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        if (m_niCtrl != null && proxy == m_niCtrl)
        {
            try
            {
                m_niCtrl.release();
            }
            catch (NetworkInterfaceException e)
            {
                System.out.println("[NIResrvation] :  caught NetworkInterfaceException trying to call NetworkInterfaceController.release() in requestRelease()");
            }
        }
        return true;
    }

    /*
     * Implements ResourceContentionHandler
     */
    public ResourceUsage[] resolveResourceContention(ResourceUsage req, ResourceUsage[] owners)
    {
        System.out.println("[NIResrvation] :  in resolveResourceContention()");
        System.out.println("Contention for " + req.getResourceNames()[0]);
        System.out.println("Requester is " + req.getAppID());

        for (int i = 0; i < owners.length; ++i)
        {
            System.out.println("\t current owner " + i + " is " + owners[i].getAppID());
        }

        return null;
    }

    public void resourceContentionWarning(ResourceUsage req, ResourceUsage[] currentReservations)
    {
        System.out.println("[NIResrvation] :  in reourceContentionWarning()");
    }

    /*
     * Implements KeyListener interface
     */
    public void keyTyped(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyPressed(KeyEvent e)
    {
        AppManagerProxy appMgrProxy = AppManagerProxy.getInstance();

        OcapLocator loc = null;
        try
        {
            loc = new OcapLocator(4002);
        }
        catch (Exception locatorE)
        {
            System.out.println("Failed to get Locator:" + locatorE);
        }

        int key = e.getKeyCode();

        switch (key)
        {
            case HRcEvent.VK_0:
                print("VK_0 pressed");

                synchronized (NItuningSync)
                {
                    try
                    {
                        m_niCtrl.tune(loc);
                    }
                    catch (Exception tuneE)
                    {
                        System.out.println("Failure to tune:" + tuneE);
                    }
                    print("Tuning to locator " + loc.toString());

                    try
                    {
                        NItuningSync.wait(60000);
                    }
                    catch (InterruptedException waitE)
                    {
                        System.out.println("caught Exception during wait:" + waitE);
                    }
                }

                break;

            case HRcEvent.VK_1:
                print("VK_1 pressed");
                try
                {
                    m_tuner.tune(loc);
                }
                catch (Exception svcCtxtSelectE)
                {
                    System.out.println("caught Exception during serviceContext.select:" + svcCtxtSelectE);
                }
                break;

            case HRcEvent.VK_2:
                print("VK_2 pressed");

                ServiceInfo svcInfo = new ServiceInfo(m_absSvcId, m_absSvcName, true);

                ApplicationInfo appInfo = new ApplicationInfo(m_unboundAppId, m_absSvcId);
                appInfo.setControlCode(OcapAppAttributes.AUTOSTART);
                appInfo.setPriority(255);
                appInfo.setStoragePriority(0);
                appInfo.setBaseDir("/");
                appInfo.setClassName(m_unboundAppClass);
                appInfo.setOCTransportSid(4002); // 0xfa2
                appInfo.setOCTransportComponent(36); // 0x24

                XAITGenerator gen = new XAITGenerator();
                gen.add(svcInfo);
                gen.add(appInfo);
                try
                {
                    print("about to register unbound APP");
                    appMgrProxy.registerUnboundApp(gen.generate());
                }
                catch (Exception ex)
                {
                    print("FAILED to register unbound APP");
                    System.out.println("Caught exception attempting to register unbound app using AppManagerProxy: "
                            + ex);
                }
                print("Successfully registered unbound APP");

                break;
        }
    }

    // printing function - allow messages to post in screen and log
    //
    private void print(String msg)
    {
        System.out.println("[NIReservationXlet] : " + msg);
        m_vbox.write("    " + msg);
    }

}
