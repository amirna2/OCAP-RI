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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.tv.service.selection.PresentationTerminatedEvent;import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;import javax.tv.service.selection.ServiceContextListener;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.ocap.OcapSystem;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.hn.Device;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListIterator;
import org.ocap.system.EASEvent;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;
import org.ocap.ui.event.OCRcEvent;

/**
 * This application is intended to exercise DVR and TSB functionality.
 * 
 * Usage notes:
 * 1. At startup time, the application will try to load a list of services 
 * from a file named 'config.properties' that is specified as a command 
 * line argument of the form 'config_file=config.properties'.  Note that 
 * this file is in the same format as that which is used by the TuneTest
 * application. 
 * 
 * 2. Once instantiated, the user can interact with the application as follows:
 * Change 'live' channels using the ChUp/ChDn keys.
 * Pause/resume content using the pause/play keys.
 * Stop presentation using the stop key.
 * 
 * 3. Pausing, then playing 'live' content will exercise the TSB playback 
 * functionality.
 * 
 * 4. Recordings can be made by pressing the 1, 2, or 3 keys to make recordings
 * of 10, 30, or 60 seconds in length, respectively.  When recording is active,
 * a flashing red recording indicator will be displayed in the upper-left of the
 * video display.
 * 
 * 5. Once a successful recording is made (as indicated by a steady red indicator 
 * in the upper-left of the video display), it can be played back using the 4 key.
 * 
 * 6. Previously recorded content can be accessed by pressing the 6 key.  This
 * will display a menu of existing recordings.  Selecting a recording using
 * the arrow and select keys will cause the recording to start playing.
 * NOTE:  One must press the 'select' key in order to get the menu to gain 
 * input focus (perhaps someone can figure out why this is required - I have
 * been unable to do so).
 * 
 * 7. Once recording playback has commenced, it can be controlled using the 
 * '<<', pause, play, and '>>' keys.
 * 
 * 8. All existing recordings can be deleted by selecting the 7 key.
 * 
 * 9. The TSB can be enabled/disabled by selecting the 8 key.  Disabling the 
 * TSB implies setting the minimum duration of TimeShiftProperties to 0, while
 * enabling the TSB implies setting the minimum duration of TimeShiftProperties
 * to some non-zero value.   
 * 
 * State definitions:
 * The application will always be in one of the following operational states:
 * a. Stopped
 *  The service context is stopped, so that nothing is playing.
 *  Transport control buttons will have no effect.
 * b. Live
 *  A 'live' service has been selected into the service context.
 *  Transport control buttons should result in expected behavior, and may
 *  result in the exercising of TSB functionality.
 * c. Playback
 *  A recorded service has been selected into the service context.
 *  Transport control buttons should result in expected behavior.
 *  
 * The Stopped state is entered by selecting the 'stop' key while in any state.
 * The Playback state is entered by playing an existing, or the current recording.
 * The Live state is entered on program startup, and by selecting the 'play' key
 * while in the Stopped state, pressing the 'live' key, or selecting the chup/chdn keys.
 * 
 */
public class DvrExerciser extends HContainer implements Xlet, Runnable, KeyListener
{
    /**
     * Added to silence the compiler
     */
    private static final long serialVersionUID = -3599127111275002970L;

    // screen dimensions
    protected static final int SCREEN_WIDTH = 640;

    protected static final int SCREEN_HEIGHT = 480;

    private static final String USE_JAVA_TV = "use_javatv_channel_map";

    // configuration file command-line identifier for tuning parameters
    private static final String CONFIG_FILE = "config_file";

    // configuration file containing tuning parameters
    private String m_strChannelFile;

    // command file command-line identifier for automated operations
    private static final String COMMAND_FILE = "command_file";

    private CommandProcessor m_commandProcessor = null;

    private static final String TEST_IFACE_UDP_PORT = "test_iface_udp_port";

    private static final String TEST_IFACE_UDP_TIMEOUT_SECS = "test_iface_udp_timeout_secs";
    private static final String USE_TEST_IFACE = "test_iface";
    private static final String AUTO_SERVER_INIT="auto_server_init";

    private int m_testIaceUDPPort = 8001;
    private int m_testIaceUDPTimeoutSecs = 2;
    private boolean m_useTestIface = false;
    private boolean m_autoServerInitialize = false;

    // various colors
    private static final Color COLOR_TEXT = Color.white;

    // shape size definitions
    private static final int SQUARE_SIDE = 250;

    // A flag indicating that the Xlet has been started.
    private boolean m_started = false;

    private boolean m_useJavaTVChannelMap = true;

    // The OCAP Xlet context.
    private XletContext m_ctx;

    // A reference to the application.
    private static DvrExerciser m_instance = null;

    // A HAVi Scene.
    protected HScene m_scene;

    private RecordingIndicator m_recordingIndicator;

    private Bargraph m_playbackIndicator;

    // manages the 'live' channel map
    private LiveContent m_liveContent;

    private static final int FONT_SIZE = 16;

    private String m_strKey = null;

    Messenger m_messenger;

    protected VidTextBox m_menuBox;

    protected int m_menuMode = 0;

    protected static final int MENU_MODE_GENERAL = 0;
    protected static final int MENU_MODE_DVR = 1;
    protected static final int MENU_MODE_HN = 2;
    protected static final int MENU_MODE_MEDIA_CONTROL = 3;
    protected static final int MENU_MODE_DVR_RECORD = 4;
    protected static final int MENU_MODE_DVR_PLAYBACK = 5;
    protected static final int MENU_MODE_DVR_DELETE = 6;
    
    private DvrHNTest m_dvrHNTest = null;
    private HNTest m_hnTest = null;
    private DvrTest m_dvrTest = null;
    private NonDvrTest m_test = null;

    private boolean m_doDisplayMenu = true;
    
    protected static String m_persistentDirStr = null;

    private boolean m_dvrExtEnabled = false;
    private boolean m_hnExtEnabled = false;

    //track if the xlet been shown initially
    boolean initialShown = false;

    // Register a ResourceContentionHandler if true
    boolean m_registerRCH = true;

    private InteractiveResourceContentionHandler m_resContentionHandler;

    /**
     * The default constructor.
     */
    public DvrExerciser()
    {
        logIt("DvrExerciser.DvrExerciser(constructor)");
        m_instance = this;
    }

    /**
     * 
     * TODO: document
     * 
     * @return
     */
    public static DvrExerciser getInstance()
    {
        return m_instance;
    }

    public DvrTest getDvrTest()
    {
        return m_dvrTest;
    }
    
    public DvrHNTest getDvrHNTest()
    {
        return m_dvrHNTest;
    }
    
    public NonDvrTest getNonDvrTest()
    {
        return m_test;
    }
        
    public boolean isDvrEnabled()
    {
        return m_dvrExtEnabled;
    }
    
    /**
     * Accessor method that allows the command processor the ability to obtain
     * the live content instance used by this application.
     * 
     * @return
     */
    public LiveContent getLiveContent()
    {
        return m_liveContent;
    }

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param ctx
     *            the context for this Xlet A reference to the context is stored
     *            for further need. This is the place where any initialization
     *            should be done, unless it takes a lot of time or resources.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialized.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
    {
        logIt("DvrExerciser.initXlet()");

        // save the xlet context
        m_ctx = ctx;

        // Determine if DVR extension is enabled
        m_dvrExtEnabled = true;
        if (System.getProperty("ocap.api.option.dvr") == null)
        {
            m_dvrExtEnabled = false;            
            m_test = new NonDvrTest();
        }
        else
        {
            m_dvrTest = DvrTest.getInstance();
            m_dvrTest.init(this);            
        }
        
        // Determine if HN Extension is enabled
        m_hnExtEnabled = true;
        if (System.getProperty("ocap.api.option.hn") == null)
        {
            m_hnExtEnabled = false;            
        }
        else
        {
            if (m_dvrExtEnabled)
            {
                m_dvrHNTest = DvrHNTest.getInstance();                         
            }
            else
            {
                m_hnTest = new HNTest(this);                         
            }
        }

        // Setup the application graphical user interface.
        initGUI();

        // Create ArgParser from Xlet arguments
        ArgParser args = null;
        try
        {
            args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));

            //
            // process any remote testing configurations
            //
            try
            {
                String temp = args.getStringArg(USE_TEST_IFACE);
                if (temp.equalsIgnoreCase("true"))
                {
                    m_useTestIface = true;
                } 
            }
            catch (Exception e)
            {
                // USE_TEST_IFACE not specified -- use default
            }
            try
            {
                int temp = args.getIntArg(TEST_IFACE_UDP_PORT);
                m_testIaceUDPPort = temp;
            }
            catch (Exception e)
            {
                // TEST_IFACE_UDP_PORT not specified -- use default
            }

            try
            {
                int temp = args.getIntArg(TEST_IFACE_UDP_TIMEOUT_SECS);
                m_testIaceUDPTimeoutSecs = temp;
            }
            catch (Exception e)
            {
                // TEST_IFACE_UDP_TIMEOUT_SECS not specified -- use default
            }

            // Build our channel list
            m_liveContent = new LiveContent();
            try
            {
                //
                // process any config file
                //
                m_strChannelFile = args.getStringArg(CONFIG_FILE);
                logIt("DvrExerciser channel file -> " + m_strChannelFile);

                FileInputStream fis = new FileInputStream(m_strChannelFile);
                ArgParser fopts = new ArgParser(fis);
                fis.close();

                // Check to see if we should use the Java TV channel map.
                try
                {
                    String value = fopts.getStringArg(USE_JAVA_TV);
                    if (value.equalsIgnoreCase("true"))
                    {
                        m_useJavaTVChannelMap = true;
                    }
                    else if (value.equalsIgnoreCase("false"))
                    {
                        m_useJavaTVChannelMap = false;
                    }
                    else
                    {
                        m_useJavaTVChannelMap = false;
                    }
                }
                catch (Exception e)
                {
                    m_useJavaTVChannelMap = false;
                }
            }
            catch (FileNotFoundException fnfex)
            {
                logIt("DvrExerciser channel file " + m_strChannelFile + " not found");
            }
            System.out.println("Use Java TV Channel Map=" + m_useJavaTVChannelMap);

            if (!m_liveContent.buildChannelVector(m_useJavaTVChannelMap, m_strChannelFile))
            {
                throw new XletStateChangeException("DvrExerciser Test Xlet Could not find any services");
            }
            // Was automatic server initialization requested?
            try
            {
                String temp = args.getStringArg(AUTO_SERVER_INIT);
                if (temp.equalsIgnoreCase("true"))
                {
                    m_autoServerInitialize = true;
                } 
            }
            catch (Exception e)
            {
                // AUTO_SERVER_INIT not specified -- use default
            }
            // create operational states
            OperationalState.initializeOperationalStates(this, m_liveContent);

            //
            // process any automated command file
            //
            try
            {
                // getArgString throws an exception if the key isn't found..
                String commandFile = args.getStringArg(COMMAND_FILE);
                logIt("Automated command file found -> " + commandFile);

                // instantiate the singleton instance of the CommandProcessor
                m_commandProcessor = CommandProcessor.getInstance();
                if (false == m_commandProcessor.processCommandFile(commandFile))
                {
                    logIt("Error processing automated command file");
                    m_commandProcessor = null;
                }
            }
            catch (Exception e)
            {
                logIt("No automated command file found");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException("Error creating ArgParser!");
        }

        // Formulate the persistent storage dir path
        String persistentRoot = System.getProperty("dvb.persistent.root");
        String oid = (String) m_ctx.getXletProperty("dvb.org.id");
        String aid = (String) m_ctx.getXletProperty("dvb.app.id");
        m_persistentDirStr = persistentRoot + "/" + oid + "/" + aid + "/";
    }

    /**
     * Starts the main application thread.
     */
    public void run()
    {
        if (m_useTestIface)
        {
            try
            {
                OcapSystem.monitorConfiguringSignal(m_testIaceUDPPort, m_testIaceUDPTimeoutSecs);
                OcapSystem.monitorConfiguredSignal();
  
                OcapTestInterfaceThread OcapTestInterfaceThread = new OcapTestInterfaceThread(this);
                OcapTestInterfaceThread.start();
            }
            catch (Exception e)
            {
                logIt("WARN: monitorConfiguredSignal failed: " +e.getMessage());
            }
        }
        
        if (m_registerRCH )
        {
            try
            {
                m_resContentionHandler = new InteractiveResourceContentionHandler(this);
                final ResourceContentionManager rcm = ResourceContentionManager.getInstance();
                rcm.setResourceContentionHandler(m_resContentionHandler);
            }
            catch (Exception e)
            {
                logIt("WARN: Failed registering RCH: " + e.getMessage());
            }
        }

        /* If requested,  auto-select as the DMS Server this running instance of
           DvrExerciser
        */
        if (m_autoServerInitialize ) {
            autoServerInit();
        }
        
        showApplication();

        // if we have a command processor...
        if (null != m_commandProcessor)
        {
            // ...start it up
            m_commandProcessor.start();
        }
    }

    /**
     * Show the application.
     * <p>
     * The HAVi scene is repainted.
     * </p>
     */
    void showApplication()
    {
        logIt("DvrExerciser.showApplication()");

        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();

        // start presenting 'live' content
        OperationalState.setLiveOperationalState();

        repaint();
    }

    /**
     * Hide the application.
     * <p>
     * The HAVi scene is set invisible.
     * </p>
     */
    void hideApplication()
    {
        logIt("DvrExerciser.hideApplication()");
        m_scene.setVisible(false);
    }

    /**
     * Dispose of the application resources.
     * <p>
     * The HAVi scene is disposed of.
     * </p>
     */
    void disposeApplication()
    {
        logIt("DvrExerciser.disposeApplication()");

        // Hide the application.
        hideApplication();

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /*
     * Initialize the application graphical user interface.
     */
    private void initGUI()
    {
        // Initialize the Container layout.
        setLayout(null);
        setSize(640, 480);

        // Create HScene and initialize it.
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_scene.setSize(SCREEN_WIDTH, SCREEN_HEIGHT); // Set the size of the
                                                      // scene, if not already
                                                      // set.
        m_scene.add(this); // Add the container to the scene.
        m_scene.addWindowListener(new WindowAdapter()
        {
            // Capture the Window closing event.
            public void windowClosing(WindowEvent e)
            {
                // System.exit(0);
            }
        });

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
                if (!initialShown)
                {
                    initialShown = true;
                }
                else
                {
                    logIt("componentShown - calling showApplication");
                    showApplication();
                }
            }

            public void componentHidden(ComponentEvent e)
            {
                logIt("componentHidden");
            }
        });

        // add a component to display log messsages
        m_messenger = new Messenger();
        m_messenger.setBounds(0, // x
                (2 * SCREEN_HEIGHT) / 4, // y
                SCREEN_WIDTH, // width
                (2 * SCREEN_HEIGHT) / 4); // height
        add(m_messenger);

        // Add menu with options
        m_menuBox = new VidTextBox(SCREEN_WIDTH / 2, 0, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2+50, 14, 5000);
        add(m_menuBox);
        m_menuBox.setBackground(new DVBColor(128, 228, 128, 155));
        m_menuBox.setForeground(new DVBColor(200, 200, 200, 255));

        // Set the default mode to be general
        m_menuMode = MENU_MODE_GENERAL;

        // Update the menu based on mode
        updateMenuBox();

        m_scene.addKeyListener(this);

        // Initialize the display characteristics.
        setFont(new Font("SansSerif", Font.BOLD, FONT_SIZE));

        m_recordingIndicator = new RecordingIndicator();
        add(m_recordingIndicator);
        m_recordingIndicator.setBounds(5, 5, 10, 10);

        m_playbackIndicator = new Bargraph();
        add(m_playbackIndicator);
        m_playbackIndicator.setBounds(0, SCREEN_HEIGHT / 2 - 10, SCREEN_WIDTH / 2, 10);
        m_playbackIndicator.setVisible(false);

        new Thread(m_recordingIndicator).start();

        validate();

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
                    repaint();

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
    }

    /**
     * Paint all.
     * 
     * @param g
     *            The <code>Graphics</code> context.
     */
    public void paint(Graphics g)
    {

        DVBGraphics dvbG = (DVBGraphics) g;
        try
        {
            // enable overlay compositing control
            dvbG.setDVBComposite(DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC));
        }
        catch (UnsupportedDrawingOperationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // set the text color
        g.setColor(COLOR_TEXT);

        // draw the any key string output on the square
        if ((null != m_strKey) && (m_doDisplayMenu))
        {
            g.drawString(m_strKey, 500, SCREEN_HEIGHT - SQUARE_SIDE / 2);
        }

        // display the current operational state
        if (m_menuMode  == MENU_MODE_HN ) {
            String friendlyName = "Unknown";
            if ( m_hnTest.m_mediaServer != null)
            {
                friendlyName = m_hnTest.m_mediaServer.getDevice().getProperty(Device.PROP_FRIENDLY_NAME);
            }
            StringBuffer sbTsb = new StringBuffer("Server: "+ friendlyName);
            g.drawString(sbTsb.toString(), 10, SCREEN_HEIGHT / 2 - 5);
            
            StringBuffer sbTsb2 = new StringBuffer("Published:");
            sbTsb2.append ( (m_dvrHNTest.m_channelPublished || m_dvrHNTest.m_recordingPublished)
                 ? "Yes" : "No" );
            g.drawString(sbTsb2.toString(), 10, SCREEN_HEIGHT / 2 - 20);
            
        } else if (m_dvrExtEnabled)
        {
            OperationalState currentOperationalState = OperationalState.getCurrentOperationalState();
            if ((null != currentOperationalState) && (m_doDisplayMenu))
            {
                float playRate = currentOperationalState.getPlayRate();
                String paused = 0.0 == playRate ? " (paused)" : "";
                g.drawString("Mode: " + currentOperationalState.getName() + ", Rate: " + playRate + paused, 10,
                    SCREEN_HEIGHT / 2 - 20);
            }

            // display the current state of the TSB
            if (m_doDisplayMenu)
            {
                StringBuffer sbTsb = new StringBuffer("TSB state: ");
                sbTsb.append(m_dvrTest.isTsbEnabled() ? "Enabled" : "Disabled");
                g.drawString(sbTsb.toString(), 10, SCREEN_HEIGHT / 2 - 5);
            }
        }
        super.paint(g);
    }

    /**
     * Accessor method for obtaining the bargraph progress indicator.
     * 
     * @return
     */
    public Bargraph getBargraph()
    {
        return m_playbackIndicator;
    }

    // /////////////////////////////////////////////////////////////////////////
    // Key Handling methods
    // /////////////////////////////////////////////////////////////////////////
    public void keyPressed(KeyEvent e)
    {
        m_strKey = "keyPressed: " + e.getKeyCode();
        m_scene.repaint();
    }

    public void keyReleased(KeyEvent e)
    {
        m_strKey = "keyReleased: " + e.getKeyCode();
        logIt(m_strKey);
        m_scene.invalidate();
        switch (m_menuMode)
        {
            case MENU_MODE_GENERAL:
                switch (e.getKeyCode())
                {
                case OCRcEvent.VK_1:
                    m_menuMode = MENU_MODE_DVR;
                    updateMenuBox();
                    break;
                    
                case OCRcEvent.VK_2:
                    m_menuMode = MENU_MODE_HN;
                    updateMenuBox();
                    break;
                    
                default:
                    keyReleasedCommon(e);
                }
                break;
            case MENU_MODE_DVR:
                m_dvrTest.keyReleasedDvr(e);
                 break;
            case MENU_MODE_HN:
                m_dvrHNTest.keyReleasedHN(e);
                break;
            case MENU_MODE_MEDIA_CONTROL:
                m_dvrTest.keyReleasedMediaControl(e);
                break;
            case MENU_MODE_DVR_RECORD:
                m_dvrTest.keyReleasedRecording(e);
                break;
            case MENU_MODE_DVR_PLAYBACK:
                m_dvrTest.keyReleasedPlayback(e);
                break;
            case MENU_MODE_DVR_DELETE:
                m_dvrTest.keyReleasedDelete(e);
                break;
            default:
                System.out.println("ERROR - DVRExerciser.keyReleased() - " + "unrecognized menu mode = " + m_menuMode);
        }
    }

    protected void keyReleasedCommon(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_CHANNEL_UP:
            case KeyEvent.VK_PAGE_UP:
                // make sure we are in the live operational state and channel up
                OperationalState.setLiveOperationalState().channelUp();
                break;
            case OCRcEvent.VK_CHANNEL_DOWN:
            case KeyEvent.VK_PAGE_DOWN:
                // make sure we are in the live operational state and channel
                // down
                OperationalState.setLiveOperationalState().channelDown();
                break;
            case OCRcEvent.VK_RECORD:
                break;

            case OCRcEvent.VK_STOP:
                // stop the current operational state and go to the stopped
                // state
                OperationalState.setStoppedOperationalState();
                break;

            case OCRcEvent.VK_LIVE:
                // make sure we are in the live operational state and start
                // playing
                OperationalState.setLiveOperationalState().play();
                break;

            case OCRcEvent.VK_PLAY:
                // if we are currently in the stopped operational state...
                OperationalState currentOperationalState = OperationalState.getCurrentOperationalState();
                if (true == (currentOperationalState instanceof StoppedOperationalState))
                {
                    // ...transition to the live operational state
                    currentOperationalState = OperationalState.setLiveOperationalState();
                }

                // now start the current operational state
                currentOperationalState.play();
                break;

            case OCRcEvent.VK_FAST_FWD:
                OperationalState.getCurrentOperationalState().fastForward();
                break;

            case OCRcEvent.VK_REWIND:
                OperationalState.getCurrentOperationalState().rewind();
                break;

            case OCRcEvent.VK_PAUSE:
                OperationalState.getCurrentOperationalState().pause();
                break;

            default:
                break;
        }
    }

    public void keyTyped(KeyEvent e)
    {
        m_strKey = "keyTyped: " + e.getKeyChar();
        m_scene.invalidate();
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        logIt("DvrExerciser.startXlet()");

        if (!m_started)
        {
            m_started = true;
            try
            {
                // start up the application in a new thread
                new Thread(m_instance).start();
                EASManager.getInstance().addListener(new EASListener()
                {
                    public void warn(EASEvent event)
                    {
                        logIt("EAS WARN - event reason: " + event.getReason());
                    }

                    public void notify(EASEvent event)
                    {
                        logIt("EAS notify - event reason: " + event.getReason());
                    }
                });
            }
            catch (Throwable ex)
            {
                logIt("Exception occured in DvrExerciser.java in startXlet(): " + ex);
            }
        }
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        System.out.println("DvrExerciser.pauseXlet()");
        if (m_instance != null)
        {
            // Hide the application.
            m_instance.hideApplication();
        }
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
    {
        System.out.println("DvrExerciser.destroyXlet()");
        if (m_instance != null)
        {
            // Hide the application.
            m_instance.disposeApplication();
        }
    }

    protected void toggleMenuDisplay(boolean doDisplay)
    {
        m_doDisplayMenu = doDisplay;
        
        // If display is requested, make sure all components are visible
        if (m_doDisplayMenu)
        {
            m_messenger.setVisible(true);
            m_menuBox.setVisible(true);
        }
        else
        {
            // Make sure all components are not visible
            m_messenger.setVisible(false);
            m_menuBox.setVisible(false);
        }
        validate();
        repaint();
     }
    
    /**
     * Displays a line of text on the lower part of the screen and in the log
     * file.
     * 
     * @param string
     *            text to be displayed.
     */
    public void logIt(String string)
    {
        System.out.println("==============>" + string + "<================\n");
        if (null != m_messenger)
        {
            m_messenger.addMessage(string);
        }
    }

    /**
     * Updates the text in the menu based on the menu mode.
     */
    protected void updateMenuBox()
    {
        // Reset the menu box
        m_menuBox.reset();

        switch (m_menuMode)
        {
            case MENU_MODE_GENERAL:
                m_menuBox.write("Dvr Exerciser Options");
                if ( m_dvrExtEnabled )
                {
                    m_menuBox.write("1: Display DVR Menu");
                }
                if (m_hnExtEnabled )
                {  
                    m_menuBox.write("2: Display Home Networking Menu");
                }
                break;
                
            case MENU_MODE_DVR:
                m_dvrTest.updateMenuBoxDvr();
                break;

            case MENU_MODE_HN:
                m_dvrHNTest.updateMenuBoxHN();
                break;

            case MENU_MODE_MEDIA_CONTROL:
                m_dvrTest.updateMenuBoxDvr();
                break;

            case MENU_MODE_DVR_RECORD:
                m_dvrTest.updateMenuBoxDvr();
                break;

            case MENU_MODE_DVR_PLAYBACK:
                m_dvrTest.updateMenuBoxDvr();
                break;

            case MENU_MODE_DVR_DELETE:
                m_dvrTest.updateMenuBoxDvr();
                break;
            default:
                System.out.println("DvrExerciser.updateMenuBox() - Unsupported menu mode " + m_menuMode);
        }
    }
    
    public void doNothing(String myString, Integer myInt, Float myFloat)
    {
        // this method is used for testing the OCAPTest interface to
        // DVRExercisor
        logIt("CALLED doNothing: myString = " + myString + ", myInt = " + myInt + ", myFloat = " + myFloat);
    }

    /**
     * Used by the OcapTestInterfaceThread to simulate remote control key
     * presses.
     * 
     * @param keyCode
     */
    public void pressKey(Integer keyCode)
    {
        KeyEvent e = new KeyEvent(this, // source
                0, // int id
                (long) 0, // long when
                0, // int modifiers
                keyCode.intValue(), // int keyCode
                (char) 0 // char keyChar
        );
        keyReleased(e);
    }

    public String dvrCreateRec(Integer recLength)
    {
        m_dvrTest.recordAndWait(recLength.intValue(), true, 0);
        
        return "dvrCreateRec(" +recLength +") - end";
    }

    public String dvrCheckRec()
    {
        return m_dvrTest.getCurrentRecState();
    }

    public String hnPublishRec()
    {
        OcapRecordingRequest currentRec = m_dvrTest.getCurrentRecordingRequest();
        if (currentRec == null)
        {
            logIt("Current recording was null");
            return "fail";
        }
        boolean publishSuccess = m_dvrHNTest.publishRecordingToCDS(currentRec);
        if (publishSuccess)
        {
            return "pass";
        }
        return "fail";
    }

    public String hnGetSvrUuid()
    {
        String uuid = m_dvrHNTest.getCurrentUuid();

        uuid = uuid.substring(uuid.indexOf(":")+1);
        return uuid;
    }

    public String hnSetSvr(String uuid)
    {
        if (m_dvrHNTest.setMediaSvr("uuid:"+uuid))
        {
            return "pass";
        }
        return "fail";
    }
    public void stopServiceContextAndWaitForPTE(final ServiceContext serviceContext)
    {
       ServiceContextListenerImpl listener = new ServiceContextListenerImpl(serviceContext);
       serviceContext.addListener(listener);
       new Thread(new Runnable()
       {
           public void run()
           {
               System.out.println("stopping servicecontext: " + serviceContext);
               serviceContext.stop();
           }
           
       }).start();
       listener.waitForStop();
       serviceContext.removeListener(listener);
    } 
    /*
     * When running this application as both the player and server, user operation
     * is simplified by automatically selecting this instance as the server.
     */
    private void autoServerInit(){
        if ( m_hnTest.localContentServerNetModule != null) 
        {
            m_hnTest.m_mediaServer = m_hnTest.localContentServerNetModule;
            logIt ("This instance of DvrExerciser selected as the Media Server");
        } 
        else
        {
            logIt ("Did not select the server.  CSNM was null");
        }
        // publish all the channels
        if (m_dvrHNTest.publishAllChannelsToCDS() )
        {
            logIt("   ... all channels published");
        }
        // publish all recordings.   
        RecordingList rc = m_dvrTest.getRecordings();
        RecordingListIterator rli = rc.createRecordingListIterator();
        if (!rli.hasNext())
        {
            logIt("   ... no recordings published");
        }
        while (rli.hasNext())
        {
            RecordingRequest rr=rli.nextEntry();
            m_dvrHNTest.publishRecordingToCDS((OcapRecordingRequest) rr);
            logIt("   ... recording published");
        }
    }
    

    
    class ServiceContextListenerImpl implements ServiceContextListener
    {
       private volatile boolean shutdown;
       private final Object monitor = new Object();
       private ServiceContext serviceContext;
       
       ServiceContextListenerImpl(ServiceContext serviceContext)
       {
           this.serviceContext = serviceContext;
       }
       
       public void waitForStop()
       {
           System.out.println("waitForStop");
           synchronized (monitor)
           {
               if (shutdown)
               {
                   System.out.println("waitForStop - SC already shut down");
                   return;
               }
               
               while (!shutdown && serviceContext.getService() != null)
               {
                   try
                   {
                       monitor.wait(2000);
                       System.out.println("waitForStop - SC: " + serviceContext);
                   }
                   catch (InterruptedException e)
                   {
                       e.printStackTrace();
                   }
               }
           }
       }
       
        public void receiveServiceContextEvent(ServiceContextEvent event)
        {
            System.out.println("waitForStop serviceContextEvent - sc: " + serviceContext + ", event: " + event);
            if (event instanceof PresentationTerminatedEvent)
            {
                synchronized(monitor)
                {
                    System.out.println("found PTE - notifying");
                    shutdown = true;
                    monitor.notifyAll();
                }
            }
        }
    }
}
