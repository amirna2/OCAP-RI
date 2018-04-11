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

package org.cablelabs.xlet.RiExerciser;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.tv.xlet.XletContext;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.OcapAppDriverInterfaceCore;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverInterfaceDVR;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverInterfaceHN;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverHNDVR;
import org.cablelabs.lib.utils.oad.hndvr.OcapAppDriverInterfaceHNDVR;
import org.cablelabs.xlet.RiExerciser.ui.DvrDeleteMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.DvrNonSelectedChannelMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.DvrPlaybackMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNContentTransformationMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNContentTransformationRecordingPage;
import org.cablelabs.xlet.RiExerciser.ui.HNContentTransformationChannelPage;
import org.cablelabs.xlet.RiExerciser.ui.HNDiagnosticsPage;
import org.cablelabs.xlet.RiExerciser.ui.HNDlnaCttTestPage;
import org.cablelabs.xlet.RiExerciser.ui.HNEncryptedRecordingMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNGeneralMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNNetAuthorizationHandlerMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNPlaybackPage;
import org.cablelabs.xlet.RiExerciser.ui.HNPublishChannelMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNPublishRecordingMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNTestPage;
import org.cablelabs.xlet.RiExerciser.ui.MediaControlPage;
import org.cablelabs.xlet.RiExerciser.ui.DvrMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.GeneralMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNPlayerMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.HNServerMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.Page;
import org.cablelabs.xlet.RiExerciser.ui.RecordingMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.RemoteUIServerManagerPage;
import org.cablelabs.xlet.RiExerciser.ui.RiExerciserContainer;
import org.cablelabs.xlet.RiExerciser.ui.VPOPClientMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.VPOPServerMenuPage;
import org.cablelabs.xlet.RiExerciser.ui.VPOPTunerMenuPage;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.ocap.ui.event.OCRcEvent;

/**
 * 
 * @author Nicolas Metts
 * This class acts as the controller for the RiExerciser Xlet. The main functions
 * of this class include logging messages, listening for KeyEvents, loading
 * menu pages, and sending updates to RiExerciserContainer in order to modify the
 * GUI as needed. This class also finds defined properties such as channel maps,
 * and enabling of extensions and instantiates the various OcapAppDriver classes,
 * depending on what extensions are enabled. OcapAppDriverCore is always 
 * instantiated, but OcapAppDriverDVR, OcapAppDriverHN, and OcapAppDriverHNDVR
 * are only instantiated when their respective extensions are enabled. In general, 
 * This class passes KeyEvents to the active Page, but handles events such as
 * changing channels and pausing/stopping/resuming playback by making calls
 * directly to OcapAppDriverCore.
 *
 */
public class RiExerciserController implements KeyListener, UserEventListener
{
    /**
     * The singleton instance of this RiExerciserController
     */
    private static RiExerciserController s_controller = new RiExerciserController();
    
    private static final Logger log = Logger.getLogger(RiExerciserController.class);
    
    /** 
     * A Map of the Pages 
     */
    private HashMap m_pageMap;
    
    /**
     * The current Page
     */
    private Page m_currentPage;
    
    /**
     * OcapAppDriverInterfaceCore provides method signatures of core functionality like
     * tuning, pausing/stopping playback, changing channels, and listening for ServiceContext
     * and Controller events. Regardless of what extensions are enabled, this Object will 
     * always be instantiated
     */
    private OcapAppDriverInterfaceCore m_oadIfCore;
    
    /**
     * OcapAppDriverInterfaceHn provides method signatures of functionality like
     * find media servers, searching for content on media servers, listening for DeviceEvents,
     * and other HN functionality. This Object will only be instantiated if HN 
     * extensions are enabled. Otherwise, it will be null. Before calling methods
     * on this Object, it should be verified that m_hnExtEnabled is true in order
     * to prevent a NullPointerException 
     */
    private OcapAppDriverInterfaceHN m_oadIfHN;
    
    
    /**
     * OcapAppDriverInterfaceDVR provides method signatures of DVR functionality such as 
     * making recordings, enabling TSB, deleting recordings, a issuing buffering requests. This 
     * Object will only be instantiated if DVR extensions are enabled. Before
     * calling methods on this Object, it should be verified that m_dvrExtEnabled
     * is true in order to prevent a NullPointerException.
     */
    private OcapAppDriverInterfaceDVR m_oadIfDVR;
    
    /**
     * OcapAppDriverInterfaceHNDVR provides method signatures of HN/DVR cross-functionality such
     *  as publishing recordings and logging uses of ContentItems. This Object will only be 
     * instantiated if DVR and HN extensions are enabled. Before calling methods
     * on this Object, it should be verified that m_dvrExtEnabled and m_hnExtEnabled
     * are both true in order to prevent a NullPointerException.
     */
    private OcapAppDriverInterfaceHNDVR m_oadIfHNDVR;
    
    /**
     * The index of the current service
     */
    private int m_serviceIndex;
    
    /**
     * The number of tuners available
     */
    private int m_tuners;
    
    /**
     * The number of services available
     */
    private int m_numServices;
    
    /**
     * The index of the playback rate
     */
    private int m_playRateIndex;
    
    /**
     * A boolean indicating whether a service was selected
     */
    private boolean m_serviceSelected;
    
    /**
     * A boolean indicating whether the ServiceContext tuned or not
     */
    private boolean m_tuned;
    
    /**
     * A boolean indicating whether the controller is listening for events
     */
    private boolean m_listeningForEvents;
    
    /**
     * A String representing the name of the current Page
     */
    private String m_currentPageName = "";
    
    /**
     * An integer representing the state of playback
     */
    private int m_playbackState;
    
    /**
     * An integer representing the type of player used such
     * as JMF or service selection
     */
    private int m_playerType;
        
    /**
     * An integer representing the current video mode
     */
    private int m_videoMode;
    
    // Static final integers to represent m_videoMode state 
    public static final int LIVE_TUNING_MODE = 0;
    
    public static final int REMOTE_PLAYBACK_MODE = 1;
    
    public static final int DVR_PLAYBACK_MODE = 2;
    
    public static final int RECORDING_MODE = 3;
    
    // Static final integers to represent m_playbackState
    public static final int PLAYBACK_STOPPED = 0;
    
    public static final int PLAYBACK_PAUSED = 1;
    
    public static final int PLAYBACK_PRESENTING = 2;
    
    // The maximum index for playrates in the playrates Array
    public static final int MAX_RATE_INDEX = 15;
    
    /**
     * The name of the config file argument
     */
    private static final String CONFIG_FILE = "config_file";
    
    /**
     * The name of the use javatv channel map argument
     */
    private static final String USE_JAVA_TV = "use_javatv_channel_map";
    
    /**
     * The name of the publish all recordings argument
     */
    private static final String PUBLISH_ALL_RECORDINGS = "publish_all_recordings";

    /**
     * The name of the publish channels argument
     */
    private static final String AUTO_PUBLISH_WAIT_MS = "auto_publish_wait_time_ms";

    /**
     * The name of the publish channels argument
     */
    private static final String PUBLISH_CHANNELS = "publish_channels";

    /**
     * The publish channels argument value for the all channels
     */
    private static final String PUBLISH_CHANNELS_ALL = "all";

    /**
     * The publish channels argument value for the curent channel
     */
    private static final String PUBLISH_CHANNELS_CURRENT = "current";
    
    /**
     * The name of the auto server init argument
     */
    private static final String AUTO_SERVER_INIT = "auto_server_init";
    
    /**
     * The name of the auto record init argument
     */
    private static final String AUTO_RECORD_INIT = "auto_record_init";
    
    /**
     * The name of the auto record init length argument
     */
    private static final String AUTO_RECORD_INIT_LENGTH = "auto_record_init_length";    
    /**
     * The name of the local media server timeout argument
     */
    private static final String LOCAL_SERVER_TIMEOUT_SECS = "local_server_timeout_secs";
    
    /**
     * The name of the player timeout argument
     */
    private static final String PLAYER_TIMEOUT_SECS = "player_timeout_secs";
    
    /**
     * The name of the HN action timeout argument
     */
    private static final String HN_ACTION_TIMEOUT_MS = "hn_action_timeout_ms";
    
    /**
     * The time to wait for TUNED state
     */
    private static final int TUNE_WAIT_TIME = 20;
    
    /**
     * The value of the config file argument
     */
    private String m_strChannelFile = "";
    
    /**
     * A boolean indicating whether auto server init is true or not
     */
    private boolean m_autoServerInit;
    
    /**
     * A boolean indicating whether the channel intially tuned to should be recorded
     */
    private boolean m_autoRecordInit;
    
    /**
     * The length of the recording to make if auto_record_init is true
     */
    private int m_autoRecordInitLength;

    /**
     * The number of milliseconds to wait before auto publish
     */
    private long m_publishWaitTimeMS = 5000;

    /**
     * A boolean indicating whether all recordings should be published
     */
    private boolean m_publishAllRecordings;

    /**
     * A boolean indicating whether all channels should be published
     */
    private String m_publishChannels;

    /**
     * An ArgParser to find Xlet arguments
     */
    private ArgParser m_args;
    
    /**
     * The RiExerciserContainer used for managing UI
     */
    private RiExerciserContainer m_container;
    
    /**
     * A boolean indicating that the InteractiveResourceUsageList is active
     */
    private boolean m_ruListActive;
    
    /**
     * A boolean indicating whether tuning is synced or not
     */
    private boolean m_tuneSynced;
    
    /**
     * A boolean indicating whether DVR extensions are enabled
     */
    private boolean m_dvrExtEnabled;

    /**
     * A boolean indicating whether HN extensions are enabled
     */
    private boolean m_hnExtEnabled;
    
    /**
     * Indicates whether the Net Authorization handler is active
     */
    private boolean m_netAuthHandlerActive;
    
    /**
     * Indicates whether OcapAppDriverInterfaceHN is registered as a listener
     * for Transformation events
     */
    private boolean m_transformationEventsActive;
    
    /**
     * Timeout in seconds to wait for local media server to be discovered
     */
    public long m_localServerTimeoutSecs = 30;
    
    /**
     * Timeout in seconds to wait for player to do state transition
     */
    public long m_playerTimeoutSecs = 10;
    
    /**
     * Timeout in milliseconds to wait for HN action to complete
     */
    public long m_hnActionTimeoutMS = 15000;
    
    private RiExerciserController()
    {
        m_serviceIndex = 0;
        m_playRateIndex = 9;
        m_listeningForEvents = false;
    }
    
    public void init(XletContext ctx)
    {   
        m_container = RiExerciserContainer.getInstance();
        displayMessage("RiExerciserController.init()");
        
        // Initialize with the arguments discovered in
        // hostapp.properties
        configureXletProperties(ctx);
        boolean useJavaTVChannelMap = configureChannelMap();
        
        String persistentRoot = System.getProperty("dvb.persistent.root");
        String oid = (String) ctx.getXletProperty("dvb.org.id");
        String aid = (String) ctx.getXletProperty("dvb.app.id");
        
        // Initialize OcapAppDriverCore with the channel map properties discovered,
        // and resource contention handling enabled
        m_oadIfCore = OcapAppDriverCore.getOADCoreInterface();
        m_oadIfCore.initChannelMap(useJavaTVChannelMap, m_strChannelFile);
        m_oadIfCore.setResourceContentionHandling(true);
        // Set organization name for playback authentication
        m_oadIfCore.setOrganization(oid);
        
        m_tuners = m_oadIfCore.getNumTuners();
        // Enable DVR and HN extensions if specified
        enableExtensions();
        
        // Initialize the HScene
        m_container.initGUI();
        m_container.addKeyListener(this);
        m_container.addSceneKeyListener(this);
        m_container.addSceneComponentListener(new ComponentListener()
        {
            public void componentResized(ComponentEvent e)
            {

            }

            public void componentMoved(ComponentEvent e)
            {

            }

            public void componentShown(ComponentEvent e)
            {
                m_container.show();
            }

            public void componentHidden(ComponentEvent e)
            {

            }
        });

        m_container.setSceneSize(RiExerciserConstants.QUARTER_PAGE_WIDTH, RiExerciserConstants.QUARTER_PAGE_HEIGHT);
        
        // Set TSB to enabled by default
        if (m_dvrExtEnabled)
        {
            m_oadIfDVR.tsbControl(true);
        }
        
        // Initialize m_pageMap
        m_container.show();
        m_pageMap = new HashMap();
        loadPages();

        displayMessage("GUI initialized");
        displayMessage("Number of tuners: " + m_tuners);
        m_numServices = m_oadIfCore.getNumServices();
        displayMessage("Number of services: " + m_numServices);
        
        // Set the size of the Player to occupy 1/4 of the screen
        m_oadIfCore.playbackTransformVideo(.5f, .5f, 0, 0);
        
        // If Services are defined, tune to index 0 and initialize the GUI
        if (m_numServices > 0)
        {
            m_container.addToScene((Page)m_pageMap.get(RiExerciserConstants.GENERAL_MENU_PAGE));
            m_currentPage = (Page)m_pageMap.get(RiExerciserConstants.GENERAL_MENU_PAGE);
            m_currentPageName = RiExerciserConstants.GENERAL_MENU_PAGE;
            m_currentPage.setVisible(true);
            m_currentPage.repaint();
            m_container.repaint();
            m_container.requestSceneFocus();
            try
            {
                String autoRecordInit = m_args.getStringArg(AUTO_RECORD_INIT);
                m_autoRecordInitLength = m_args.getIntArg(AUTO_RECORD_INIT_LENGTH);
                m_autoRecordInit = autoRecordInit.equalsIgnoreCase("true");
            }
            catch(Exception ex)
            {
                if (log.isDebugEnabled())
                {
                	log.debug("Exception trying to read autoRecordInit arguments: ", ex);
                }
            }
            
            if (m_autoRecordInit && m_autoRecordInitLength > 0 && m_dvrExtEnabled)
            {
                if (autoRecordInit())
                {
                    displayMessage("Successfully recorded initial Service");
                }
                else
                {
                    displayMessage("Failed to record initial service");
                }
            }
            else
            {
                m_serviceSelected = m_oadIfCore.serviceSelectByIndex(m_serviceIndex);
                displayMessage(m_serviceSelected + " = ServiceSelectByIndex(tuner-0, " 
                        + m_serviceIndex + ")");
            }

            m_videoMode = LIVE_TUNING_MODE;
            m_playerType = OcapAppDriverInterfaceCore.PLAYBACK_TYPE_SERVICE;
            m_playbackState = PLAYBACK_PRESENTING;
            m_ruListActive = false;
            m_tuneSynced = false;
            m_netAuthHandlerActive = false;
            m_transformationEventsActive = false;
            m_autoServerInit = false;
            m_publishAllRecordings = false;
            m_publishChannels = "";
            
            // Publish any existing remote recordings
            if (m_dvrExtEnabled && m_hnExtEnabled)
            {
                if (publishRemoteRecordings())
                {
                    displayMessage("All Remote Recordings Published");
                }
            }
            
            try
            {
                String serverInit = m_args.getStringArg(AUTO_SERVER_INIT);
                m_autoServerInit = serverInit.equalsIgnoreCase("true");
            }
            catch(Exception e)
            {
                if (log.isDebugEnabled())
                {
                	log.debug("Exception trying to read autoServerInit argument: ", e);
                }
            }
            try
            {
                final String publishAllRecordingsString = m_args.getStringArg(PUBLISH_ALL_RECORDINGS);
                m_publishAllRecordings = publishAllRecordingsString.equalsIgnoreCase("true");
            }
            
            catch (Exception e1)
            {
                if (log.isDebugEnabled())
                {
                	log.debug("Exception trying to read publishAllRecordings argument: ", e1);
                }
            }
            
            try
            {
                m_publishChannels = m_args.getStringArg(PUBLISH_CHANNELS);
            }
            
            catch (Exception e2)
            {
                if (log.isDebugEnabled())
                {
                	log.debug("Exception trying to read publishChannels argument: ", e2);
                }
            }
            
            try
            {
                m_publishWaitTimeMS = new Long(m_args.getStringArg(AUTO_PUBLISH_WAIT_MS)).longValue();
            }
            
            catch (Exception e3)
            {
                if (log.isDebugEnabled())
                {
                	log.debug("Exception trying to read publishWaitTime argument: ", e3);
                }
            }

            // TODO - Remove this once the race condition has been fixed
            if (m_publishAllRecordings
                    || m_publishChannels.equals(PUBLISH_CHANNELS_ALL)
                    || m_publishChannels.equals(PUBLISH_CHANNELS_CURRENT))
            {
                try
                {
                    Thread.sleep(m_publishWaitTimeMS);
                }
                catch (InterruptedException e)
                {
                    // continue
                }
            }

            // publish all recordings and channels if auto_server_init is true
            if (m_autoServerInit)
            {
                autoServerInit();
            }
            // If both publish_all_recordings and auto_server_init are specified,
            // only perform auto_server_init
            else if (m_publishAllRecordings)
            {
                publishAllRecordings();
            }
            // If publish_all_channels is specified, publish all channels
            if (m_publishChannels.equals(PUBLISH_CHANNELS_ALL))
            {
                displayMessage("Publishing all channels");
                HNPublishChannelMenuPage.getInstance().publishAllChannels();
            }
            else if (m_publishChannels.equals(PUBLISH_CHANNELS_CURRENT))
            {
                displayMessage("Publishing current channel");
                HNPublishChannelMenuPage.getInstance().publishCurrentChannel(false);
            }
            listenForEvents();
            run();
        }
        else
        {
            displayMessage("No services defined.");
            displayMessage("Restart with Java TV Channel Map or properties file");
        }
    }
    
    private boolean publishRemoteRecordings()
    {
        int numRecordings = m_oadIfDVR.getNumRecordings();
        for (int i = 0; i < numRecordings; i++)
        {
            if (m_oadIfHNDVR.isRemoteScheduledRecording(i))
            {
                if (!m_oadIfHNDVR.publishRecording(i, m_hnActionTimeoutMS, false))
                {
                    displayMessage("Failed to publish remote recording " + i);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * This method tunes to the Service at index 0, waits for the Service to reach
     * the TUNED state, then makes a recording of the specified length
     * (AUTO_RECORD_INIT_LENGTH) if AUTO_RECORD_INIT is set to true.
     * @return true if a recording of the Service at index 0 completes, false otherwise
     */
    private boolean autoRecordInit()
    {
        m_oadIfCore.serviceSelectByIndex(0);
        {
            if (m_oadIfCore.waitForTuningState(TUNE_WAIT_TIME, OcapAppDriverCore.TUNED))
            {
                displayMessage("Beginning recording of " + m_autoRecordInitLength + " seconds");
                if (m_oadIfDVR.recordTuner(0, m_autoRecordInitLength, 0, false))
                {
                    return m_oadIfDVR.waitForRecordingState
                        (0, m_autoRecordInitLength + 10, OcapAppDriverDVR.COMPLETED_STATE);
                }
                else
                {
                    return false;
                }
            }
        }
        displayMessage("Failed to tune to initial service. No recording started.");
        return false;
    }

    /**
     * Assigns member variables based on values supplied via hostapp.properties.
     * 
     * @param ctx   xlet context which has properties
     */
    private void configureXletProperties(XletContext ctx)
    {
        try
        {
            m_args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));

            String tmp = null;

            // Get timeout to wait for media server
            try
            {
                tmp = m_args.getStringArg(LOCAL_SERVER_TIMEOUT_SECS);
                if (tmp != null)
                {
                    try
                    {
                        m_localServerTimeoutSecs = Long.parseLong(tmp);
                        displayMessage("Time to wait for local media server in secs -> " + m_localServerTimeoutSecs);
                    }
                    catch (NumberFormatException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("configureXletProperties() - unable to parse arg " + LOCAL_SERVER_TIMEOUT_SECS +
                                    " into numeric value: " + tmp + ", leaving at default value: " + m_localServerTimeoutSecs);
                        }
                    }               
                }
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("configureXletProperties() - no value specified for " + LOCAL_SERVER_TIMEOUT_SECS +
                            ", leaving at default value: " + m_localServerTimeoutSecs);
                }
            }

            // Get timeout to wait for player to do state transitions
            try
            {
                tmp = m_args.getStringArg(PLAYER_TIMEOUT_SECS);
                if (tmp != null)
                {
                    try
                    {
                        m_playerTimeoutSecs = Long.parseLong(tmp);
                        displayMessage("Time to wait for player state transitions in secs -> " + m_playerTimeoutSecs);
                    }
                    catch (NumberFormatException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("configureXletProperties() - unable to parse arg " + PLAYER_TIMEOUT_SECS +
                                    " into numeric value: " + tmp + ", leaving at default value: " + m_playerTimeoutSecs);
                        }
                    }               
                }                
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("configureXletProperties() - no value specified for " + PLAYER_TIMEOUT_SECS +
                            ", leaving at default value: " + m_playerTimeoutSecs);
                }
            }
            
            // Get timeout to wait for HN actions to complete
            try
            {
                tmp = m_args.getStringArg(HN_ACTION_TIMEOUT_MS);
                if (tmp != null)
                {
                    try
                    {
                        m_hnActionTimeoutMS = Long.parseLong(tmp);
                        displayMessage("Time to wait for HN actions to complete in ms -> " + m_hnActionTimeoutMS);
                    }
                    catch (NumberFormatException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("configureXletProperties() - unable to parse arg " + HN_ACTION_TIMEOUT_MS +
                                    " into numeric value: " + tmp + ", leaving at default value: " + m_hnActionTimeoutMS);
                        }
                    }               
                }                
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("configureXletProperties() - no value specified for " + PLAYER_TIMEOUT_SECS +
                            ", leaving at default value: " + m_playerTimeoutSecs);
                }
            }
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("configureXletProperties() - exception getting xlet properties: ", e);
            }
        }
    }
    
    /**
     * This method attempts to find a channel map from a configuration file 
     * as designated by the config_file arg in hostapp.properties. It also 
     * looks for the use_javatv_channel_map arg. This method returns true if
     * use_javatv_channel_map is true and false otherwise.
     */
    private boolean configureChannelMap()
    {
        boolean useJavaTVChannelMap = false;
        
        try
        {
            // Get the value of the config_file arg
            m_strChannelFile = m_args.getStringArg(CONFIG_FILE);
            displayMessage("RiExerciser channel file -> " + m_strChannelFile);

            FileInputStream fis = new FileInputStream(m_strChannelFile);
            ArgParser fopts = new ArgParser(fis);
            fis.close();

            // Check to see if we should use the Java TV channel map.
            try
            {
                String value = fopts.getStringArg(USE_JAVA_TV);
                if (value.equalsIgnoreCase("true"))
                {
                    useJavaTVChannelMap = true;
                }
                else if (value.equalsIgnoreCase("false"))
                {
                    useJavaTVChannelMap = false;
                }
                else
                {
                    useJavaTVChannelMap = false;
                }
            }
            catch (Exception e)
            {
                useJavaTVChannelMap = false;
                displayMessage("Exception in configureChannelMap " + e);
            }
        }
        catch (FileNotFoundException fnfex)
        {
            displayMessage("Rixerciser channel file " + m_strChannelFile + " not found");
            m_strChannelFile = "";
        }
        catch (IOException e)
        {
            displayMessage(e.toString());
        }
        catch (Exception e)
        {
            displayMessage("Unable to find config_file arg");
        }
        return useJavaTVChannelMap;
    }

    /**
     * Search for a auto_server_init arg in hostapp.properties
     */
    private void autoServerInit()
    {
        displayMessage("Auto server init");
        if (m_dvrExtEnabled && m_hnExtEnabled)
        {
            publishAllRecordings();
            boolean channelsPublished = m_oadIfHN.publishAllServices(m_hnActionTimeoutMS);
            if (channelsPublished)
            {
                int numPublishedContent = m_oadIfHN.getNumPublishedContentItems();
                for (int i = 0; i < numPublishedContent; i++)
                {
                    String publishedContent = m_oadIfHN.getPublishedContentString();
                    displayMessage(publishedContent);
                }
                displayMessage(m_numServices + " channels published");
            }
            else
            {
                displayMessage("Error publishing channels");
            }
        }
    }

    private void publishAllRecordings()
    {
        m_oadIfHN.waitForLocalContentServerNetModule(30);
        int localMediaServerIndex = m_oadIfHN.findLocalMediaServer();
        int numRecordings = m_oadIfDVR.getNumRecordings();
        if (numRecordings > 0 && localMediaServerIndex >= 0)
        {
            HNPlayerMenuPage playerMenuPage = HNPlayerMenuPage.getInstance();
            playerMenuPage.setMediaServerIndex(localMediaServerIndex);
            boolean recordingsPublished;
            recordingsPublished = m_oadIfHNDVR.publishAllRecordings(m_hnActionTimeoutMS, false);
            if (recordingsPublished)
            {
                displayMessage("Publishing all recordings");
                int numPublishedContent = m_oadIfHN.getNumPublishedContentItems();
                for (int i = 0; i < numPublishedContent; i++)
                {
                    String publishedContent = m_oadIfHN.getPublishedContentString();
                    displayMessage(publishedContent);
                }
                displayMessage(numRecordings + " recordings published");
            }
            else
            {
                displayMessage("Error publishing recordings");
            }
        }
        if (numRecordings == 0)
        {
            displayMessage("No recordings to publish");
        }
        if (localMediaServerIndex == -1)
        {
            displayMessage("Unable to find local media server");
        }
    }
    
    private void enableExtensions()
    {
        m_dvrExtEnabled = true;
        // Determine if DVR extension is enabled
        if (System.getProperty("ocap.api.option.dvr") == null)
        {
            m_dvrExtEnabled = false;            
        }
        else
        {
            m_oadIfDVR = OcapAppDriverDVR.getOADDVRInterface();
            m_oadIfDVR.setNumTuners(m_tuners);
        }
        
        // Determine if HN Extension is enabled
        m_hnExtEnabled = true;
        if (System.getProperty("ocap.api.option.hn") == null)
        {
            m_hnExtEnabled = false;            
        }
        else
        {
            m_oadIfHN = OcapAppDriverHN.getOADHNInterface();
            if (m_dvrExtEnabled)
            {
                m_oadIfHNDVR = OcapAppDriverHNDVR.getOADHNDVRInterface();
            }
        }
    }
    
    /**
     * Added to periodically repaint the HScene, m_currentPage, and Messenger
     * as well as to post any new ServiceContextEvent messages received in 
     * OcapAppDriverCore
     */
    private void run()
    {
        new Thread()
        {
            public void run()
            {
                int lastEventIndex = 0;
                while(true)
                {
                    try
                    {
                        Thread.sleep(100);
                        if (m_oadIfCore.resourceContentionActive())
                        {
                            if (!m_ruListActive)
                            {
                                m_container.showResourceUsageList();
                                m_ruListActive = true;
                            }
                        }
                        // Check for NetAuthorizationHandler messages and log 
                        // them to GUI
                        if (m_hnExtEnabled && m_netAuthHandlerActive)
                        {
                            int numNetAuthMessages = m_oadIfHN.getNumNotifyMessages();
                            for (int i = 0; i < numNetAuthMessages; i++)
                            {
                                displayMessage(m_oadIfHN.getNotifyMessage(0));
                            }
                        }
                        
                        // Check for any TransformationEvents
                        if (m_hnExtEnabled && m_transformationEventsActive)
                        {
                            int numTransformationMessages = m_oadIfHN.getNumTransformationEvents();
                            for (int i = 0; i < numTransformationMessages; i++)
                            {
                                String message = m_oadIfHN.getNextTransformationEventString();
                                displayMessage(message);
                            }
                        }
                        
                        // Check for any new ServiceContextEvent messages
                        int curEventIndex = m_oadIfCore.playbackEventGetLastIndex();
                        
                        while (lastEventIndex <= curEventIndex)
                        {
                            String description =  m_oadIfCore.playbackEventGetDescription(lastEventIndex);
                            if (description != null)
                            {
                                displayMessage(description);
                            }
                            lastEventIndex++;
                        }
                        m_container.repaint();
                        m_container.repaintScene();
                    }
                    catch (InterruptedException e)
                    {
                
                    }
                }
            }
        }.start();
    }    
    
    // Initialize m_pageMap with the key being the page name from RiExerciserConstants
    // and the value being the Page Object referred to
    private void loadPages()
    {
        m_pageMap.put(RiExerciserConstants.GENERAL_MENU_PAGE, GeneralMenuPage.getInstance());
        
        // Load the DVR pages if the DVR Extension is enabled
        if (m_dvrExtEnabled)
        {
            m_pageMap.put(RiExerciserConstants.DVR_MENU_PAGE, DvrMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.RECORDING_MENU_PAGE, RecordingMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.DVR_PLAYBACK_PAGE, DvrPlaybackMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.MEDIA_CONTROL_PAGE, MediaControlPage.getInstance());
            m_pageMap.put(RiExerciserConstants.DVR_DELETE_MENU_PAGE, DvrDeleteMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.DVR_NON_SELECTED_CHANNEL_MENU_PAGE, 
                    DvrNonSelectedChannelMenuPage.getInstance());

        }
        
        // Load the HN pages if the HN extension is enabled
        if (m_hnExtEnabled)
        {
            m_pageMap.put(RiExerciserConstants.HN_GENERAL_MENU_PAGE, HNGeneralMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_SERVER_OPTIONS_PAGE, HNServerMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_PLAYER_OPTIONS_PAGE, HNPlayerMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_DIAGNOSTICS_PAGE, HNDiagnosticsPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_TEST_PAGE, HNTestPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_DLNA_CTT_TEST_PAGE, HNDlnaCttTestPage.getInstance());
            m_pageMap.put(RiExerciserConstants.VPOP_CLIENT_MENU_PAGE, VPOPClientMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.VPOP_SERVER_MENU_PAGE, VPOPServerMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.VPOP_TUNER_MENU_PAGE, VPOPTunerMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_PLAYBACK_PAGE, HNPlaybackPage.getInstance());
            m_pageMap.put(RiExerciserConstants.REMOTE_UI_SERVER_MANAGER_PAGE, 
                    RemoteUIServerManagerPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_PUBLISH_CHANNEL_MENU_PAGE, 
                    HNPublishChannelMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_NET_AUTHORIZATION_HANDLER_MENU_PAGE, 
                    HNNetAuthorizationHandlerMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_MENU_PAGE,
            		HNContentTransformationMenuPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_RECORDING_PAGE,
            		HNContentTransformationRecordingPage.getInstance());
            m_pageMap.put(RiExerciserConstants.HN_CONTENT_TRANSFORMATION_CHANNEL_PAGE,
            		HNContentTransformationChannelPage.getInstance());
           
            // Only load these pages if DVR and HN are enabled
            if (m_dvrExtEnabled)
            {
                m_pageMap.put(RiExerciserConstants.HN_PUBLISH_RECORDING_MENU_PAGE, 
                        HNPublishRecordingMenuPage.getInstance());
                m_pageMap.put(RiExerciserConstants.HN_ENCRYPTED_RECORDING_MENU_PAGE,
                        HNEncryptedRecordingMenuPage.getInstance());
            }
        }
    }
    
    /**
     * Returns the Singleton instance of RiController
     * @return The Singleton instance of this RiController
     */
    public static RiExerciserController getInstance()
    {
        return s_controller;
    }
    
    public boolean isDvrEnabled()
    {
        return m_dvrExtEnabled;
    }
    
    public boolean isHnEnabled()
    {
        return m_hnExtEnabled;
    }
    
    /**
     * A method to determine whether the tuner is tuned
     * @return true if the OcapTuner is tuned, false otherwise
     */
    public boolean isTuned()
    {
        return m_tuned;
    }
    
    /**
     * Set Net Authorization Handler to active or inactive
     * @param active indicates whether Net Authorization Handler is active or
     * inactive
     */
    public void setNetAuthHandlerActive(boolean active)
    {
        m_netAuthHandlerActive = active;
    }
    
    public void setTransformationEventsActive(boolean active)
    {
        m_transformationEventsActive = active;
    }
    
    
    /**
     * A method to toggle the boolean that indicated whether the Resource 
     * Contention List is currently active or not
     * @param active boolean indicating whether the Resource Contention List is
     * currently active
     */
    public void setResourceContentionListActive(boolean active)
    {
        m_ruListActive = active;
    }
    
    /**
     * Set the video mode to the given index. Only the static final ints defined
     * in this class should be used (LIVE_MODE, PLAYBACK_MODE, RECORDING_MODE).
     * @param index the int representing the video mode the RiExerciser xlet is
     * currently engaged in
     */
    public void setVideoMode(int index)
    {
        m_videoMode = index;
    }
    
    /**
     * Loads a new page from m_pageMap
     * @param pageName the name of the Page to be loaded
     */
    public void displayNewPage(String pageName)
    {
        Page page = (Page)m_pageMap.get(pageName);
        Page previousPage = m_currentPage;
        m_currentPage = page;
        
        if (previousPage != null)
        {
            previousPage.destroy();
            m_container.removeFromScene(previousPage);
        }
        m_currentPageName = pageName;
        m_container.addToScene(m_currentPage);
        m_container.show();
        m_currentPage.init();
        m_currentPage.setVisible(true);
        m_currentPage.repaint();
        m_container.repaint();
        m_container.requestSceneFocus();
    }
    
    public void hidePage()
    {
        m_currentPage.setVisible(false);
        m_container.repaint();
    }
    
    /**
     * Accesses the current Page that matches the 
     * @param pageName
     * @return
     */
    public Page getPage(String pageName)
    {
        return (Page)m_pageMap.get(pageName);
    }
    
    public String getCurrentPageName()
    {
        return m_currentPageName;
    }
    
    public int getVideoMode()
    {
        return m_videoMode;
    }
    
    public String getVideoModeString()
    {
        switch(m_videoMode)
        {
            case LIVE_TUNING_MODE:
            {
                return "Live";
            }
            case REMOTE_PLAYBACK_MODE:
            {
                return "Remote Playback";
            }
            case DVR_PLAYBACK_MODE:
            {
                return "DVR Playback";
            }
            default:
            {
                return "Unknown";
            }
        }
    }
    
    /**
     * A method to access the current service
     * @return the index of the current service
     */
    public int getCurrentService()
    {
        return m_serviceIndex;
    }
    
    public boolean isPlayerStopped()
    {
        return m_playbackState == PLAYBACK_STOPPED;
    }
    
    public boolean isPlayerPaused()
    {
        return m_playbackState == PLAYBACK_PAUSED;
    }
    
    public boolean isPlayerPlaying()
    {
        return m_playbackState == PLAYBACK_PRESENTING && m_videoMode == REMOTE_PLAYBACK_MODE;
    }
    
    /**
     * The key events VK_LEFT, VK_RIGHT, VK_UP, VK_DOWN, VK_PAGE_UP, and 
     * VK_PAGE_DOWN are being handled by keyPressed() in order to allow rapid 
     * selection by holding down a key, which is not possible using keyReleased()
     */
    public void keyPressed(KeyEvent arg0)
    {
        int lastEventIndex = m_oadIfCore.playbackEventGetLastIndex();
        switch (arg0.getKeyCode())
        {
            // Allows fast incrementing/decrementing of recording duration and
            // recording start time values on RecordingMenuPage 
            case OCRcEvent.VK_LEFT:
            case OCRcEvent.VK_RIGHT:
            case OCRcEvent.VK_UP:
            case OCRcEvent.VK_DOWN:
            {
                m_currentPage.processUserEvent(arg0);
                break;
            }
            
            // Allows fast tuning on all pages
            case OCRcEvent.VK_PAGE_UP:
            {
                // Only tune to a new service if in live mode
                if (m_videoMode == LIVE_TUNING_MODE)
                {
                    // Update the index of the current service
                    m_serviceIndex++;
                    m_serviceIndex %= m_numServices;
                    m_oadIfCore.channelUp();
                }
                if (m_videoMode == REMOTE_PLAYBACK_MODE)
                {
                    m_oadIfHN.playPrevious();
                }
                if (m_videoMode == DVR_PLAYBACK_MODE)
                {
                    m_oadIfDVR.playPrevious();
                }
                break;
            }
            
            // Allows fast tuning on all pages
            case OCRcEvent.VK_PAGE_DOWN:
            {
                // Only tune to a new service if in live mode
                if (m_videoMode == LIVE_TUNING_MODE)
                {
                    // Update the index of the current service
                    if (m_serviceIndex == 0)
                    {
                        m_serviceIndex = m_numServices - 1;
                    }
                    else
                    {
                        m_serviceIndex--;
                    }
                    m_oadIfCore.channelDown();
                }
                if (m_videoMode == REMOTE_PLAYBACK_MODE)
                {
                    m_oadIfHN.playNext();
                }
                if (m_videoMode == DVR_PLAYBACK_MODE)
                {
                    m_oadIfDVR.playNext();
                }
                break;
            }
        }
        
        m_container.repaint();
        m_container.repaintScene();
    }
    
    /**
     * Listen for Key Released events. For most events, the KeyEvent is simply
     * passed to m_currentPage, which decides what action to take. Regardless of
     * what page is active, channel up, channel down, play, pause, stop, rewind
     * and fast forward will be handled by the controller
     */
    public void keyReleased(KeyEvent arg0)
    {
        char released = arg0.getKeyChar();
        // Ignore non-printing characters
        if (Character.isDefined(released))
        {
            displayMessage("Received KeyEvent: " + released);
        }

        
        // If currently doing remote playback, let the relevant page handle keys
        if (m_videoMode == REMOTE_PLAYBACK_MODE)
        {
            if (m_currentPageName.equals(RiExerciserConstants.HN_PLAYBACK_PAGE))
            {
                HNPlaybackPage playerPage = (HNPlaybackPage)getPage
                        (RiExerciserConstants.HN_PLAYBACK_PAGE);
                playerPage.processUserEvent(arg0);
                return;
            }
            else if (m_currentPageName.equals(RiExerciserConstants.VPOP_CLIENT_MENU_PAGE))
            {
                VPOPClientMenuPage vpopClientPage = (VPOPClientMenuPage)getPage
                        (RiExerciserConstants.VPOP_CLIENT_MENU_PAGE);
                vpopClientPage.processUserEvent(arg0);
                return;
            }
        }
        
        switch (arg0.getKeyChar())
        {
            // Alternate between TUNER_SYNC_STATE_SYNCED and 
            // TUNER_SYNC_STATE_UNSYNCED states with the x key
            case 'x':
            case 'X':
            {
                m_tuneSynced = !m_tuneSynced;
                if (m_tuneSynced)
                {
                    m_oadIfCore.setTunerSyncState(0, true);
                    displayMessage("Tuner Sync State = Synced");
                }
                else
                {
                    m_oadIfCore.setTunerSyncState(0, false);
                    displayMessage("Tuner Sync State = Not Synced");
                }
            }
        }
        switch (arg0.getKeyCode())
        {
            case OCRcEvent.VK_CHANNEL_UP: 
            {
                // Only tune to a new service if in live mode
                if (m_videoMode == LIVE_TUNING_MODE)
                {
                	// Make sure the Service index is the same as the currently
                	// selected service
                    m_oadIfCore.resetLiveServiceIndex();
                    int tempServiceIndex = m_oadIfCore.getServiceIndex();
                    // Only update channel index and channel up if the current
                    // Service index is in the range [0, m_numServices)
                    if (tempServiceIndex >= 0 && tempServiceIndex < m_numServices)
                    {
                        m_serviceIndex = m_oadIfCore.getServiceIndex();
                        // Update the index of the current service
                        m_serviceIndex++;
                        m_serviceIndex %= m_numServices;
                        m_oadIfCore.channelUp();
                    }
                    else
                    {
                        displayMessage("Error updating channel index");
                    }
                }
                else
                {
                    //TODO: Add action for other m_videoMode states
                }
                break;
            }
            case OCRcEvent.VK_CHANNEL_DOWN: 
            {
                // Only tune to a new service if in live mode
                if (m_videoMode == LIVE_TUNING_MODE)
                {
                	// Make sure the Service index is the same as the currently
                	// selected service
                    m_oadIfCore.resetLiveServiceIndex();
                    int tempServiceIndex = m_oadIfCore.getServiceIndex();
                    // Only update channel index and channel up if the current
                    // Service index is in the range [0, m_numServices)
                    if (tempServiceIndex >= 0 && tempServiceIndex < m_numServices)
                    {
                        m_serviceIndex = m_oadIfCore.getServiceIndex();
                        // Update the index of the current service
                        if (m_serviceIndex == 0)
                        {
                            m_serviceIndex = m_numServices - 1;
                        }
                        else
                        {
                            m_serviceIndex--;
                        }
                        m_oadIfCore.channelDown();
                    }
                    else
                    {
                        displayMessage("Error updating channel index");
                    }
                }
                else
                {
                    //TODO: Add action for other m_videoMode states
                }
                break;
            }
            case OCRcEvent.VK_PLAY:
            {
                // When in paused mode, simply start the player again
                if (m_playbackState == PLAYBACK_PAUSED)
                {
                    displayMessage("Resuming playback");
                    m_oadIfCore.playbackChangeRate(OcapAppDriverCore.PLAY_RATE_INDEX_PLAY);
                    m_playbackState = PLAYBACK_PRESENTING;
                    m_playRateIndex = OcapAppDriverCore.PLAY_RATE_INDEX_PLAY;
                }
                // When playback is presenting, set rate back to 1.0 unless it
                // is already 1.0
                else if (m_playbackState == PLAYBACK_PRESENTING && 
                		m_playRateIndex != OcapAppDriverCore.PLAY_RATE_INDEX_PLAY)
                {
                	displayMessage("Attempting to set playback rate to 1.0");
                	boolean rateSet = m_oadIfCore.playbackChangeRate
                			(OcapAppDriverCore.PLAY_RATE_INDEX_PLAY);
                	if (rateSet)
                	{
                		m_playRateIndex = OcapAppDriverCore.PLAY_RATE_INDEX_PLAY;
                	}
                	float currentRate = m_oadIfCore.getPlaybackRate();
                	displayMessage("Rate set to: " + currentRate);
                }
                // When in stopped mode, whether viewing live content or 
                // DVR/remote content, tune back to the current index
                else if (m_playbackState == PLAYBACK_STOPPED)
                {   
                    displayMessage("Resuming playback");
                    tuneToCurrentIndex();
                    m_playbackState = PLAYBACK_PRESENTING;
                    m_playRateIndex = OcapAppDriverCore.PLAY_RATE_INDEX_PLAY;
                }
                break;
            }
            case OCRcEvent.VK_STOP:
            {
                if (m_playbackState == PLAYBACK_PRESENTING)
                {
                    // If in live mode, stop the video playback
                    if (m_videoMode == LIVE_TUNING_MODE)
                    {
                        displayMessage("Stopping playback");
                        boolean playbackStopped = m_oadIfCore.serviceSelectStop(15);
                        if (playbackStopped)
                        {
                            displayMessage("Playback stopped");
                            m_playbackState = PLAYBACK_STOPPED;
                        }
                    }
                    else if (m_videoMode == DVR_PLAYBACK_MODE)
                    {
                        // If in playback mode, stop video playback and tune
                        // to the current service index
                        displayMessage("Stopping playback");
                        boolean playbackStopped = m_oadIfCore.playbackStop(15);
                        if (playbackStopped)
                        {
                            displayMessage("Playback stopped");
                        }
                        m_videoMode = LIVE_TUNING_MODE;
                        m_container.stopIndicatePlayback();
                        tuneToCurrentIndex();
                        displayNewPage(m_currentPageName);
                        m_container.show();
                        m_container.repaint();
                    }
                    else if (m_videoMode == RECORDING_MODE)
                    {
                        //TODO: Stub for when recording from any page is
                        // implemented
                        m_videoMode = LIVE_TUNING_MODE;
                    }
                }
                break;
            }
            case OCRcEvent.VK_PAUSE:
            {
                // Only attempt to change rate if DVR extension is enabled
                if (m_dvrExtEnabled)
                {
                    if (m_playbackState == PLAYBACK_PRESENTING)
                    {
                        displayMessage("Pausing playback");
                        boolean paused = m_oadIfCore.playbackChangeRate(
                                OcapAppDriverCore.PLAY_RATE_INDEX_PAUSED);
                        if (paused)
                        {
                            displayMessage("Playback paused");
                            m_playbackState = PLAYBACK_PAUSED;
                        }
                        else
                        {
                            displayMessage("Pause failed");
                        }
                    }
                }
                else
                {
                    displayMessage("Trick modes only available when DVR is enabled");
                }
                break;
            }
            case OCRcEvent.VK_FAST_FWD:
            {
                // Only attempt to change rate if DVR extension is enabled
                if (m_dvrExtEnabled)
                {
                    // If a BOS or EOS event has been received, the playback rate
                    // will reset to 1.0
                    m_playRateIndex = m_oadIfCore.getPlayRateIndex(m_oadIfCore.getPlaybackRate());
                    if (m_playRateIndex < MAX_RATE_INDEX)
                    {
                    	m_playbackState = PLAYBACK_PRESENTING;
                    	int newRateIndex;
                        // If playing at slow speed reverse or -1.0x, set the new rate to 1.0x
                    	// otherwise, just increment the playRateIndex
                    	if (m_playRateIndex == OcapAppDriverCore.PLAY_RATE_INDEX_RWD_HALF ||
                    			m_playRateIndex == OcapAppDriverCore.PLAY_RATE_INDEX_RWD_1)
                    	{
                    		newRateIndex = OcapAppDriverCore.PLAY_RATE_INDEX_PLAY;
                    	}
                    	else
                    	{
                    		newRateIndex = m_playRateIndex + 1;
                    	}
                        float newRate = m_oadIfCore.getPlayRate(newRateIndex);
                        displayMessage("Attempting to set rate to: " + newRate);
                        boolean playRateChanged = m_oadIfCore.playbackChangeRate(newRateIndex);
                        if (playRateChanged)
                        {
                        	newRate = m_oadIfCore.getPlaybackRate();
                            displayMessage("Playrate changed to: " + newRate);
                            m_playRateIndex++;
                        }
                        else
                        {
                            displayMessage("Failed to change playrate");
                        }
                    }
                }
                else
                {
                    displayMessage("Trick modes only available when DVR is enabled");
                }
                break;
            }
            case OCRcEvent.VK_REWIND:
            {
                // Only attempt to change rate if DVR extension is enabled
                if (m_dvrExtEnabled)
                {
                    // If a BOS or EOS event has been received, the playback rate
                    // will reset to 1.0
                    m_playRateIndex = m_oadIfCore.getPlayRateIndex(m_oadIfCore.getPlaybackRate());
                    if (m_playRateIndex > 0)
                    {
                    	m_playbackState = PLAYBACK_PRESENTING;
                    	int newRateIndex;
                        // If playing at 1.0x, change to -1.0x. Otherwise, just decrement
                    	// the playRateIndex
                    	if (m_playRateIndex == OcapAppDriverCore.PLAY_RATE_INDEX_PLAY ||
                    			m_playRateIndex == OcapAppDriverCore.PLAY_RATE_INDEX_FWD_HALF)
                    	{
                    		newRateIndex = OcapAppDriverCore.PLAY_RATE_INDEX_RWD_1;
                    	}
                    	else
                    	{
                    		newRateIndex = m_playRateIndex - 1;
                    	}
                        float newRate = m_oadIfCore.getPlayRate(newRateIndex);
                        displayMessage("Attempting to set rate to: " + newRate);
                        boolean playRateChanged = m_oadIfCore.playbackChangeRate(newRateIndex);
                        if (playRateChanged)
                        {
                        	newRate = m_oadIfCore.getPlaybackRate();
                            displayMessage("Playrate changed to :" + newRate);
                            m_playRateIndex--;
                        }
                        else
                        {
                            displayMessage("Failed to change playrate");
                        }
                    }
                }
                else
                {
                    displayMessage("Trick modes only available when DVR is enabled");
                }
                break;
            }
            case OCRcEvent.VK_RECORD:
            {
                //TODO: Implement recording when in live mode
                if (m_videoMode == LIVE_TUNING_MODE)
                {
                    m_videoMode = RECORDING_MODE;
                }
                break;
            }
            
            case OCRcEvent.VK_LEFT:
            case OCRcEvent.VK_RIGHT:
            case OCRcEvent.VK_UP:
            case OCRcEvent.VK_DOWN:
            {
                // These events are handled in keyPressed(). This is merely a 
                // place holder to keep the events from being processed twice 
                // by the current page
                break;
            }
            
            default:
            {
                m_currentPage.processUserEvent(arg0);
                break;
            }
        }

    }
    
    /**
     * Added to satisfy KeyListener
     */
    public void keyTyped(KeyEvent arg0)
    {

    }
    
    /**
     * Used by pages to log a message to Messenger and to log
     * @param message the message to log
     */
    public void displayMessage(String message)
    {
        if (log.isInfoEnabled())
        {
            log.info(message);
        }
        m_container.addMessage(message);
        m_container.repaint();
    }
    
    /**
     * A method to tune to m_serviceIndex
     */
    public void tuneToCurrentIndex()
    {
        m_oadIfCore.playbackTransformVideo(.5f, .5f, 0, 0);
        m_serviceSelected = m_oadIfCore.serviceSelectByIndex(m_serviceIndex);
        m_playbackState = PLAYBACK_PRESENTING;
        m_videoMode = LIVE_TUNING_MODE;
    }
    
    public void listenForEvents()
    {
        // Only create a thread checking for DeviceEvents if HN extensions are enabled
        if (m_hnExtEnabled)
        {
            new Thread()
            {
                public void run()
                {
                    while(m_listeningForEvents)
                    {
                        logLastEventReceived();
                        try
                        {
                            Thread.sleep(500);
                        }
                        catch(InterruptedException e)
                        {
                
                        }
                    }
                }
            }.start();
        }
    }
    
    public void toggleListenForEvents()
    {
        m_listeningForEvents = !m_listeningForEvents;
    }
    
    private void logLastEventReceived()
    {
        String lastEvent = m_oadIfHN.getLastDeviceEvent();
        if (lastEvent != null)
        {
            displayMessage(lastEvent);
        }
    }
    
    public void displayLastTenEvents()
    {
        if (m_hnExtEnabled)
        {
            for (int i = 0; i < 10; i++)
            {
                logLastEventReceived();
            }
        }
    }

    public void userEventReceived(UserEvent e)
    {
        char keyChar = e.getKeyChar();
        displayMessage("VPOP Server Event received: " + keyChar);
    }
    
    /**
     * Returns amount of time to wait in seconds for local 
     * media server to be discovered based on default value
     * or value supplied in hostapp.properties.
     * 
     * @return  time out in seconds to wait for local server
     */
    public long getLocalServerTimeoutSecs()
    {
        return m_localServerTimeoutSecs;
    }

    /**
     * Returns amount of time to wait in seconds for player
     * to transition to next state based on default value
     * or value supplied in hostapp.properties.
     * 
     * @return  time out in seconds to wait for player state transition
     */
    public long getPlayerTimeoutSecs()
    {
        return m_playerTimeoutSecs;
    }
    
    /**
     * Returns amount of time to wait in milliseconds for an HN action
     * to complete based on default value
     * or value supplied in hostapp.properties.
     * 
     * @return  time out in milliseconds to wait for HN action to completed
     */
    public long getHNActionTimeoutMS()
    {
        return m_hnActionTimeoutMS;
    }
}
