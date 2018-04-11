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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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
 * @author Alan Cohn
 */
package org.cablelabs.xlet.eashostkeylistener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

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
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.SystemEventManager;
import org.ocap.ui.event.OCRcEvent;

/*
 * This is an example of an application that can be run when an Emergency Alert System (EAS) 
 * message is received by the EAS that has an 'indefinite' duration. 
 * The application displays a message that allows the user to continue viewing the
 * EAS alert or terminate viewing the alert. 
 * 
 * The EAS manager knows about this application by it's
 * Applicatiion ID (AppID) which is set in the mpeenv.ini file.
 * The value of the “OCAP.eas.presentation.interrupt.appId” MPE environment variable is used to 
 * locate and start the application via an AppProxy instance obtained from the AppsDatabase. 
 * If the variable is not defined, or is not defined as a hexadecimal string prefixed with “0x”, 
 * or the application ID is not mapped to an available application, the application is not started. 
 * The application must have been signaled with a PRESENT control code to be successfully started.
 * The value of the “OCAP.eas.presentation.minimum.time” MPE environment variable is passed 
 * in as the first string argument to the application. 
 * The value specifies the number of seconds that key presses should be ignored by the 
 * application to ensure a minimal amount of time the alert is presented. 
 * If the variable is not defined or is not defined as a positive decimal integer, 
 * then a default value of “0” is used indicating that the user may immediately dismiss the alert.
 * 
 */
public class EASHostKeyListenerXlet extends HContainer implements ResourceClient, KeyListener, ResourceStatusListener,
        Xlet
{
    /**
     * Constants
     */
    static final long serialVersionUID = 1;

    static final int NUM_ROWS = 4;

    static final int NUM_COLUMNS = 1;

    static final int FONT_SIZE = 16;

    static final int USERPROMPT_WIDTH = 560;

    static final int USERPROMPT_HEIGHT = 200;

    static final String FONT_NAME = "SansSerif";

    static final String ENGLISH = "eng"; // ISO 639-2 code for English

    static final String SPANISH = "spa"; // ISO 639-2 code for Spanish

    static final String[] DEFAULT_USER_LANGUAGE = new String[] { ENGLISH };

    static final String ENG_USERPROMPT_STR1 = "An indefinite Emergency Alert is in progress.";

    static final String ENG_USERPROMPT_STR2 = "Do you want to continue viewing the Emergency Alert?";

    static final String ENG_USERPROMPT_EXIT = "Press Exit to stop viewing Emergency Alert";

    static final String ENG_USERPROMPT_LAST = "Press Last to continue viewing Emergency Alert";

    // Spanish
    static final String SPA_USERPROMPT_STR1 = "Indefinido de Alerta de Emergencia se encuentra en progreso.";

    static final String SPA_USERPROMPT_STR2 = "¿Desea continuar viendo el de Alerta de Emergencia?";

    static final String SPA_USERPROMPT_EXIT = "Pulse Salir para dejar de ver de Alerta de Emergencia.";

    static final String SPA_USERPROMPT_LAST = "Pulse el botón para Última seguir de Alerta de Emergencia.";

    static final String[] SPA_MESSAGE = { SPA_USERPROMPT_STR1, SPA_USERPROMPT_STR2, SPA_USERPROMPT_EXIT,
            SPA_USERPROMPT_LAST };

    static final String[] ENG_MESSAGE = { ENG_USERPROMPT_STR1, ENG_USERPROMPT_STR2, ENG_USERPROMPT_EXIT,
            ENG_USERPROMPT_LAST };

    /*
     * Class member variables
     */
    int m_wait_sec = 0; // Seconds to wait before accept key presses

    long m_startTime = 0; // Future start time to accept key strokes

    boolean m_startFlag = false; // Use a flag if started display

    boolean m_started = false; // Xlet has started.

    boolean m_eventRsvd = false; // I own all key

    String[] m_preferredLanguages = DEFAULT_USER_LANGUAGE;

    XletContext m_ctx = null; // Xlet context.

    HScene m_scene = null; // Draw area

    OverallRepository m_allKeys = null;

    UserPreferenceManager m_preferenceManager = null;

    EventManager m_eventManager = null;

    String[] m_message = ENG_MESSAGE; // Default to English language

    SystemEventManager m_sysEventManager = SystemEventManager.getInstance();

    /**
     * Default constructor.
     */
    public EASHostKeyListenerXlet()
    {
        // No-Op
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.microedition.xlet.Xlet#initXlet(javax.microedition.xlet.XletContext
     * )
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "EASHostKeyListenerXlet in initXlet"));

        m_ctx = ctx; // save my context

        m_preferenceManager = UserPreferenceManager.getInstance();
        m_eventManager = EventManager.getInstance();

        // get the wait time in seconds from my context
        String[] args = (String[]) m_ctx.getXletProperty("dvb.caller.parameters");
        if (null != args && args.length > 0)
        {
            m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                    "EASHostKeyListenerXlet dvb args.length is " + args.length));
            m_wait_sec = Integer.parseInt(args[0]); // Determine wait time in
                                                    // seconds before allowing
                                                    // input
        }

        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                "EASHostKeyListenerXlet delay time seconds is " + m_wait_sec));

        m_startTime = System.currentTimeMillis() + (m_wait_sec * 1000);
        m_startFlag = false;

        // Determine if language is English or Spanish
        updatePreferredLanguages();
        if (m_preferredLanguages[0].equals(SPANISH))
        {
            m_message = SPA_MESSAGE;
        }

        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                "EASHostKeyListenerXlet preferred language is " + m_preferredLanguages[0]));

        initGUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.microedition.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "EASHostKeyListenerXlet in startXlet"));

        try
        {
            if (!m_started)
            {
                m_started = true;

                m_allKeys = new OverallRepository("allKeys");

                m_eventManager.addResourceStatusEventListener(this);

                m_eventRsvd = m_eventManager.addExclusiveAccessToAWTEvent(this, m_allKeys);
                m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                        "EASHostKeyListenerXlet addExclusiveAccessToAWTEvent is " + m_eventRsvd));

                m_scene.addKeyListener(this);

                m_scene.setVisible(true);
                m_scene.requestFocus();
            }
        }
        catch (Exception e)
        {
            m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_CAT_GENERAL_ERROR, e));
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "EASHostKeyListenerXlet in pauseXlet"));

        if (m_started)
        {
            if (isVisible())
            {
                setVisible(false); // container
            }

            m_scene.removeKeyListener(this);
            m_eventManager.removeResourceStatusEventListener(this);

            if (m_eventRsvd)
            {
                m_eventManager.removeExclusiveAccessToAWTEvent(this);
            }

            m_started = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.microedition.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT, "EASHostKeyListenerXlet in destroyXlet"));

        try
        {
            if (m_started)
            {
                pauseXlet();
                m_scene.removeAll();
                m_scene.dispose();
                m_started = false;
            }
        }
        catch (Exception e)
        {
            m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_CAT_GENERAL_ERROR, e));
            throw new XletStateChangeException(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#notifyRelease(org.davic.resources.
     * ResourceProxy)
     */
    public void notifyRelease(ResourceProxy proxy)
    {
        m_eventRsvd = false; // Lost resource
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#release(org.davic.resources.ResourceProxy
     * )
     */
    public void release(ResourceProxy proxy)
    {
        m_eventRsvd = false; // Lost resource
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceClient#requestRelease(org.davic.resources
     * .ResourceProxy, java.lang.Object)
     */
    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        return false; // Won't release
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent ke)
    {
        int key = ke.getKeyCode();

        m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                "EASHostKeyListenerXlet in keyReleased key = " + key));

        if (!isVisible()) // Container visible?
        {
            if (m_startFlag || System.currentTimeMillis() >= m_startTime)// can
                                                                         // we
                                                                         // appear
                                                                         // yet?
            {
                m_startFlag = true;
                m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                        "EASHostKeyListenerXlet make visible"));
                setVisible(true); // Container
                m_scene.setVisible(true);
                m_scene.setActive(true);
                m_scene.requestFocus();
                m_scene.repaint();
            }
        }
        else if (key == OCRcEvent.VK_EXIT) // exit key
        {
            m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                    "EASHostKeyListenerXlet Exit key - Exit EAS Viewing"));
            setVisible(false); // container
            m_scene.repaint();

            try
            {
                destroyXlet(false);
                m_ctx.notifyPaused(); // tell EAS Manager I'm pausing.
            }
            catch (Exception ex)
            {
                m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_CAT_GENERAL_ERROR, ex));
            }
        }
        else if (key == OCRcEvent.VK_LAST) // Last key
        {
            m_sysEventManager.log(new ErrorEvent(ErrorEvent.APP_INFO_GENERAL_EVENT,
                    "EASHostKeyListenerXlet Last key - Only Exit Screen"));
            setVisible(false); // container
            m_scene.repaint();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent arg0)
    {
        // No-Op
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceStatusListener#statusChanged(org.davic.resources
     * .ResourceStatusEvent)
     */
    public void statusChanged(ResourceStatusEvent event)
    {
        // Skip if already reserved or not the right kind of event
        if (m_eventRsvd || !(event instanceof UserEventAvailableEvent)) return;

        // Try and acquire the keys reservation
        m_eventRsvd = m_eventManager.addExclusiveAccessToAWTEvent(this, m_allKeys);
    }

    /**
     * Initialize the application graphical user interface.
     */
    private void initGUI()
    {
        // Create full screen HScene
        HGraphicsDevice device = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();
        m_scene = HSceneFactory.getInstance().getFullScreenScene(device);

        // Initialize the Container layout.
        setLayout(new GridLayout(NUM_ROWS, NUM_COLUMNS));
        Dimension size = m_scene.getSize();
        int x = (size.width - USERPROMPT_WIDTH) / 2;
        int y = (size.height - USERPROMPT_HEIGHT) / 2;
        setBounds(x, y, USERPROMPT_WIDTH, USERPROMPT_HEIGHT);

        // Initialize the display characteristics of the container
        setForeground(Color.white);
        setBackground(Color.blue);
        setFont(new Font(FONT_NAME, Font.BOLD, FONT_SIZE));

        // Add text
        StaticText line1 = new StaticText(m_message[0], "Line1");
        add(line1);

        StaticText line2 = new StaticText(m_message[1], "Line2");
        add(line2);

        StaticText line3 = new StaticText(m_message[2], "Line3");
        add(line3);

        StaticText line4 = new StaticText(m_message[3], "Line4");
        add(line4);

        setVisible(false); // container
        m_scene.add(this); // Add the container to the scene.

        validate();
    }

    /**
     * Paint all.
     * 
     * @param g
     *            The <code>Graphics</code> context.
     */
    public void paint(Graphics g)
    {
        Dimension size = getSize();
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);

        super.paint(g);
    }

    class StaticText extends HStaticText
    {
        private static final long serialVersionUID = 0xadcL;

        public StaticText(String str, String name)
        {
            super(str);
            setBackgroundMode(BACKGROUND_FILL);
            setName(name);
            setFont(new Font("SansSerif", 0, FONT_SIZE));
        }
    }

    /**
     * Updates the user-preferred language settings from the User Preference
     * Manager. Defaults to ISO 639-2 English ("eng") if no value sets are
     * defined for user language preference.
     */
    void updatePreferredLanguages()
    {
        GeneralPreference preference = new GeneralPreference("User Language");
        m_preferenceManager.read(preference);
        String[] languageCodes = preference.getFavourites();
        m_preferredLanguages = (null == languageCodes || languageCodes.length == 0) ? DEFAULT_USER_LANGUAGE
                : languageCodes;
    }
}
