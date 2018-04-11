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
package org.cablelabs.xlet.DvrExerciser;

import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.tv.service.Service;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.ui.event.OCRcEvent;

public class RemoteServicePlayer
{
    // player and rendering
    private Player player;

    boolean initialized;

    // service
    private ServiceContext serviceContext;

    private Service service;

    private float requestedRate = 1.0f;

    // private boolean stopped;
    private boolean playing = false;

    private RemoteServiceStatusBox statusBox = null;

    private RemoteServiceStatusBox titleBox = null;
    
    private RemoteServiceStatusBox mediaTimeBox = null;

    private Bargraph playbackIndicator;

    private ServiceContextListenerImpl scListener = null;
    
    private ControllerListener cListener = null;
    
    // this is a list of permissible playback rates - pressing the '<<' or '>>'
    // keys on the remote control will
    private float m_playRates[] = new float[] { (float) -64.0, (float) -32.0, (float) -16.0, (float) -8.0, (float) -4.0,
            (float) -2.0, (float) -1.0, (float) 1.0, (float) 2.0, (float) 4.0, (float) 8.0,
            (float) 16.0, (float) 32.0, (float) 64.0, };

    public RemoteServicePlayer(String title)
    {
        System.out.println("******************* RemoteServicePlayer initializing");

        titleBox = new RemoteServiceStatusBox(0, 0, 640, 30);
        titleBox.update(RemoteServiceStatusBox.COLOR_INIT, "    " + title);

        statusBox = new RemoteServiceStatusBox(30, 40, 580, 30);
        statusBox.update(RemoteServiceStatusBox.COLOR_INIT, RemoteServiceStatusBox.MSG_INIT);
        statusBox.setVisible(true); // Show startup status

        mediaTimeBox = new RemoteServiceStatusBox(30, 80, 580, 20);
        mediaTimeBox.update(RemoteServiceStatusBox.COLOR_INIT, "Current Media Time Secs: 0");
        mediaTimeBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mediaTimeBox.setVisible(false);

        playbackIndicator = new Bargraph();
        playbackIndicator.setBounds(30, 70, 580, 10);

        DvrExerciser.getInstance().m_scene.add(titleBox);
        DvrExerciser.getInstance().m_scene.add(statusBox);
        DvrExerciser.getInstance().m_scene.add(mediaTimeBox);
        DvrExerciser.getInstance().m_scene.add(playbackIndicator);
 
        DvrExerciser.getInstance().m_scene.validate();

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
                        //System.out.println("Player get duration called");
                        Time duration = player.getDuration();
                        Time mediaTime = player.getMediaTime();
                        
                        // If the duration is greater than 300 minutes or 18000 secs
                        if (duration.getSeconds() >  18000)
                        {
                            //System.out.println("RemoteServicePlayer - Not updating playback indicator due to invalid duration");
                            mediaTimeBox.setVisible(true); // Show startup status
                            mediaTimeBox.update(RemoteServiceStatusBox.COLOR_INIT, "Current Media Time secs: " + 
                                    ((int)mediaTime.getSeconds()));
                            mediaTimeBox.repaint();
                        }
                        else if (duration.getNanoseconds() > 0) 
                        {
                            float completionRatio = ((float) (mediaTime.getSeconds())) / (float) duration.getSeconds();
                            playbackIndicator.setCompletionRatio(completionRatio);
                            //System.out.println("RemoteServicePlayer - Updating playback indicator with ratio: "
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
                        // System.out.println("RemoteServicePlayer - Not updating playback indicator due to not playing");
                    }
                    DvrExerciser.getInstance().m_scene.repaint();

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
        scListener = new ServiceContextListenerImpl();
        serviceContext.addListener(scListener);

        System.out.println("******************* RemoteServicePlayer initailized ");
    }

    protected void playViaSelectService(Service service)
    {
        System.out.println("RemoteServicePlayer selecting service via service: " + service);

        // Make sure status indicates initializing
        statusBox.update(RemoteServiceStatusBox.COLOR_PENDING, RemoteServiceStatusBox.MSG_PENDING);
        playbackIndicator.setCompletionRatio(0);
        DvrExerciser.getInstance().m_scene.repaint();
        
        serviceContext.select(service);
    }

    protected void stop()
    {
        System.out.println("RemoteServicePlayer stopping service context ");

        DvrExerciser.getInstance().stopServiceContextAndWaitForPTE(serviceContext);
        playbackIndicator.setCompletionRatio(0);
        playbackIndicator.setVisible(false);
        titleBox.update(RemoteServiceStatusBox.COLOR_INIT, "    Remote Service Selection Xlet");
        service = null;
        System.out.println("release resources");
        DvrExerciser.getInstance().m_scene.remove(statusBox);
        DvrExerciser.getInstance().m_scene.remove(titleBox);
        DvrExerciser.getInstance().m_scene.remove(playbackIndicator);
        DvrExerciser.getInstance().m_scene.remove(mediaTimeBox);
    }

    private void setRate(float rate)
    {
        requestedRate = rate;
        System.out.println("RemoteServicePlayer setting rate to " + requestedRate);
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
    
    private void skip(int seconds)
    {
        System.out.println("RemoteServicePlayer skipping " + seconds + " seconds.");
        ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();
        System.out.println("handler count: " + handlers.length);
        if (handlers.length > 0)
        {
            System.out.println("handler class " + handlers[0].getClass().getName());

            // The one & only handler is a RemoteServicePlayer
            player = (Player) handlers[0];

            // Increase the player's media time by 10 seconds
            player.setMediaTime(new Time(player.getMediaTime().getSeconds() + seconds));
        }
        else
        {
            System.out.println("No player available, unable to set rate");
        }
    }

    private class ControllerListenerImpl implements ControllerListener
    {
        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("RemoteServicePlayer.controllerUpdate() - " + "received controller event: "
                    + event);
            if (event instanceof StartEvent)
            {
                if (player.getState() == Controller.Started)
                {
                    System.out.println("RemoteServicePlayer.controllerUpdate() - started");

                    playing = true;
                 }
                playbackIndicator.setVisible(true);
            }
            else if (event instanceof org.ocap.shared.media.EndOfContentEvent)
            {
                synchronized (this)
                {
                    System.out.println("RemoteServicePlayer.controllerUpdate() - "
                            + " received end of content event");
                    statusBox.update(RemoteServiceStatusBox.COLOR_PAUSED, RemoteServiceStatusBox.MSG_EOS);
                    DvrExerciser.getInstance().m_scene.repaint();
                    // stopped = true;
                    // playing = false;
                }
            }
            else if ((event instanceof StopEvent) ||
                     (event instanceof javax.media.ControllerClosedEvent))
            {
                synchronized (this)
                {
                    System.out.println("RemoteServicePlayer.controllerUpdate() - " + " received stop event");

                    // Remove listener since stopped
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    if (playing)
                    {
                        statusBox.update(RemoteServiceStatusBox.COLOR_STOP, RemoteServiceStatusBox.MSG_STOP);
                        playbackIndicator.setCompletionRatio(0);
                        playing = false;
                    }
                    // playbackIndicator.repaint();
                    DvrExerciser.getInstance().m_scene.repaint();
                    // stopped = true;
                }
            }
            else if (event instanceof org.ocap.shared.media.BeginningOfContentEvent)
            {
                System.out.println("RemoteServicePlayer.controllerUpdate() - " + " got begining of content");
                if (requestedRate < 0.0)
                {
                    statusBox.update(RemoteServiceStatusBox.COLOR_PAUSED, RemoteServiceStatusBox.MSG_BOS);
                    DvrExerciser.getInstance().m_scene.repaint();
                }
            }
            else if (event instanceof javax.media.RateChangeEvent)
            {
                System.out.println("RemoteServicePlayer.controllerUpdate() - " + " got rate change event");

                // Verify rate change event has requested rate
                if (player.getRate() != requestedRate)
                {
                    System.out.println("RemoteServicePlayer.controllerUpdate() - "
                            + " unable to change rate, requested rate " + requestedRate + ", stayed at rate "
                            + player.getRate());
                    statusBox.update(RemoteServiceStatusBox.COLOR_STOP, RemoteServiceStatusBox.MSG_FAILED_RATE);
                }
                else if (player.getRate() == 0.0)
                {
                    statusBox.setColor(RemoteServiceStatusBox.COLOR_PAUSED);
                }
                else
                {
                    statusBox.setColor(RemoteServiceStatusBox.COLOR_PLAY);
                }
                DvrExerciser.getInstance().m_scene.repaint();
            }
            else
            {
                System.out.println("RemoteServicePlayer unhandled event");
            }
        }
    }

    /**
     * keyPressed implementation of the KeyListener interface this is where the
     * user interaction happens from remote key presses
     */
    public void keyPressed(KeyEvent key)
    {
        switch (key.getKeyCode())
        {
        case OCRcEvent.VK_PLAY:
            if (!playing)
            {
                System.out.println("Simulator: VK_1/VK_PLAY player needs to be started");
                statusBox.update(RemoteServiceStatusBox.COLOR_PENDING, RemoteServiceStatusBox.MSG_PENDING);
                playViaSelectService(service);
            }
            else
            {
                System.out.println("Simulator: VK_1/VK_PLAY need to set rate to 1.0");
                statusBox.update(RemoteServiceStatusBox.COLOR_PENDING, RemoteServiceStatusBox.MSG_PLAY_1);
                setRate(1.0f);
            }
            break;

        case OCRcEvent.VK_PAUSE:
            statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_0);
            setRate(0.0f);
            break;
        case OCRcEvent.VK_FAST_FWD:
            fastForward();
            statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_2_PREFIX + requestedRate);
            break;
        case OCRcEvent.VK_REWIND:
            rewind();
            statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_3_PREFIX + requestedRate);
            break;
        case OCRcEvent.VK_LEFT:
			statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_5);
            skip(-10);
            break;
        case OCRcEvent.VK_RIGHT:
			statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_4);
            skip(10);
            break;
        default:
            // *TODO* - add text area for messages
            // vidTextBox.write("*** Unsupported option ****");
            break;
        }

        DvrExerciser.getInstance().m_scene.repaint();
    }

    private class ServiceContextListenerImpl implements ServiceContextListener
    {
        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            System.out.println("RemoteServicePlayer.receiveServiceContextEvent() - "
                    + "called with servicecontext event: " + e);
            if ((e instanceof SelectionFailedEvent) ||
                (e instanceof AlternativeContentErrorEvent))
            {
                synchronized (this)
                {
                    System.out.println("RemoteServicePlayer.receiveServerContextEvent() - "
                            + "got failure event");

                    // Remove any existing listeners
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    statusBox.update(RemoteServiceStatusBox.COLOR_STOP, RemoteServiceStatusBox.MSG_FAILED);
                    DvrExerciser.getInstance().m_scene.repaint();
                    playing = false;
                }
            }
            else if (e instanceof PresentationTerminatedEvent)
            {
                synchronized (this)
                {
                    System.out.println("RemoteServicePlayer.receiveServerContextEvent() - "
                            + "presentation terminated");
                    
                    // Remove any existing listeners
                    if ((player != null) && (cListener != null))
                    {
                        player.removeControllerListener(cListener);
                        cListener = null;
                    }
                    if (playing)
                    {
                        statusBox.update(RemoteServiceStatusBox.COLOR_STOP, RemoteServiceStatusBox.MSG_STOP);
                        playing = false;
                    }
                    DvrExerciser.getInstance().m_scene.repaint();
                }
            }
            else
            {
                System.out.println("RemoteServicePlayer.receiveServerContextEvent() - "
                        + "Looking for the player returned via service context");
                ServiceContentHandler[] handlers = serviceContext.getServiceContentHandlers();
                System.out.println("RemoteServicePlayer.receiveServerContextEvent() - " + "handler count: "
                        + handlers.length);
                if (handlers.length > 0)
                {
                    System.out.println("RemoteServicePlayer.receiveServerContextEvent() - "
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
                    System.out.println("RemoteServicePlayer.receiveServerContextEvent() - "
                            + "got normal content event, now playing");
                    playing = true;
                    statusBox.update(RemoteServiceStatusBox.COLOR_PLAY, RemoteServiceStatusBox.MSG_PLAY_1);
                    DvrExerciser.getInstance().m_scene.repaint();
                }
            }
        }
    }

    public int getPlayerState()
    {
        return player.getState();
    }
    
    /**
     * Increases the play rate by 1 step.
     * 
     */
    private void fastForward()
    {
        float rate = 2.0f;

        // now get the next fastest playback rate
        if (requestedRate > 1.0f)
        {
            rate = getNextPlayRate(requestedRate);
        }
        // set the faster playback rate
        setRate(rate);
    }

    /**
     * Decreases the play rate by 1 step.
     * 
     */
    private void rewind()
    {
        float rate = -1.0f;
        if (requestedRate < 0.0f)
        {
            rate = getPreviousPlayRate(requestedRate); 
        }
        
        // set the faster rewind rate
        setRate(rate);
    }
    
    /**
     * Gets the next lowest valid play rate. TODO: document
     * 
     * @param currentPlayRate
     * @return
     */
    public float getPreviousPlayRate(float currentPlayRate)
    {
        int index = -1;
        float fRetVal = 1.0f; // default is 1x

        // find the index of the current play rate in the table of
        // valid play rates
        if (currentPlayRate != 0.0f)
        {
            index = getPlayRateIndex(currentPlayRate);
        }
        if (-1 != index)
        {
            index--;

            // make sure the index doesn't run past the beginning of the table
            if (0 > index)
            {
                index = 0;
            }
        }
        else
        {
            // Get index of 1.0 and decrement
            index = getPlayRateIndex(1.0f);
            index--;
        }

        fRetVal = m_playRates[index];
        return fRetVal;
    }
    
    /**
     * Gets the next highest valid play rate.
     * 
     * @param currentPlayRate
     * @return
     */
    public float getNextPlayRate(float currentPlayRate)
    {
        int index = -1;
        float fRetVal = 1.0f; // default is 1x

        // find the index of the current play rate in the table of
        // valid play rates
        if (currentPlayRate != 0.0f)
        {
            index = getPlayRateIndex(currentPlayRate);
        }
        if (-1 != index)
        {
            index++;

            // make sure the index doesn't run past the end of the table
            if (m_playRates.length <= index)
            {
                index = m_playRates.length - 1;
            }
        }
        else
        {
            // Get index of 1.0 and increment
            index = getPlayRateIndex(1.0f);
            index++;
         }

        fRetVal = m_playRates[index];
        return fRetVal;
    }

    /**
     * Finds the index of the current play rate in the table of valid play rates
     * 
     * If the current play rate is not in the table of valid play rates, the
     * index of the entry that represents 1.0 is returned.
     * 
     * @param currentPlayRate
     * @return
     */
    private int getPlayRateIndex(float currentPlayRate)
    {
        int i;
        int retVal = -1;

        // find current play rate in the table of valid play rates
        for (i = 0; i < m_playRates.length; i++)
        {
            if (m_playRates[i] == currentPlayRate)
            {
                retVal = i;
                break;
            }
        }

        System.out.println("getPlayRateIndex() - returning for rate = " +
                currentPlayRate + ", index = " + retVal);
        return retVal;
    }
}
