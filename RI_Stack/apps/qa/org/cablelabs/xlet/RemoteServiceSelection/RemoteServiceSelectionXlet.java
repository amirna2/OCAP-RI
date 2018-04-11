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
package org.cablelabs.xlet.RemoteServiceSelection;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Component;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.rmi.RemoteException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.Controller;
import javax.media.StopEvent;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.DVBColor;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.service.RemoteService;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.XletLogger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.xlet.DvrExerciser.Bargraph;

public class RemoteServiceSelectionXlet implements Xlet, Driveable, KeyListener
{
    // cds query arguments
    private boolean browseDirectChildren = true;

    private int startingIndex = 0;

    private int requestedCount = 5;

    private String parentId = "0";

    private String propertyFilter = "*";

    private String sortCriteria = "";

    // player and rendering
    private HScene scene;

    private Player player;

    boolean initialized;

    // config file parameters
    private String inetAddress; // arg 0

    private String serverName; // arg 1

    private String path; // optional, arg 2

    private String title; // optional, arg 3

    // service
    private ServiceContext serviceContext;

    private Service services[] = new Service[9];

    private Service service;

    // private RemoteService remoteService;
    // private Locator remoteServiceLocator;
    private Locator serviceLocator;

    private Locator resourceLocator;

    private float requestedRate;

    // test UI
    private VidTextBox vidTextBox;

    // private boolean stopped;
    private boolean playing = false;

    private StatusBox statusBox = null;

    private StatusBox titleBox = null;

    private Bargraph playbackIndicator;

    // AutoXlet
    private Monitor eventMonitor = null;

    private AutoXletClient _axc = null;

    private Logger _log = null;

    private Monitor _repeatTuneCleanupMonitor = null;

    private Test _test = null;
    
    private ServiceContextListenerImpl scListener = null;
    
    private ControllerListener cListener = null;
    
    // screen dimensions
    private static final int SCREEN_WIDTH = 640;

    private static final int SCREEN_HEIGHT = 480;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("******************* remoteserviceselection initXlet starting");
        String[] args = (String[]) ctx.getXletProperty(XletContext.ARGS);
        inetAddress = args[0];
        serverName = args[1];
        if (args.length > 2)
        {
            path = args[2];
        }
        if (args.length > 3)
        {
            title = args[3];
        }

        eventMonitor = new Monitor();
        // Initialize AutoXlet
        _axc = new AutoXletClient(this,ctx);
        _test = _axc.getTest();
        
        if (_axc.isConnected())
        {
            _log = _axc.getLogger();
        }
        else
        {
            _log = new XletLogger();
        }
        _log.log("RemoteServiceSelectionXlet autoxlet logger has been initialized.");


        /*
         * Set up video graphics for the application Establish self as RC key
         * listener
         */
        System.out.println("Setting up key listener and havi interface");
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());

        vidTextBox = new VidTextBox(50, 100, 330, 230, 14, 5000);
        vidTextBox.setBackground(new DVBColor(128, 128, 128, 155));
        vidTextBox.setForeground(new DVBColor(200, 200, 200, 255));
        vidTextBox.write("Select Recording by remote numeric key: ");
        vidTextBox.setVisible(false);

        titleBox = new StatusBox(0, 0, 640, 30);
        titleBox.update(StatusBox.COLOR_INIT, "    Remote Service Selection Xlet");

        statusBox = new StatusBox(30, 40, 580, 30);
        statusBox.update(StatusBox.COLOR_INIT, StatusBox.MSG_INIT);
        statusBox.setVisible(true); // Show startup status

        scene.add(titleBox);
        scene.add(statusBox);
        scene.add(vidTextBox);
        scene.addKeyListener(this);
        // scene.addKeyListener(vidTextBox);

        playbackIndicator = new Bargraph();
        scene.add(playbackIndicator);
        playbackIndicator.setBounds(30, 70, 580, 10);
        playbackIndicator.setVisible(false);

        scene.validate();

        // This thread is used to force a periodic paint of the video display -
        // when in the paused mode during playback, the paint method ceases
        // to be called. This can result in a situation where the displayed
        // play rate does not correspond to the value that the stack is
        // operating against.
        new Thread()
        {
            public void run()
            {
                for (;;)
                {
                    if (playing)
                    {
                        Time duration = player.getDuration();
                        Time mediaTime = player.getMediaTime();
                        if (duration.getNanoseconds() > 0)
                        {
                            float completionRatio = ((float) (mediaTime.getSeconds())) / (float) duration.getSeconds();
                            playbackIndicator.setCompletionRatio(completionRatio);
                            // System.out.println("Updating playback indicator with ratio: "
                            // + completionRatio +
                            // ", media time: " + mediaTime.getSeconds() +
                            // ", duration: " + duration.getSeconds());
                            playbackIndicator.repaint();
                        }
                        else
                        {
                            // System.out.println("Not updating playback indicator due to 0 duration");
                        }
                    }
                    else
                    {
                        // System.out.println("Not updating playback indicator due to not playing");
                    }
                    scene.repaint();

                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException ex)
                    {
                        // TODO Auto-generated catch block
                        ex.printStackTrace();
                    }
                }
            }
        }.start();

        // Remove any previous listeners from previous service contexts
        if ((serviceContext != null) && (scListener != null))
        {
            serviceContext.removeListener(scListener);
            scListener = null;
        }
        
        /*
         * Create a Service Context to display video
         */
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            serviceContext = scf.createServiceContext();
        }
        catch (InsufficientResourcesException e)
        {
            e.printStackTrace();
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (ServiceContextException e)
        {
            throw new XletStateChangeException("Unable to get service context: " + e.getMessage());
        }
        scListener = new ServiceContextListenerImpl();
        serviceContext.addListener(scListener);

        System.out.println("******************* remoteserviceselection initXlet - inetAddress: " + inetAddress
                + " serverName: " + serverName);
    }

    public void startXlet() throws XletStateChangeException
    {
        System.out.println("remoteserviceselection startXlet");

        scene.show();
        scene.requestFocus();
        statusBox.update(StatusBox.COLOR_PENDING, 
                "Looking for Server@" + inetAddress);

        HNUtil hnUtil = new HNUtil();
        ContentServerNetModule contentServerNetModule = hnUtil.getContentServerNetModuleByNameAndAddress(serverName,
                inetAddress);
        if (contentServerNetModule == null)
        {
            int tryCnt = 0;
            int maxTries = 10;
            do
            {
                tryCnt++;

                try
                {
                    Thread.sleep(3000);
                }
                catch (Exception e)
                {
                    // ignore exception
                }
                // Sleep for a while prior to trying again
                contentServerNetModule = hnUtil.getContentServerNetModuleByNameAndAddress(serverName, inetAddress);
                statusBox.update(StatusBox.COLOR_PENDING, "Looking for Server@" + inetAddress + ", Try " + (tryCnt + 1)
                        + " of " + maxTries);
                scene.repaint();
            }
            while ((contentServerNetModule == null) && (tryCnt < maxTries));

            if (contentServerNetModule == null)
            {
                System.out.println("ContentServerNetModule Not Found after " + maxTries + " tries");
                statusBox.update(StatusBox.COLOR_STOP, "Unable to find Server@" + inetAddress);
                scene.repaint();
                throw new XletStateChangeException("Unable to find ContentServerNetModule");
            }
        }
        System.out.println("ContentServerNetModule Found: " + "with ip addr: "
                + contentServerNetModule.getDevice().getInetAddress().getHostAddress() + ", uuid = "
                + contentServerNetModule.getDevice().getProperty("UDN"));

        statusBox.update(StatusBox.COLOR_PENDING, "Looking for services");
        scene.repaint();

        int idx = 0;

        // ContentList contentList =
        // hnUtil.browseEntries(contentServerNetModule, parentId,
        // propertyFilter, browseDirectChildren, startingIndex, requestedCount,
        // sortCriteria);
        // System.out.println("********* Calling hnUtil.browseEntries(contentServerNetModule, "
        // + parentId + ", " + propertyFilter + ", " + browseDirectChildren +
        // ", " + startingIndex + ", " + requestedCount + ", " + sortCriteria +
        // ")");
        ContentList contentList = hnUtil.searchRootContainer(contentServerNetModule);
        if (contentList == null)
        {
            statusBox.update(StatusBox.COLOR_STOP, "No Content Available on Server");
            scene.repaint();
            throw new XletStateChangeException("Failed to get content list from: " + contentServerNetModule);
        }
        else
        {
            System.out.println("********* Received ContentList. ***********");
            System.out.println("********* ContentList Size: " + contentList.size() + " ***********");
            while ((contentList.hasMoreElements()) && (idx < 8))
            {
                ContentEntry contentEntry = (ContentEntry) contentList.nextElement();
                System.out.println("********* Investigating ContentEntry ID/class: " + contentEntry.getID() + "/"
                        + contentEntry.getClass().getName());
                if (contentEntry instanceof ContentContainer)
                {
                    System.out.println("********* FOUND CONTAINER ************");
                    System.out.println("********* CONTAINER NAME: " + ((ContentContainer) contentEntry).getName()
                            + " ************");
                    System.out.println("********* Moving on to next ContentEntry");
                    continue;
                }
                else if (contentEntry instanceof ContentItem)
                {
                    System.out.println("********* FOUND CONTENT ITEM ************");
                    System.out.println("********* ITEM NAME: " + ((ContentItem) contentEntry).getTitle()
                            + " ************");
                    ContentItem contentItem = (ContentItem) contentEntry;
                    services[idx] = contentItem.getItemService();
                    if (null == services[idx])
                    {
                        System.out.println("No remote service associated with content item");
                    }
                    else if (services[idx] instanceof RemoteService)
                    {
                        System.out.println("Service is an instance of RemoteService");
                        // remoteService = (RemoteService)service;
                        // remoteServiceLocator = remoteService.getLocator();
                        vidTextBox.setVisible(true);
                        vidTextBox.write((idx + 1) + " - Recording " + (idx + 1) + ", use Export id = "
                                + contentEntry.getID());
                        idx++;
                    }
                    else
                    {
                        System.out.println("Service is NOT an instance of RemoteService, class = "
                                + services[idx].getClass().getName());
                    }
                    System.out.println("Set service to contentItem.getItemService() = " + service);
                    if (services[idx] != null)
                    {
                        serviceLocator = services[idx].getLocator();
                        System.out.println("********* CONTENT ITEM HAS A SERVICE LOCATOR: " + service + ", locator: "
                                + (serviceLocator == null ? null : serviceLocator.toExternalForm()) + " ************");
                        if (serviceLocator != null)
                        {
                            StringTokenizer tokens = new StringTokenizer(serviceLocator.toExternalForm(), ":");
                            try
                            {
                                tokens.nextToken(); // protocol
                                tokens.nextToken(); // UUID prefix
                                tokens.nextToken(); // uuid itself
                                tokens.nextToken(); // ContentId
                                String resID = tokens.nextToken(); // Resource
                                                                   // ID
                                ContentResource contentResource = contentItem.getResource(0);
                                if (contentResource != null)
                                {
                                    resourceLocator = contentResource.getLocator();
                                    System.out.println("********* RESOURCE LOCATOR FROM RESOURCE: " + contentResource
                                            + ": "
                                            + (resourceLocator == null ? null : resourceLocator.toExternalForm())
                                            + " ************");
                                }
                                else
                                {
                                    System.out.println("********* UNABLE TO FIND CONTENT RESOURCE for: "
                                            + serviceLocator.toExternalForm() + " ************");
                                }
                            }
                            catch (NoSuchElementException e)
                            {
                                System.out.println("Ran out of tokens on " + serviceLocator.toExternalForm());
                            }
                        }
                    }
                    if (serviceLocator == null)
                    {
                        System.out.println("********* CONTENT ITEM DOES NOT HAVE A SERVICE LOCATOR - service: "
                                + contentItem.getItemService() + " , RESOURCES LENGTH: "
                                + contentItem.getResources().length + " ************");
                        // NOTE: getRenderableResources was zero-length, had to
                        // use getResources
                        if (contentItem.getResources().length > 0)
                        {
                            resourceLocator = contentItem.getResources()[0].getLocator();
                            System.out.println("********* RESOURCE INDEX-ZERO LOCATOR: "
                                    + (resourceLocator == null ? null : resourceLocator.toExternalForm())
                                    + " ************");
                        }
                        else
                        {
                            System.out.println("********* NO RESOURCES FOR CONTENT ITEM ************");
                        }
                    }
                }
                else if (contentEntry instanceof NetRecordingEntry)
                {
                    System.out.println("********* FOUND NET RECORDING ENTRY  ************");
                    System.out.println("********* RECORDING ENTRY ID: " + contentEntry.getID() + " ************");
                }
                else
                {
                    System.out.println("********* ERROR - NO ITEM OR CONTAINER FOUND ************");
                }
            }
            if (resourceLocator == null && services[0] == null)
            {
                // we didn't find a content item with a service or resource with
                // locator
                statusBox.update(StatusBox.COLOR_STOP, "No Content Available on Server");
                scene.repaint();
                throw new XletStateChangeException("no resource locator or service found");
            }

            /*
             * Request UI keys
             */
            System.out.println("RemoteServiceSelectionXlet:startXlet() finished");
            if (idx > 0)
            {
                // Found at least one recording so show video selection list
                // first
                // and hide status bar
                statusBox.setVisible(false);
            }
            statusBox.update(StatusBox.COLOR_INIT, StatusBox.MSG_INIT);
            scene.repaint();
        }
    }

    private void compareLocators()
    {
        if (serviceLocator != null && serviceLocator.equals(resourceLocator))
        {
            // vidTextBox.write("Locators match");
        }
        else
        {
            // vidTextBox.write("Locators failed to match - serviceLocator: " +
            // serviceLocator + ", resource locator: " + resourceLocator);
        }
    }

    private void playStandaloneUsingResourceLocator()
    {
        if (resourceLocator != null)
        {
            playStandalone(resourceLocator);
        }
        else
        {
            // vidTextBox.write("No resource locator");
        }
    }

    private void playStandaloneUsingServiceLocator()
    {
        System.out.println("remoteserviceselectionxlet play standalone using service locator: "
                + serviceLocator.toExternalForm());
        if (serviceLocator != null)
        {
            playStandalone(serviceLocator);
        }
        else
        {
            // vidTextBox.write("No service locator");
        }
    }

    private void playStandalone(Locator locator)
    {
        try
        {
            if (!initialized)
            {
                System.out.println("remoteserviceselectionxlet creating player with locator: "
                        + locator.toExternalForm());
                if ((player != null) && (cListener != null))
                {
                    player.removeControllerListener(cListener);
                }
                cListener = new ControllerListenerImpl();
                player = Manager.createPlayer(new MediaLocator(locator.toExternalForm().trim()));
                player.addControllerListener(cListener);
                initialized = true;
            }

            System.out.println("remoteserviceselectionxlet calling player.start");
            synchronized (this)
            {
                // stopped = false;
            }

            player.start();
            System.out.println("remoteserviceselectionxlet returned from player.start");
            synchronized (this)
            {
                while (playing)
                {
                    try
                    {
                        wait(1000);
                    }
                    catch (InterruptedException ie)
                    {
                        // ignore
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (NoPlayerException e)
        {
            e.printStackTrace();
        }
    }

    private void playViaSelectServiceLocator()
    {
        /*
         * System.out.println("remoteserviceselectionxlet selecting service via locator: "
         * + serviceLocator.toExternalForm()); try { //serviceContext.select(new
         * Locator[]{serviceLocator}); serviceContext.select(new
         * Locator[]{remoteServiceLocator}); } catch
         * (InvalidServiceComponentException e) { e.printStackTrace(); } catch
         * (SecurityException e) { e.printStackTrace(); } catch
         * (InvalidLocatorException e) { e.printStackTrace(); }
         */
    }

    private void playViaSelectResourceLocator()
    {
        System.out.println("remoteserviceselectionxlet selecting service via resource locator");
        try
        {
            serviceContext.select(new Locator[] { resourceLocator });
        }
        catch (InvalidServiceComponentException e)
        {
            e.printStackTrace();
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (InvalidLocatorException e)
        {
            e.printStackTrace();
        }
    }

    private void playViaSelectService()
    {
        System.out.println("remoteserviceselectionxlet selecting service via service: " + service);

        // Make sure status indicates initializing
        statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PENDING);
        playbackIndicator.setCompletionRatio(0);
        scene.repaint();
        
        serviceContext.select(service);
    }

    private void stopService()
    {
        System.out.println("remoteserviceselectionxlet stopping service context ");

        serviceContext.stop();
    }

    private void setRate(float rate)
    {
        requestedRate = rate;
        System.out.println("remoteserviceselectionxlet setting rate to " + requestedRate);
        ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();
        System.out.println("handler count: " + handlers.length);
        if (handlers.length > 0)
        {
            System.out.println("handler class " + handlers[0].getClass().getName());

            // The one & only handler is a RemoteServicePlayer
            player = (Player) handlers[0];

            // Call set rate method on player
            player.setRate(requestedRate);
        }
        else
        {
            System.out.println("No player available, unable to set rate");
        }
    }

    public void pauseXlet()
    {
        System.out.println("remoteserviceselection pausexlet");
        releaseResources();
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        System.out.println("remoteserviceselection destroyxlet");
        releaseResources();
    }

    private void releaseResources()
    {
        System.out.println("release resources");
        if (initialized)
        {
            if (player != null)
            {
                player.stop();
            }
            if (scene != null)
            {
                scene.removeAll();
                scene.setVisible(false);
            }
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - " + "received controller event: "
                    + event);
            if (event instanceof StartEvent)
            {
                if (player.getState() == Controller.Started)
                {
                    System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - started");

                    playing = true;
                    Component component = player.getVisualComponent();
                    if (component != null)
                    {
                        System.out.println("remoteserviceselectionxlet component available, adding to scene: "
                                + component);
                        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
                        if (scene != null)
                        {
                            scene.add(component);
                            scene.setVisible(true);
                        }
                        else
                        {
                            System.out.println("unable to get scene - unable to add component: " + component);
                        }
                    }
                    else
                    {
                        System.out.println("remoteserviceselectionxlet no component, not adding to scene");
                    }
                }
                playbackIndicator.setVisible(true);
            }
            else if (event instanceof org.ocap.shared.media.EndOfContentEvent)
            {
                synchronized (RemoteServiceSelectionXlet.this)
                {
                    System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - "
                            + " received end of content event");
                    statusBox.update(StatusBox.COLOR_PAUSED, StatusBox.MSG_EOS);
                    scene.repaint();
                    // stopped = true;
                    // playing = false;
                }
            }
            else if ((event instanceof StopEvent) ||
                     (event instanceof javax.media.ControllerClosedEvent))
            {
                synchronized (RemoteServiceSelectionXlet.this)
                {
                    System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - " + " received stop event");

                    // Remove listener since stopped
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    if (playing)
                    {
                        statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_STOP);
                        playbackIndicator.setCompletionRatio(0);
                        playing = false;
                    }
                    // playbackIndicator.repaint();
                    scene.repaint();
                    // stopped = true;
                }
            }
            else if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - " + " got begining of content");
                if (requestedRate < 0.0)
                {
                    statusBox.update(StatusBox.COLOR_PAUSED, StatusBox.MSG_BOS);
                    scene.repaint();
                }
            }
            else if (event instanceof javax.media.RateChangeEvent)
            {
                System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - " + " got rate change event");

                // Verify rate change event has requested rate
                if (player.getRate() != requestedRate)
                {
                    System.out.println("RemoteServiceSelectionXlet.controllerUpdate() - "
                            + " unable to change rate, requested rate " + requestedRate + ", stayed at rate "
                            + player.getRate());
                    statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_FAILED_RATE);
                }
                else if (player.getRate() == 0.0)
                {
                    statusBox.setColor(StatusBox.COLOR_PAUSED);
                }
                else
                {
                    statusBox.setColor(StatusBox.COLOR_PLAY);
                }
                scene.repaint();
            }
            else
            {
                System.out.println("remoteserviceselectionxlet unhandled event");
            }
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
    public void keyPressed(KeyEvent key)
    {
        // If a recording has not yet been selected, either default to 1 or use
        // vk 1
        if (service == null)
        {
            int recIdx = 0;
            switch (key.getKeyCode())
            {
                case HRcEvent.VK_2:
                    if (services.length > 1)
                    {
                        recIdx = 1;
                    }
                    break;
                case HRcEvent.VK_3:
                    if (services.length > 2)
                    {
                        recIdx = 2;
                    }
                    break;
                case HRcEvent.VK_4:
                    if (services.length > 3)
                    {
                        recIdx = 3;
                    }
                    break;
                case HRcEvent.VK_5:
                    if (services.length > 4)
                    {
                        recIdx = 4;
                    }
                    break;
                case HRcEvent.VK_6:
                    if (services.length > 5)
                    {
                        recIdx = 5;
                    }
                    break;
                case HRcEvent.VK_7:
                    if (services.length > 6)
                    {
                        recIdx = 6;
                    }
                    break;
                case HRcEvent.VK_8:
                    if (services.length > 7)
                    {
                        recIdx = 7;
                    }
                    break;
                case HRcEvent.VK_9:
                    if (services.length > 8)
                    {
                        recIdx = 8;
                    }
                    break;
                default:
                    // leave set at 0
            }

            // Set the remote service to selected recording
            service = services[recIdx];

            // Remove list of recordings box
            vidTextBox.setVisible(false);

            playbackIndicator.setCompletionRatio(0);
            playbackIndicator.setVisible(true);
            statusBox.setVisible(true);
            titleBox.update(StatusBox.COLOR_INIT, "    Remote Service Selection Xlet - Recording " + (recIdx + 1));
            statusBox.update(StatusBox.COLOR_INIT, StatusBox.MSG_INIT);
            // Automatically start playing selected recording
            if (!playing)
            {
                System.out.println("Simulator: VK_1/VK_PLAY player needs to be started");
                statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PENDING);
                playViaSelectService();
            }
        }
        else
        {
            switch (key.getKeyCode())
            {
                case HRcEvent.VK_6:
                case HRcEvent.VK_1:
                case OCRcEvent.VK_PLAY:
                    if (!playing)
                    {
                        System.out.println("Simulator: VK_1/VK_PLAY player needs to be started");
                        statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PENDING);
                        playViaSelectService();
                    }
                    else
                    {
                        System.out.println("Simulator: VK_1/VK_PLAY need to set rate to 1.0");
                        statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PLAY_1);
                        setRate(1.0f);
                    }
                    break;

                case OCRcEvent.VK_STOP:
                case HRcEvent.VK_2:
                    // statusBox.update(StatusBox.COLOR_PLAY,
                    // StatusBox.MSG_PLAY);
                    // playViaSelectServiceLocator();
                    statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_STOP);
                    stopService();
                    vidTextBox.setVisible(true);
                    playbackIndicator.setCompletionRatio(0);
                    playbackIndicator.setVisible(false);
                    //statusBox.update(StatusBox.COLOR_PENDING, StatusBox.MSG_PENDING);
                    //statusBox.setVisible(false);
                    titleBox.update(StatusBox.COLOR_INIT, "    Remote Service Selection Xlet");
                    service = null;
                    break;
                case OCRcEvent.VK_PAUSE:
                case HRcEvent.VK_3:
                    // statusBox.update(StatusBox.COLOR_PLAY,
                    // StatusBox.MSG_PLAY);
                    // playStandaloneUsingServiceLocator();
                    statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_0);
                    setRate(0.0f);
                    break;
                case OCRcEvent.VK_FAST_FWD:
                case HRcEvent.VK_4:
                    statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_2);
                    setRate(2.0f);
                    // playStandaloneUsingResourceLocator();
                    break;
                case OCRcEvent.VK_REWIND:
                case HRcEvent.VK_5:
                    // compareLocators();
                    statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_3);
                    setRate(-2.0f);
                    break;
                default:
                    // *TODO* - add text area for messages
                    // vidTextBox.write("*** Unsupported option ****");
                    break;
            }
        }

        scene.repaint();
    }

    private class ServiceContextListenerImpl implements ServiceContextListener
    {
        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            System.out.println("RemoteServiceSelectionXlet.receiveServiceContextEvent() - "
                    + "called with servicecontext event: " + e);
            if (e instanceof SelectionFailedEvent)
            {
                synchronized (RemoteServiceSelectionXlet.this)
                {
                    System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - "
                            + "got failure event");

                    // Remove any existing listeners
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_FAILED);
                    scene.repaint();
                    playing = false;
                }
            }
            else if (e instanceof PresentationTerminatedEvent)
            {
                synchronized (RemoteServiceSelectionXlet.this)
                {
                    System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - "
                            + "presentation terminated");
                    
                    // Remove any existing listeners
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    if (playing)
                    {
                        statusBox.update(StatusBox.COLOR_STOP, StatusBox.MSG_STOP);
                        playing = false;
                    }
                    scene.repaint();
                }
            }
            else
            {
                System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - "
                        + "Looking for the player returned via service context");
                ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();
                System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - " + "handler count: "
                        + handlers.length);
                if (handlers.length > 0)
                {
                    System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - "
                            + "Assigning player to handler class " + handlers[0].getClass().getName());

                    // The one & only handler is a RemoteServicePlayer
                    player = (Player) handlers[0];
                    
                    // Remove any existing listeners
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                    }
                    cListener = new ControllerListenerImpl();
                    player.addControllerListener(cListener);
                }

                if (e instanceof javax.tv.service.selection.NormalContentEvent)
                {
                    System.out.println("RemoteServiceSelectionXlet.receiveServerContextEvent() - "
                            + "got normal content event, now playing");
                    playing = true;
                    statusBox.update(StatusBox.COLOR_PLAY, StatusBox.MSG_PLAY_1);
                    scene.repaint();
                }
            }
        }
    }
    
    /**
     * Method required to implement Driveable interface.  Interface is used so that 
     * this Xlet can be run using with AutoXlet.
     */
    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        if (useMonitor)
        {
            eventMonitor.setTimeout(monitorTimeout);

            synchronized (eventMonitor)
            {
                keyPressed(event);
                eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(event);
        }
    }
}
