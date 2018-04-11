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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.xlet.RiExerciser.RiExerciserConstants;
import org.cablelabs.xlet.RiExerciser.RiExerciserController;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.ocap.ui.event.OCRcEvent;

/**
 * This page displays HN Playback and handles trick modes
 * keys.  It also displays current status via status banner
 * and playback indicator bar graph.
 */
public class HNPlaybackPage extends FullPage
{
    private static final Logger log = Logger.getLogger(HNPlaybackPage.class);

    private static HNPlaybackPage m_page = new HNPlaybackPage();
    
    // Colors for playback status
    public static final Color COLOR_INIT = Color.WHITE;
    
    public static final Color COLOR_PENDING = Color.BLUE;
    
    public static final Color COLOR_PLAY = Color.GREEN;
    
    public static final Color COLOR_PAUSED = Color.YELLOW;
    
    public static final Color COLOR_FAILED = Color.RED;
    
    // Messages for playback status
    protected static final String MSG_INIT = "Initializing";

    protected static final String MSG_PENDING = "Play starting";

    protected static final String MSG_FAILED = "Failed - Press STOP to return to main menu";

    protected static final String MSG_PAUSED = "Paused: 0.0";

    protected static final String MSG_PLAY = "Playing: 1.0";

    protected static final String MSG_FAST_FWD = "Fast Fwd: ";

    protected static final String MSG_SLOW_FWD = "Slow Fwd: ";

    protected static final String MSG_REWIND = "Rewind: ";

    protected static final String MSG_SLOW_REWIND = "Slow Rewind: ";

    protected static final String MSG_EOS = "EOS - Press STOP or REWIND";

    protected static final String MSG_BOS = "BOS - Press PLAY, FAST FWD or STOP";
    
    private static final int PLAY_RATE_INDEX_0 = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_PAUSED;
    private static final int PLAY_RATE_INDEX_1 = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_PLAY;
    private static final int PLAY_RATE_INDEX_REWIND_1 = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_RWD_HALF;
    private static final int PLAY_RATE_INDEX_REWIND_MAX = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_RWD_64;
    private static final int PLAY_RATE_INDEX_FFWD_1 = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_FWD_HALF;
    private static final int PLAY_RATE_INDEX_FFWD_MAX = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_FWD_64;
        
    
    /**
     * The instance of the RiExerciserController
     */
    private RiExerciserController m_controller;
    
    /**
     * OcapAppDrivers from RiExerciserController
     */
    private OcapAppDriverInterfaceCore m_oadCore;
    private OcapAppDriverInterfaceHN m_oadHN;
        
    /**
     * An instance of RiExerciserContainer
     */
    private RiExerciserContainer m_container;
    
    /**
     * Indices relevant to playback
     */
    private int m_serverIndex;
    private int m_contentIndex;
    private int m_playerType;
    private int m_playRateIndex;
    
    /**
     * Flag which indicates if duration is available for this playback
     */
    private boolean m_isDurationAvailable;
    
    /**
     * Status boxes and bar graph indicator used on this page
     */
    private RemoteServiceStatusBox m_statusBox;
    private RemoteServiceStatusBox m_titleBox;
    private Bargraph m_playbackIndicator;
    
    // If initial playback fails on a remote service playback, by spec
    // the stack will re-select broadcast service.  We want consistent
    // behavior between JMF vs Remote Service Playback and for all failures.
    // Ensure video is not displayed once hn playback has failed
    private Rectangle m_hideVideoCover;
    
    // Flag indicating HN playback has failed to prevent confusion with re-selection
    // of broadcast service for remote service playback
    private boolean m_hasFailed = false;
    
    // Flag indicating if HN playback has started
    private boolean m_hasStarted = false;
    
    /**
     * Constructs an instance of HN playback page which is displayed
     * during playback of remote content.
     */
    private HNPlaybackPage()
    {
        // Initialize components
        m_controller = RiExerciserController.getInstance();
        m_oadCore = OcapAppDriverCore.getOADCoreInterface();
        m_oadHN = OcapAppDriverHN.getOADHNInterface();
        m_container = RiExerciserContainer.getInstance();

        m_titleBox = new RemoteServiceStatusBox(0, 0, 
                        RiExerciserConstants.QUARTER_PAGE_WIDTH, 30);
        m_titleBox.setFont(new Font("SansSerif", Font.PLAIN, RiExerciserConstants.FONT_SIZE));
        m_titleBox.setVisible(false);
        m_container.add(m_titleBox);

        m_statusBox = new RemoteServiceStatusBox(30, 40, 
                            RiExerciserConstants.QUARTER_PAGE_WIDTH - 60, 30);
        m_statusBox.setFont(new Font("SansSerif", Font.PLAIN, RiExerciserConstants.FONT_SIZE_LARGE));
        m_statusBox.setVisible(false);
        m_container.add(m_statusBox);

        m_playbackIndicator = new Bargraph();
        m_playbackIndicator.setBounds(0, RiExerciserConstants.QUARTER_PAGE_HEIGHT - 10, 
                RiExerciserConstants.QUARTER_PAGE_WIDTH, 10);
        m_playbackIndicator.setCompletionRatio(0.0f);
        m_playbackIndicator.setVisible(false);
        m_container.add(m_playbackIndicator);

        // Dimensions for failure cover
        m_hideVideoCover = new Rectangle(0, 0,
                                RiExerciserConstants.FULL_PAGE_WIDTH, RiExerciserConstants.FULL_PAGE_HEIGHT);
        
        setFont(new Font("SansSerif", Font.BOLD, RiExerciserConstants.FONT_SIZE));
        
        setLayout(null);
        setSize(RiExerciserConstants.FULL_PAGE_WIDTH, RiExerciserConstants.FULL_PAGE_HEIGHT);
        this.repaint();
    }
    
    public static HNPlaybackPage getInstance()
    {
        return m_page;
    }
    
    public void processUserEvent(KeyEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processUserEvent() - called");
        }
        switch(event.getKeyCode())
        {
            case OCRcEvent.VK_INFO:
            {
                // Specific to ServiceResolutionHandler
                m_controller.displayMessage("Calling updateTuningLocatorWithSRH");
                m_oadHN.updateTuningLocatorWithSRH();
                break;
            }

            case OCRcEvent.VK_PLAY:
            {
                m_playRateIndex = PLAY_RATE_INDEX_1;
                boolean playRateChanged = m_oadCore.playbackChangeRate(m_playRateIndex);
                if (!playRateChanged)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("processUserEvent() - unable to change rate to index: " +
                                m_playRateIndex);
                    }                    
                }
                break;
            }
            case OCRcEvent.VK_STOP:
            {
                if (log.isDebugEnabled())
                {
                    log.debug("processUserEvent() - stop key received");
                }
                destroy();
                break;
            }
            case OCRcEvent.VK_PAUSE:
            {
                if (m_controller.isDvrEnabled())
                {
                    if (m_playRateIndex != PLAY_RATE_INDEX_0)
                    {
                        m_playRateIndex = PLAY_RATE_INDEX_0;                    
                        boolean paused = m_oadCore.playbackChangeRate(m_playRateIndex);
                        if (!paused)
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("processUserEvent() - unable to change rate to index: " +
                                        m_playRateIndex);
                            }
                        }
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Trick mode not supported");
                    }
                }
                break;
            }
            case OCRcEvent.VK_FAST_FWD:
            {
                // Only attempt rate changes if DVR is enabled
                if (m_controller.isDvrEnabled())
                {
                    int nextIndex = getNextRateIndex(false);
                    if (nextIndex != m_playRateIndex)
                    {
                        m_playRateIndex = nextIndex;
                        boolean playRateChanged = m_oadCore.playbackChangeRate(m_playRateIndex);
                        if (!playRateChanged)
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("processUserEvent() - unable to change rate to index: " +
                                        m_playRateIndex);
                            }
                        }
                     }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Trick mode not supported");
                    }
                }
                break;
            }
            case OCRcEvent.VK_REWIND:
            {
                if (m_controller.isDvrEnabled())
                {
                    int nextIndex = getNextRateIndex(true);
                    if (nextIndex != m_playRateIndex)
                    {
                        m_playRateIndex = nextIndex;
    
                        boolean playRateChanged = m_oadCore.playbackChangeRate(m_playRateIndex);
                        if (!playRateChanged)
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("processUserEvent() - unable to change rate to index: " +
                                        m_playRateIndex);
                            }
                        }
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Trick mode not supported");
                    }
                }
                break;
            }

            case OCRcEvent.VK_LIVE:
            {
                // Set playspeed at +16x 
                m_playRateIndex = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_FWD_16;

                boolean playRateChanged = m_oadCore.playbackChangeRate(m_playRateIndex);
                if (!playRateChanged)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("processUserEvent() - unable to change rate to index: " +
                                m_playRateIndex);
                    }
                }
                
                break;
            }

            case OCRcEvent.VK_LIST:
            {
                // Set playspeed at -16x
                m_playRateIndex = OcapAppDriverInterfaceCore.PLAY_RATE_INDEX_RWD_16;

                boolean playRateChanged = m_oadCore.playbackChangeRate(m_playRateIndex);
                if (!playRateChanged)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("processUserEvent() - unable to change rate to index: " +
                                m_playRateIndex);
                    }
                }
                
                break;
            }
        }
        updatePlaybackStatus(true);
    }

    /**
     * Determines the index of the new rate for rewind or fast
     * forward key press based on current rate.
     * 
     * @param isRewindKey   true if rewind key initiated rate change, 
     *                      false if fast fwd key initiated rate change
     * 
     * @return  new rate index to use, maybe the same as current rate index
     *          if no rate change is available
     */
    private int getNextRateIndex(boolean isRewindKey)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNextRateIndex() - called with rate index: " + m_playRateIndex + 
                    ", rewind key: " + isRewindKey);
        }
        
        // Set return value to current value
        int newIndex = m_playRateIndex;
        
        if (isRewindKey)
        {
            // Requested to rewind, determine if currently rewinding
            if (m_playRateIndex <= PLAY_RATE_INDEX_REWIND_1)
            {
                // Currently rewinding, see if can decrement index
                if ((m_playRateIndex) - 1 >= PLAY_RATE_INDEX_REWIND_MAX)
                {
                    newIndex = m_playRateIndex - 1;
                }
            }
            else // Not currently rewinding, set rate index to lowest rewind rate 
            {
                newIndex = PLAY_RATE_INDEX_REWIND_1;
            }
        }
        else // is fast fwd key
        {
            // Requested to fast forward, determine if currently fast forward
            if (m_playRateIndex >= PLAY_RATE_INDEX_FFWD_1)
            {
                // Currently fast fwd, see if can increment index
                if ((m_playRateIndex) + 1 <= PLAY_RATE_INDEX_FFWD_MAX)
                {
                    newIndex = m_playRateIndex + 1;
                }
            }
            else // Not currently fast forward, set rate index to lowest fast fwd rate
            {
                newIndex = PLAY_RATE_INDEX_FFWD_1;
            }
            
        }
        if (log.isDebugEnabled())
        {
            log.debug("getNextRateIndex() - returning new index: " + newIndex);
        }
        return newIndex;
    }
    
    public void destroy()
    {
        m_oadCore.playbackChangeRate(PLAY_RATE_INDEX_1);
        m_oadCore.playbackStop(15);
        
        m_statusBox.setVisible(false);
        m_titleBox.setVisible(false);
        m_playbackIndicator.setVisible(false);

        m_controller.displayNewPage(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
        HNPlayerMenuPage playerPage = (HNPlayerMenuPage)m_controller.getPage
                                        (RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE);
        playerPage.refreshAfterPlayback();
    }
    
    public void init()
    {
        m_hasFailed = false;
        
        // Update the screen
        m_titleBox.setVisible(true);
        m_titleBox.update(COLOR_INIT, MSG_INIT);
        
        m_statusBox.setVisible(true);
        m_statusBox.update(COLOR_PENDING, MSG_PENDING);

        m_playbackIndicator.setVisible(true);
        m_playbackIndicator.setCompletionRatio(0.0f);

        m_page.repaint();
    }
    
    /**
     * A method to play video either from a remote player or a JMF player. This
     * method attempts to create the player and logs messages indicating whether
     * playback was successful
     * 
     * @param playerType    type of player to be created, either Service or JMF
     * @param serverIndex   index of media server where content is located
     * @param contentIndex  index of content item to playback
     * 
     * @return  true if playback has started, false otherwise
     */
    public boolean playVideo(int playerType, int serverIndex, int contentIndex)
    {
        if (log.isInfoEnabled())
        {
            log.info("playVideo() - called with player type: " + HNPlayerMenuPage.getPlayerTypeStr(playerType));
        }
        
        // Initialize display of page
        init();
        
        m_serverIndex = serverIndex;
        m_contentIndex = contentIndex;
        m_playerType = playerType;
        m_playRateIndex = PLAY_RATE_INDEX_1;

        // Get media server friendly name for display
        String serverName = m_oadHN.getMediaServerFriendlyName(m_serverIndex);

        // Verify player type is supported and get info to display
        String title = null;
        String contentTypeStr = null;
        if (m_playerType == OcapAppDriverInterfaceCore.PLAYBACK_TYPE_JMF)
        {
            title = RiExerciserConstants.PLAYBACK_REMOTE_JMF;
            contentTypeStr = OcapAppDriverInterfaceHN.HN_CONTENT_TYPE_STR_URL;
        }
        else if (m_playerType == OcapAppDriverInterfaceCore.PLAYBACK_TYPE_SERVICE)
        {
            title = RiExerciserConstants.PLAYBACK_REMOTE_SERVICE;
            contentTypeStr = m_oadHN.getContentItemTypeStr(m_serverIndex, m_contentIndex);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playVideo() - unsupported player type: " + m_playerType);
            }            
            return false;            
        }
        if (log.isInfoEnabled())
        {
            log.info("playVideo() - server name: " + serverName + ", title: " + title + ", content type:" + contentTypeStr + ", content index: " + m_contentIndex);
        }
        
        // Update the screen
        m_titleBox.update(COLOR_INIT, title + " from " + serverName + " - Content Type: " +
                            contentTypeStr);
        m_statusBox.update(COLOR_PENDING, MSG_PENDING);
        m_page.repaint();

        // Start a thread to do this so display is refreshed because 
        // there are actions which maybe delayed such as issuing HTTP
        // HEAD & GET requests, socket reads, etc.
        Runnable run = new Runnable()
        {
            public void run()
            {
                if (log.isInfoEnabled())
                {
                    log.info("playVideo() - creating player");
                }
                // Create the playback using temp index in order to ensure playback is valid prior to paint()
                m_hasFailed = false;
                if (!m_oadHN.playbackStart(m_playerType, m_serverIndex, m_contentIndex, 30))
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("playVideo() - playback start returned false - error creating player");
                    }            
                    m_controller.displayMessage("Error creating player.");
                    m_hasStarted = true;
                    m_hasFailed = true;
                    return;
                }
                boolean fullScreen = m_oadCore.setPlaybackFullscreen();
                if (log.isInfoEnabled())
                {
                    if (fullScreen)
                    {
                        log.info("playVideo() - video set to fullscreen");
                    }
                    else
                    {
                        log.info("playVideo() - failed to set video to fullscreen");
                    }
                }
                if (log.isInfoEnabled())
                {
                    log.info("playVideo() - successfully started player");
                }
                m_hasStarted = true;
                updatePlaybackStatus(true);
            }
        };
        new Thread(run, "RxHN-Playback").start();

        // Determine if duration is available
        m_isDurationAvailable = true;
        // Removed call to OcapAppDriverHN.isChannelContentItem(), since this
        // method assumes all Content Items are published to the root container
        if ((m_playerType == OcapAppDriverInterfaceCore.PLAYBACK_TYPE_JMF) ||
            (m_oadHN.isVPOPContentItem(m_serverIndex, m_contentIndex)))
        {
            // *TODO* - unavailable for JMF playbck until ability to get duration from HTTP response is added
            // to LocatorDataSource - OCORI-4281
            m_isDurationAvailable = false;
        }            

        if (log.isInfoEnabled())
        {
            log.info("playVideo() - returning: " + true);
        }
        return true;
    }    
    
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
        
        // If playback has failed, make sure no video is showing
        if ((m_hasFailed) || (m_oadCore.playbackGetState() == OcapAppDriverInterfaceCore.PLAYBACK_STATE_FAILED))
        {
            dvbG.setColor(Color.BLACK);
            dvbG.fillRect(m_hideVideoCover.x, m_hideVideoCover.y, 
                    m_hideVideoCover.width, m_hideVideoCover.height);
        }

        updatePlaybackStatus(false);
        
        if ((!m_hasFailed) && (m_hasStarted) && 
            (m_oadCore.playbackGetState()!= OcapAppDriverInterfaceCore.PLAYBACK_STATE_UNKNOWN) &&
            (m_oadCore.playbackGetState()!= OcapAppDriverInterfaceCore.PLAYBACK_STATE_FAILED))
        {
            if (m_isDurationAvailable)
            {
                // Display current media time & duration
                String currentMediaTimeStr = "Media Time: " +
                (int)m_oadCore.getPlaybackPositionSecs() + 
                " secs, Duration: " +  (int)m_oadCore.getPlaybackDurationSecs();
                g.drawString(currentMediaTimeStr, 10, RiExerciserConstants.FULL_PAGE_MEDIA_TIME_Y);                    

                float completionRatio = 1.0f;
                long playbackDuration = m_oadCore.getPlaybackDurationNS();
                if (playbackDuration > 0)
                {
                    completionRatio = (float)m_oadCore.getPlaybackPosition()/(float)playbackDuration;

                    m_playbackIndicator.setCompletionRatio(completionRatio);
                }
            }
            else
            {
                String currentMediaTimeStr = "Media Time: " +
                (int)m_oadCore.getPlaybackPositionSecs() + " secs";
                g.drawString(currentMediaTimeStr, 10, RiExerciserConstants.FULL_PAGE_MEDIA_TIME_Y);   
            }
        }

        super.paint(g);        
    }
    
    /**
     * Updates the status box based on current state of playback.
     * 
     * @param playerControllerState current state of playback
     */
    public void updatePlaybackStatus(boolean repaint)
    {
        int playbackState = m_oadCore.playbackGetState();
        
        //TODO: OCORI-5103: shadow state...playback state should be updated in core instead
        // This is necessary until OCORI-5146 is resolved
        if ((m_hasStarted) && (m_hasFailed))
        {
            playbackState = OcapAppDriverCore.PLAYBACK_STATE_FAILED;
        }
        
        Color updateColor = COLOR_PENDING;
        String message = MSG_PENDING;
        switch (playbackState)
        {
            case OcapAppDriverCore.PLAYBACK_STATE_BEGINNING_OF_CONTENT:
            {
                updateColor = COLOR_PAUSED;
                message = MSG_BOS;
                break;
            }
            case OcapAppDriverCore.PLAYBACK_STATE_END_OF_CONTENT:
            {
                updateColor = COLOR_PAUSED;
                message = MSG_EOS;
                break;
            }
            case OcapAppDriverCore.PLAYBACK_STATE_PRESENTING:
            {
                updateColor = COLOR_PLAY;
                
                // Handle case where started but rate is 0
                float rate = m_oadCore.getPlaybackRate();
                if (rate == 0.0)
                {
                    updateColor = COLOR_PAUSED;
                    message = MSG_PAUSED;
                }
                else if (rate == 1.0)
                {
                    message = MSG_PLAY;
                }
                else if (rate == 0.5)
                {
                    message = MSG_SLOW_FWD + rate;
                }
                else if (rate == -0.5)
                {
                    message = MSG_SLOW_REWIND + rate;
                }
                else if (rate > 0.0)
                {
                    message = MSG_FAST_FWD + rate;
                }
                else if (rate < 0.0)
                {
                    message = MSG_REWIND + rate;
                }
                    
                break;
            }
            case OcapAppDriverCore.PLAYBACK_STATE_PAUSED:
            {
                updateColor = COLOR_PAUSED;
                message = MSG_PAUSED;
                break;
            }
            case OcapAppDriverCore.PLAYBACK_STATE_FAILED:
            {
                updateColor = COLOR_FAILED;
                message = MSG_FAILED;
                if ((m_hasStarted) && (!m_hasFailed))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("updatePlaybackStatus() - saw playback in failed state");
                    }
                    m_hasFailed = true;
                }
                break;
            }
            case OcapAppDriverCore.PLAYBACK_STATE_UNKNOWN:
            {
                message = MSG_PENDING;
                updateColor = COLOR_PENDING;
                break;
            }
            default:
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStatus() - unsupported state: " + 
                            playbackState);
                }
                break;
        }

        m_statusBox.update(updateColor, message);
        if (repaint)
        {
            m_statusBox.repaint();
        }
    }
}
