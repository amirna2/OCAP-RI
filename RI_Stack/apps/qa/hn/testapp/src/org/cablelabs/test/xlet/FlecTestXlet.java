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

package org.cablelabs.test.xlet;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.graphics.AlphaColor;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.cablelabs.test.ElementList;
import org.cablelabs.test.Logger;
import org.cablelabs.test.Test;
import org.cablelabs.test.TestRunner;
import org.cablelabs.test.TestStateChangeListener;
import org.cablelabs.test.TestStatusListener;
import org.cablelabs.test.TestingState;
import org.cablelabs.test.xlet.ui.ListBox;
import org.cablelabs.test.xlet.ui.StatusBar;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.havi.ui.HScene;

public class FlecTestXlet extends java.awt.Component implements Xlet, TestStatusListener, TestStateChangeListener
{
    /**
     * Unit test variables
     */
    private Vector testLists = new Vector();

    private Test currentTest;

    private int currentTestIndex = -1;

    private ElementList currentTestList;

    private int currentTestListIndex = -1;

    private Hashtable excludeTcHashTable;

    private ListBox m_mainMenu = null;

    private StatusBar statusBar = null;

    /**
     * Stats variables
     */
    private int passCount = 0;

    private int failCount = 0;

    private int skipCount = 0;

    private int errorCount = 0;

    private Logger logger = null;

    private XletContext m_xletContext = null;

    private Container myRootContainer;

    private AppID myAppId;

    private AppAttributes myAttributes = null;

    private AppsDatabase myAppsDatabase = null;

    private Logger myLogger = null;

    private String myAppName;

    private String myAppTitle;

    protected boolean inFocus = false;

    /**
     * UI related stuff
     */
    private static final int HEADER_HEIGHT = 67;

    private static final int HEADER_Y1 = 40;

    private static final int HEADER_Y2 = 60;

    private static final int HEADER_MARGIN_LEFT = 50;

    private boolean transparency = false;

    private boolean headerDisplay = true;

    private boolean menuDisplay = true;

    private Color bgColor1 = Color.black;

    private Color bgColor2 = new AlphaColor(255, 255, 255, 0);

    private Dimension dimScreen = null;

    private Font headerFont = new Font("Tiresias", Font.BOLD, 14);

    private boolean statusDisplay = true;

    protected int currentKeyCount = 0;

    protected int currentCode = 0;

    /**
     * Configuration stuff
     */
    private int internalState;

    private TestRunner m_runner = null;

    protected static final int STATE_INTERNAL_APP_INIT = -2;

    protected static final int STATE_INTERNAL_APP_CONTROL = -3;

    protected static final int STATE_INTERNAL_MAIN_MENU = -4;

    protected static final int STATE_INTERNAL_SUB_MENU = -5;

    protected static final int STATE_INTERNAL_TEST_EXECUTION = -6;

    /**
     * Initialisation method for the Xlet.
     * 
     * @param ctx
     *            Xlet Context.
     */
    public final void initXlet(final XletContext ctx)
    {
        String testFile = null;

        logger = new Logger();
        logger.setPrefix("FlecTestXlet: ");

        this.myAppTitle = "FlecTestXlet";

        this.m_xletContext = ctx;

        String[] arguments = (String[]) ctx.getXletProperty(javax.tv.xlet.XletContext.ARGS);

        if (arguments == null)
        {
            arguments = (String[]) ctx.getXletProperty("dvb.caller.parameters");
        }

        if (arguments[0] == null)
        {
            // fail to start
            logger.log("No test file is available - can't start - bye bye.");
        }

        testFile = arguments[0];
        logger.log("Initializing TestRunner with File: " + testFile);

        try
        {
            this.m_runner = new TestRunner(testFile);

            m_runner.dumpTests(m_runner.getRootElementList());
            currentTestList = m_runner.getRootElementList();
            m_runner.addTestStateChangeListener(this);
            m_runner.addTestStatusListener(this);
            // this.mainMenu.initialize(currentTestList);
        }
        catch (FileNotFoundException e)
        {
            logger.log("Invalid File: " + testFile + "aborting....bye bye.");
            e.printStackTrace(System.err);
        }

    }

    public final void startXlet()
    {
        logger.log("FlecTestXlet.startXlet()");

        enableEvents(KeyEvent.KEY_EVENT_MASK | FocusEvent.FOCUS_EVENT_MASK);

        statusBar = new StatusBar();
        this.updateStatusBar(null);

        this.internalState = FlecTestXlet.STATE_INTERNAL_MAIN_MENU;

        this.setMenuDisplay(true);
        gainFocus();
        createMainMenu();
    }

    protected void gainFocus()
    {
        if (inFocus)
        {

            // logger.log("gain focus - in focus == true - repainting....");
            repaint();
        }
        else
        {
            // logger.log("gain focus - in focus == false - initGraphicProps....");
            initGraphicProperties();
            ((HScene) myRootContainer).show();
            requestFocus();
        }
    }

    private void initGraphicProperties()
    {
        dimScreen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if (myRootContainer == null)
        {
            myRootContainer = javax.tv.graphics.TVContainer.getRootContainer(m_xletContext);
            myRootContainer.add(this, -1);
            myRootContainer.setSize(dimScreen.width, dimScreen.height);
        }
        this.setBounds(0, 0, dimScreen.width, dimScreen.height);
        myRootContainer.setBounds(0, 0, dimScreen.width, dimScreen.height);
    }

    public void destroyXlet(final boolean unconditional)
    {
        logger.log("FlecTestXlet.destroyXlet()");

        myRootContainer.setVisible(false);
        m_xletContext.notifyDestroyed();
        if (null != currentTest)
        {
            logger.logError("Test interrupted by EXIT command");
        }
    }

    /**
     * Called when Xlet is paused.
     */
    public final void pauseXlet()
    {
        logger.log("UnitTestApp.pauseXlet()");
        disableEvents(KeyEvent.KEY_EVENT_MASK | FocusEvent.FOCUS_EVENT_MASK);
        inFocus = false;
    }

    /**
     * Increment result statistics.
     * 
     * @param result
     *            Result that indicates if it was PASS/FAIL/SKIP
     */
    private void incrementStats(final int result)
    {
        switch (result)
        {
            case Test.TEST_STATUS_PASS:
                ++passCount;
                break;
            case Test.TEST_STATUS_FAIL:
                ++failCount;
                break;
            case Test.TEST_STATUS_NONE:
                // ++skipCount;
                break;
            case Test.TEST_STATUS_INTERRUPTED:
                ++errorCount;
                break;
            default:
                break;
        }
    }

    /**
     * Get stats for UnitTest that were executed so far.
     * 
     * @param type
     *            Type of stat that we want to retrieve PASS/FAIL/SKIP
     * 
     * @return count of PASS/FAIL/SKIP tests
     * 
     */
    public int getStats(final int type)
    {
        // switch(type) {
        // case UnitTestResultEvent.RESULT_PASS:
        // return passCount;
        // case UnitTestResultEvent.RESULT_FAIL:
        // return failCount;
        // case UnitTestResultEvent.RESULT_SKIP:
        // return skipCount;
        // case UnitTestResultEvent.RESULT_ERROR:
        // return errorCount;
        // default:
        return -1;
        // }
    }

    /**
     * Implementation of UnitTestResultListener. If there is a need of
     * overriding this.
     * 
     * @param event
     *            Event to be handled.
     */

    public void notifyStatusChanged(int status, Test t)
    {
        int result = status;

        logger.log("Received test result event = " + Test.statusToString(result));
        // update stats
        incrementStats(t.getStatus());

        // check if status bar is enabled before spending a lot of time to build
        // the string
        if (this.statusDisplay)
        {
            // report results
            this.updateStatusBar("P: " + this.passCount + " F: " + this.failCount + " I: " + this.errorCount
                    + " TEST: " + t.getName() + " RESULT: " + Test.statusToString(t.getStatus()));
        }
        repaint();
    }

    public void notifyStateChanged(int newState, int oldState, Test t)
    {
    }

    /**
     * This method will paint the status bar on the screen using the
     * Toolkit.sync() method. This means that whenever it will be called it will
     * paint the menu without calling repaint().
     * 
     * @param msg
     *            is the new message to be displayed
     * 
     */
    public void updateStatusBar(String msg)
    {
        if (null == msg)
        {
            msg = "P: " + this.passCount + " F: " + this.failCount + " I: " + this.errorCount + " TEST: "
                    + this.m_runner.getCurrentTestName() + " RESULT: "
                    + Test.statusToString(this.m_runner.getCurrentTestStatus());
        }

        statusBar.setMessage(msg);
        Graphics gr = getGraphics();
        if (gr == null)
        {
            logger.log("updateStatusBar(" + msg + "): unable to update StatusBar at this time (graphics == null)");
        }
        else
        {
            statusBar.draw(getGraphics());
            Toolkit.getDefaultToolkit().sync();
        }
    }

    public void drawMainMenu(final Graphics g)
    {
        if ((null != this.m_mainMenu) && (null != g))
        {
            m_mainMenu.draw(g);
        }
    }

    /**
     * This method will paint the status bar on the screen.
     * 
     */
    public void drawStatusBar(final Graphics g)
    {
        if (g != null)
        {
            statusBar.draw(g);
        }
    }

    /**
     * Implementation of the paint() method. This will process all painting
     * after which it will invoke appPaint() method from the extending class.
     * 
     * @param g
     *            Graphics object associated with this xlet
     */
    public final void paint(final Graphics g)
    {
        logger.log("paint Enter.");
        // if (!inFocus) {
        // logger.log("App is not in foreground ");
        // return;
        // }

        // clear background
        if (transparency)
        {
            g.setColor(bgColor2);
        }
        else
        {
            g.setColor(bgColor1);
        }
        g.fillRect(0, 0, dimScreen.width, dimScreen.height);

        //
        // header and status bar were moved to UnitTestApp
        //
        drawHeader(g);

        // display status bar if wanted
        drawStatusBar(g);

        switch (this.internalState)
        {
            case FlecTestXlet.STATE_INTERNAL_APP_CONTROL:
            case FlecTestXlet.STATE_INTERNAL_MAIN_MENU:
            case FlecTestXlet.STATE_INTERNAL_SUB_MENU:
            case FlecTestXlet.STATE_INTERNAL_TEST_EXECUTION:
                // logger.log("paint() state = testExectution");
                // appPaint(g);
                drawMainMenu(g);
                break;

            default:
                break;
        }
    }

    public final boolean isMenuOn()
    {
        logger.log("isMenuOn = " + this.menuDisplay);
        return menuDisplay;
    }

    /**
     * Sets the display of the menu on or off. By default the menu will be
     * always displayed but the user can turn it off.
     * 
     * @param displayMenu
     *            true to turn on header display or false to turn it off
     * 
     */
    public final void setMenuDisplay(final boolean displayMenu)
    {
        logger.log("setMenuDisplay = " + displayMenu);
        menuDisplay = displayMenu;
    }

    /**
     * This will be called instead of the processKeyEvent() method. It will be
     * implemented in the app to process user specific key events.
     * 
     * @param event
     *            is the KeyEvent to be processed
     * 
     */
    public void processKey(final KeyEvent event)
    {
    }

    // /////////////////////////////////////////////////////////////////////////
    // AWT Focus Handling method
    // /////////////////////////////////////////////////////////////////////////
    protected void processFocusEvent(final FocusEvent e)
    {
        int id = e.getID();
        switch (id)
        {
            case FocusEvent.FOCUS_LOST:
                logger.log(" FocusEvent.FOCUS_LOST");
                inFocus = false;
                break;
            case FocusEvent.FOCUS_GAINED:
                logger.log(" FocusEvent.FOCUS_GAINED");
                inFocus = true;
                repaint();
                break;
            default:
                break;
        }
    }

    /**
     * Method needed to be implemented to handle key events. By default it will
     * display last key in the header. The user can overwrite this method but if
     * the user still wants to display last key in the header then it should
     * first call super before adding its own implementation.
     * 
     * @param e
     *            current KeyEvent to be processed
     */
    protected final void processKeyEvent(final KeyEvent e)
    {
        logger.log("processKeyEvent Enter id = " + e.getID());
        if (e.getID() == KeyEvent.KEY_PRESSED)
        {
            currentCode = e.getKeyCode();
            currentKeyCount++;
            drawKey(e);

            switch (currentCode)
            {
                case KeyEvent.VK_1:
                case KeyEvent.VK_2:
                case KeyEvent.VK_3:
                case KeyEvent.VK_4:
                case KeyEvent.VK_5:
                case KeyEvent.VK_6:
                case KeyEvent.VK_7:
                case KeyEvent.VK_8:
                case KeyEvent.VK_9:
                {
                    int index = this.m_mainMenu.getSelectedIndex(Integer.parseInt("" + e.getKeyChar()));
                    this.updateStatusBar(null);
                    index = (currentCode - KeyEvent.VK_0);
                    logger.log("processKeyEvent() index = " + index);
                    this.m_mainMenu.handleOption(index);
                    repaint();
                    break;
                }
                case KeyEvent.VK_PAGE_DOWN:
                {
                    m_mainMenu.pageDown();
                    repaint();
                    break;
                }
                case KeyEvent.VK_PAGE_UP:
                {
                    m_mainMenu.pageUp();
                    repaint();
                    break;
                }
                case KeyEvent.VK_UP:
                {
                    m_mainMenu.scrollUp();
                    repaint();
                    break;
                }
                case KeyEvent.VK_DOWN:
                {
                    m_mainMenu.scrollDown();
                    repaint();
                    break;
                }
                default:
                {
                    logger.log("Invalid option");
                    break;
                }
            }
            // }
            // do app specific key processing
            processKey(e);
        }
    }

    public final void drawHeader(final Graphics g)
    {
        if (!headerDisplay)
        {
            return;
        }

        String runMode = "Manual";
        // String launcher = (isLaunchedByHarness == true) ? "TSH" :
        // "Standalone";
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();

        // set background
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, dimScreen.width, HEADER_HEIGHT);
        // g.fillRect( headerArea.x, headerArea.y, headerArea.width,
        // headerArea.height );

        // Title
        g.setColor(Color.white);
        g.setFont(headerFont);
        g.drawString(myAppTitle, HEADER_MARGIN_LEFT, HEADER_Y1);
        // g.drawString( myAppTitle, HEADER_MARGIN_LEFT, hdrRowHeight +
        // HEADER_MARGIN_TOP_LR_HR );

        // Runtime Details
        // g.drawString("RunMode: " + runMode, HEADER_MARGIN_LEFT, HEADER_Y2);
        ElementList list = this.m_mainMenu.getListInFocus();

        String mark = (list.isRoot()) ? "" : ">>";

        String location = mark + list.getName();

        while (list.isRoot() == false)
        {
            mark = (((ElementList) list.getParent()).isRoot()) ? "" : ">>";
            location = mark + list.getParent().getName() + location;
            list = (ElementList) list.getParent();
        }

        this.logger.log("printing location..." + location);

        g.drawString(location, HEADER_MARGIN_LEFT, HEADER_Y2);
        // g.drawString( IPAddress + ", " + runMode, HEADER_MARGIN_LEFT,
        // (hdrRowHeight*2) + HEADER_MARGIN_TOP_LR_HR );

        // Draw last key
        if (currentKeyCount != 0)
        {
            g.drawString("Key Count: " + currentKeyCount + " Code: " + currentCode, dimScreen.width - 195, HEADER_Y2);
        }
        // g.drawString( "Key Count: " + currentKeyCount + " Code: " +
        // currentCode, getDimension().width - 195, (hdrRowHeight*2) +
        // HEADER_MARGIN_TOP_LR_HR );

        // Restore the font and color
        if (oldFont != null && oldColor != null)
        {
            g.setFont(oldFont);
            g.setColor(oldColor);
        }
    }

    private void drawKey(final KeyEvent e)
    {
        logger.log("drawKey( " + e.getKeyCode() + " )");
        drawHeader(getGraphics());
        Toolkit.getDefaultToolkit().sync();
    }

    protected void createMainMenu()
    {
        m_mainMenu = new ListBox(0, HEADER_HEIGHT, this.dimScreen.width, this.dimScreen.height - HEADER_HEIGHT
                - StatusBar.STATUS_BAR_HEIGHT);
        m_mainMenu.initialize(this.currentTestList, this.m_runner, getGraphics());
    }
}
