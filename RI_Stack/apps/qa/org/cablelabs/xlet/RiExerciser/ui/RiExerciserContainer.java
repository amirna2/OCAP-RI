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

package org.cablelabs.xlet.RiExerciser.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * 
 * @author Nicolas Metts
 * This class handles the UI throughout the various Pages that are made active
 * by the RiExerciserController. In particular,  this class adds and removes
 * components from the HScene, displays log and status messages received from
 * RiExerciserController and Pages, writes status messages to the video screen
 * indicating TSB and tuning status and also media server selected. Basically,
 * this class is the primary HContainer for the RiExerciser Xlet. The purpose 
 * of this class is to separate UI functionality from the RiExerciserController, 
 * in order to maintain the MVC design for the RiExerciser Xlet. Since this
 * class may need to make calls to OcapAppDriverDVR, OcapAppDriverHN, or
 * OcapAppDriverHNDVR, it should always be verified that the respective extensions
 * are enabled before making calls to these classes.
 *
 */
public class RiExerciserContainer extends HContainer
{
    private static Logger log = Logger.getLogger(RiExerciserContainer.class);
    
    /**
     * The Singleton instance of this RiExerciserContainer
     */
    private static RiExerciserContainer s_container = new RiExerciserContainer();
    
    /**
     * A reference to the RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDriverCore from RiExerciserController for core OCAP functionality
     */
    private OcapAppDriverInterfaceCore m_oadCore;
    
    /**
     * OcapAppDriverDVR from RiExerciserController for DVR functionality
     */
    private OcapAppDriverInterfaceDVR m_oadDVR;
    
    /**
     * The main HScene for the UI
     */
    private HScene m_scene;
    
    /**
     * The playback rate of the Player
     */
    private float m_playRate;
    
    /**
     * A Bargraph to display playback progress
     */
    private Bargraph m_playbackIndicator;
    
    private long m_playbackDuration;
    
    private InteractiveResourceUsageList m_ruList;
    
    /** 
     * A Messenger to display log messages and status messages
     */
    private Messenger m_messenger;
    
    /**
     * A boolean indicating whether a recording has a zero length or not
     */
    private boolean m_zeroLengthRecording;
    
    /**
     * Indicates whether the most recent publish attempt was successful or not
     */
    private boolean m_publishCompleted;
    
    private RiExerciserContainer()
    {
        // Initialize the member Objects
        m_controller = RiExerciserController.getInstance();
        m_publishCompleted = false;
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_messenger = new Messenger();
        m_messenger.setBounds(0, // x
                (2 * RiExerciserConstants.QUARTER_PAGE_HEIGHT) / 4, // y
                RiExerciserConstants.QUARTER_PAGE_WIDTH, // width
                (2 * RiExerciserConstants.QUARTER_PAGE_HEIGHT) / 4); // height
        m_messenger.setFont(new Font("SansSerif", Font.BOLD,
                RiExerciserConstants.FONT_SIZE));
        setFont((new Font("SansSerif", Font.BOLD, 
                RiExerciserConstants.FONT_SIZE)));
        m_scene.setVisible(true);
        m_scene.add(m_messenger);
        m_playbackIndicator = new Bargraph();
        m_scene.add(m_playbackIndicator);
        m_playbackIndicator.setVisible(false);
    }
    
    public void initGUI()
    {
        // Initialize the layout and size of this Container, and add it to the
        // HScene
        s_container.setLayout(null);
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        if (m_controller.isDvrEnabled())
        {
            m_oadDVR = OcapAppDriverDVR.getOADDVRInterface();
        }
        m_playRate = m_oadCore.getPlaybackRate();
        s_container.setSize(RiExerciserConstants.QUARTER_PAGE_WIDTH,
                RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        m_scene.add(s_container);
        m_ruList = new InteractiveResourceUsageList();
    }
    
    /**
     * Accessor method for the Singleton instance of RiExerciserContainer
     * @return
     */
    public static RiExerciserContainer getInstance()
    {
        return s_container;
    }
    
    /**
     * A method to draw a bargraph that indicates the progress of the current
     * playback
     * @param playerIndex the index of the player playing
     * @param duration the duration of the content item
     */
    public void indicatePlayback(int playerType, long duration)
    {   
        m_playbackIndicator.setVisible(true);
        m_playbackDuration = duration;
        m_zeroLengthRecording = (m_playbackDuration == 0);
        
        // The following statement is commented out, but can be uncommented to 
        // debug issues with indicating playback progress for JMF playback
        //m_controller.displayMessage("Playback duration is: " + duration + " nanoseconds");

        m_playbackIndicator.setBounds(0, RiExerciserConstants.QUARTER_PAGE_HEIGHT/2 - 10, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH/2, 10);
    }
    
    public void stopIndicatePlayback()
    {
        m_playbackIndicator.setVisible(false);
        s_container.repaint();
    }
    
    
    /**
     * A convenience method for setting the size of the HScene
     * @param x the width of the HScene
     * @param y the height of the HScene
     */
    public void setSceneSize(int x, int y)
    {
        m_scene.setSize(x, y);
    }
    
    /**
     * Convenience method for adding Components to the HScene
     * @param c the Component to be added to the HScene
     */
    public void addToScene(Component c)
    {
        m_scene.add(c);
    }
    
    /**
     * Convenience method for removing Components from the HScene
     * @param c the Component to remove from the HScene
     */
    public void removeFromScene(Component c)
    {
        m_scene.remove(c);
    }
    
    /**
     * Convenience method for repainting the HScene
     */
    public void repaintScene()
    {
        m_scene.repaint();
    }
    
    /**
     * Convenience method for adding a KeyListener to the HScene
     * @param k the KetListener to be added to the HScene
     */
    public void addSceneKeyListener(KeyListener k)
    {
        m_scene.addKeyListener(k);
    }

    public void addSceneComponentListener(ComponentListener listener)
    {
        m_scene.addComponentListener(listener);
    }
    
    /**
     * Convenience method for requesting the HScene focus
     */
    public void requestSceneFocus()
    {
        m_scene.setFocusable(true);
        m_scene.requestFocus();
    }
    
    /**
     * Convenience method for accessing the Graphics for the HScene
     * @return the Graphics for the HScene
     */
    public Graphics getSceneGraphics()
    {
        return m_scene.getGraphics();
    }
    
    /**
     * Convenience method for adding a message to Messenger
     * @param m the message to be added to Messenger
     */
    public void addMessage(String m)
    {
        m_messenger.addMessage(m);
    }
    
    /**
     * A method to hide the Messenger
     */
    public void hideMessenger()
    {
        m_messenger.setVisible(false);
    }
    
    /**
     * A method to show the Messenger
     */
    public void showMessenger()
    {
        m_messenger.setVisible(true);
    }
    
    /**
     * Shows the list of ResourceUsage requests
     */
    public void showResourceUsageList()
    {
        m_ruList.show();
    }
    
    /**
     * Make all the Components and the HScene invisible
     */
    public void hide()
    {
        m_messenger.setVisible(false);
        s_container.repaint();
        m_scene.repaint();
    }
    
    /**
     * Show the HScene and Messenger
     */
    public void show()
    {
        s_container.setVisible(true);
        m_messenger.setVisible(true);
        m_scene.setVisible(true);
        m_scene.show();
        m_scene.requestFocus();
    }
    
    /**
     * Convenience method for popping a HScene Component to front
     * @param c the Component to be popped to the front of the HScene
     */
    public void scenePopToFront(Component c)
    {
        m_scene.popToFront(c);
    }
    
    /**
     * Override of paint from HContainer. This method displays messages that
     * relate the status of various OcapAppDriver states, such as whether TSB
     * is enabled or if the viewing mode is currently Live, in addition to
     * playback rate. When viewing one of the HN Pages, this method displays
     * the current media server selected, and also indicates whether the last
     * ContentItem that was attempted to be published was successfully published.
     * @param g the Graphics for adding String messages
     */
    public void paint(Graphics g)
    {
        
        DVBGraphics dvbG = (DVBGraphics)g;
        dvbG.setColor(Color.WHITE);
        
        try
        {
            // enable overlay compositing control
            dvbG.setDVBComposite(DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC));
        }
        catch (UnsupportedDrawingOperationException e)
        {
            //not logging error in paint
        }
        if (m_controller.getVideoMode() == RiExerciserController.LIVE_TUNING_MODE)
        {
            String currentPageName = m_controller.getCurrentPageName();
            // If one of the HN pages is active, display the media server selected
            // and also the status of the most recent publish
            if ((currentPageName.equals(
                    RiExerciserConstants.HN_SERVER_OPTIONS_PAGE) || 
                    currentPageName.equals(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE) ||
                    currentPageName.equals(RiExerciserConstants.HN_DIAGNOSTICS_PAGE) ||
                    currentPageName.equals(RiExerciserConstants.HN_GENERAL_MENU_PAGE) ||
                    currentPageName.equals(RiExerciserConstants.VPOP_CLIENT_MENU_PAGE) ||
                    currentPageName.equals(RiExerciserConstants.HN_PUBLISH_CHANNEL_MENU_PAGE) ||
                    currentPageName.equals(RiExerciserConstants.HN_PUBLISH_RECORDING_MENU_PAGE))) 
            {
                HNPlayerMenuPage playerPage = (HNPlayerMenuPage)m_controller.getPage
                (RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
                playerPage.init();
                String mediaServerSelected = playerPage.getSelectedMediaServer();
                StringBuffer sbTsb = new StringBuffer("Server: "+ mediaServerSelected);
                g.drawString(sbTsb.toString(), 10, 
                        (RiExerciserConstants.QUARTER_PAGE_HEIGHT / 2) - 5);
                
                if (currentPageName.equals(RiExerciserConstants.HN_PUBLISH_RECORDING_MENU_PAGE))
                {
                    HNPublishRecordingMenuPage publishRecordingPage = HNPublishRecordingMenuPage.getInstance();
                    m_publishCompleted = publishRecordingPage.publishSuccessful();
                }
                
                else if (currentPageName.equals(RiExerciserConstants.HN_PUBLISH_CHANNEL_MENU_PAGE))
                {
                    HNPublishChannelMenuPage publishChannelPage = HNPublishChannelMenuPage.getInstance();
                    m_publishCompleted = publishChannelPage.publishSuccessful();
                }
                
                String publishStatus = "Published: " + (m_publishCompleted ? "Yes" : "No");
                g.drawString(publishStatus, 10, (RiExerciserConstants.QUARTER_PAGE_HEIGHT / 2) - 20);
            }
            
           
         
            
            // Otherwise, display the TSB status, the playback rate, and the viewing mode
            else
            {
                if (m_controller.isPlayerStopped())
                {
                    m_playRate = Float.NaN;
                }
                else if (m_controller.isPlayerPaused())
                {
                    m_playRate = 0.0f;
                }
                else
                {
                    m_playRate = m_oadCore.getPlaybackRate();
                }
                
                if (m_controller.isDvrEnabled())
                {
                    String tsbEnabled = m_oadDVR.isTsbEnabled() ? "enabled" : "disabled";
                    g.drawString("TSB State: " + tsbEnabled, 10, 
                            (RiExerciserConstants.QUARTER_PAGE_HEIGHT/2) -5);
                }
                String mode = m_controller.getVideoModeString();
                String paused = (m_playRate == 0.0 ? " (paused)" : "");
                g.drawString("Mode: " + mode + ", Rate: " + m_playRate + paused, 10,
                    (RiExerciserConstants.QUARTER_PAGE_HEIGHT/2) - 20);
            }
        }
        // If in playback mode, draw the playback indicator. Check for a zero
        // length recording to avoid DivideByZero error
        else if (! m_zeroLengthRecording  && 
                m_controller.getVideoMode() == RiExerciserController.DVR_PLAYBACK_MODE)
        {
            if (m_oadCore.playbackGetState() != OcapAppDriverInterfaceCore.PLAYBACK_STATE_UNKNOWN)
            {
                float completionRatio = 1.0f;
                if (m_playbackDuration != 0)
                {
                    completionRatio = (float)m_oadCore.getMediaTime()/(float)m_playbackDuration;

                    m_playbackIndicator.setCompletionRatio(completionRatio);
                }
            }
            m_scene.repaint();
        }
        else if (m_controller.getVideoMode() == RiExerciserController.REMOTE_PLAYBACK_MODE)
        {
            HNPlaybackPage playerPage = (HNPlaybackPage)m_controller.getPage
                                            (RiExerciserConstants.HN_PLAYBACK_PAGE);
            playerPage.paint(g);

            m_scene.repaint();
            
        }
        super.paint(g);
    }
}
