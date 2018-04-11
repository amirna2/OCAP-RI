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

package org.cablelabs.xlet.FrontPanelExtTest;

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

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

import org.havi.ui.event.HRcEvent;

import org.ocap.ui.event.OCRcEvent;
import org.dvb.application.*;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.system.MonitorAppPermission;
import java.security.PermissionCollection;
import java.security.Permissions;

import org.ocap.hardware.frontpanel.*;

import java.lang.SecurityException;
import java.lang.IllegalArgumentException;
import java.io.IOException;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;

import org.cablelabs.test.autoxlet.*;

public class FrontPanelExtXlet extends Container implements Xlet, KeyListener, ResourceClient, Driveable
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    private String m_appName = "FrontPanelExtXlet";

    private int m_testNumber = 0;

    private Vector m_testList = new Vector();

    private final String POWER = IndicatorDisplay.POWER;

    private final String RFBYPASS = IndicatorDisplay.RFBYPASS;

    private final String MESSAGE = IndicatorDisplay.MESSAGE;

    private final String RECORD = IndicatorDisplay.RECORD;

    private final String TEXT = "Text Display";

    private final String BLINK = "Blink Spec";

    private final String BRIGHT = "Bright Spec";

    private final String COLOR = "Color Spec";

    private final String SCROLL = "Scroll Spec";

    private int currentColorIdx = 0;

    private final byte[] colors = {0x0, ColorSpec.GREEN, ColorSpec.RED, ColorSpec.YELLOW, 0x12};

    private final byte BRIGHTOFF = BrightSpec.OFF; // 0x00

    private int currentTextModeIdx = 0;

    private int currentClockModeIdx = 0;

    private final byte[] clockModes = {TextDisplay.TWELVE_HOUR_CLOCK, 
                                       TextDisplay.TWENTYFOUR_HOUR_CLOCK, 
                                       TextDisplay.STRING_MODE}; 

    private FrontPanelManager m_fpMgr = null;

    private TextDisplay m_textDisplay = null;

    private boolean textWrap = false;

    private int currentTextStrIdx = 0;

    private final String[] text1 = { "Test 1" };

    private final String[] text2 = { "Test A", "Test B", "Test C" };

    private final String[] text3 = { "1", "2", "3", "4", "5", "6", "7" };

    private final String[] text4 = { "Single line that is very very very long" };

    private final String[][] textStrings = { text1, text2, text3, text4 };

    private int currentBlinkDurationIdx = 5;

    private int currentScrollDurationIdx = 1;

    private final int[] durations = { -1, 0, 5, 25, 26, 70, 100, 105 };

    private int currentBlinkRateIdx = 1;

    private int currentBrightnessIdx = 1;

    private int currentScrollHRateIdx = 1;

    private int currentScrollVRateIdx = 1;

    private Vector m_supportedInds = new Vector();

    private Hashtable m_reservedIndSpecs = new Hashtable();
    private Vector m_reservedInds = new Vector();
    private boolean m_textReserved = false;

    private IndicatorDisplay m_indicatorDisp = null;

    private static String SECTION_DIVIDER = "==================================";

    protected int guiState = 0;

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    private static Monitor m_eventMonitor = null;

    /**
     * Initializes the OCAP Xlet.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");

        // initialize AutoXlet
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }
        m_eventMonitor = new Monitor(); // used by event dispatcher

        // store off our xlet context
        m_ctx = ctx;

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 50, 530, 370, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        debugLog(" initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     */
    public void startXlet() throws XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        // get an instance of the FrontPanelManager
        try
        {
            m_fpMgr = FrontPanelManager.getInstance();
        }
        catch (SecurityException se)
        {
            debugLog(" Error: Unable to get an instance of FrontPanelManager, caught SecurityException: " + se);
            m_test.fail("Test Failed: Unable to get an instance of FrontPanelManager, caught SecurityException: " + se);
            throw new XletStateChangeException(m_appName + " could not get an instance of FrontPanelManager");
        }

        printTestGroup();

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();

        debugLog(" startXlet() - end");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        System.out.println("[" + m_appName + "] : pauseXlet() - begin");
        m_scene.setVisible(false);
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

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private Hashtable getBlinkSpecAttributes(String indicatorName, BlinkSpec spec)
    {
        Hashtable attributes = new Hashtable();

        attributes.put(BLINK + "'s on duration", new Integer(spec.getOnDuration()));
        attributes.put(BLINK + "'s rate", new Integer(spec.getIterations()));
        attributes.put(BLINK + "'s max rate", new Integer(spec.getMaxCycleRate()));

        return attributes;
    }

    private Hashtable getBrightSpecAttributes(String indicatorName, BrightSpec spec)
    {
        Hashtable attributes = new Hashtable();

        attributes.put(BRIGHT + " brightness", new Integer(spec.getBrightness()));
        attributes.put(BRIGHT + " levels", new Integer(spec.getBrightnessLevels()));
        return attributes;
    }

    private Hashtable getColorSpecAttributes(ColorSpec spec)
    {
        Hashtable attributes = new Hashtable();

        attributes.put(COLOR + " color", new Byte(spec.getColor()));
        attributes.put(COLOR + " colors supported", new Byte(spec.getSupportedColors()));

        return attributes;
    }

    private boolean validColor(String indicatorName, byte color)
    {
        Hashtable specs = (Hashtable)m_reservedIndSpecs.get(indicatorName);

        ColorSpec colorSpec = (ColorSpec)specs.get(COLOR);

        byte supportedColor = colorSpec.getSupportedColors();
        debugLog("supported Color for " +indicatorName +" is "+supportedColor +";  color = " +color); 
        if ((supportedColor & color) == 0 || (color & color - 1) != 0)
        {
            debugLog("color "+color +" is not a supported color");
            return false;
        }
        debugLog("color "+color +" is a supported color");
        return true;
    }

    private Hashtable getScrollSpecAttributes(ScrollSpec spec)
    {
        Hashtable attributes = new Hashtable();

        attributes.put(SCROLL + "'s max horizontal rate", new Integer(spec.getMaxHorizontalIterations()));
        attributes.put(SCROLL + "'s horizontal rate", new Integer(spec.getHorizontalIterations()));
        attributes.put(SCROLL + "'s max vertical rate", new Integer(spec.getMaxVerticalIterations()));
        attributes.put(SCROLL + "'s vertical rate", new Integer(spec.getVerticalIterations()));
        attributes.put(SCROLL + "'s hold duration", new Integer(spec.getHoldDuration()));

        return attributes;
    }

    private void printAttributes(String indicatorName, Hashtable specAttributes)
    {
        print(SECTION_DIVIDER);
        print(indicatorName + ":");

        Enumeration attributeKeys = specAttributes.keys();
        String attributeName = null;
        while (attributeKeys.hasMoreElements())
        {
            attributeName = (String) attributeKeys.nextElement();
            if (attributeName.startsWith(TEXT)) // text display attributes
            {
                String attributeObj = (String) specAttributes.get(attributeName);
                print("\t" + attributeName + " = " + attributeObj);
            }
            else
            // spec attributes
            {
                Number attributeObj = (Number) specAttributes.get(attributeName);
                if (attributeName.startsWith(COLOR))
                {
                    print("\t" + attributeName + " = " + attributeObj.byteValue());
                }
                else
                {
                    print("\t" + attributeName + " = " + attributeObj.intValue());
                }
            }
        }
    }

    private void printSpecs(String indicatorName, Hashtable specs)
    {
        BlinkSpec blink = (BlinkSpec) specs.get(BLINK);
        if (blink != null)
        {
            printAttributes(indicatorName, getBlinkSpecAttributes(indicatorName, blink));
        }

        BrightSpec bright = (BrightSpec) specs.get(BRIGHT);
        if (bright != null)
        {
            printAttributes(indicatorName, getBrightSpecAttributes(indicatorName, bright));
        }

        ColorSpec color = (ColorSpec) specs.get(COLOR);
        if (color != null)
        {
            printAttributes(indicatorName, getColorSpecAttributes(color));
        }

        ScrollSpec scroll = (ScrollSpec) specs.get(SCROLL);
        if (scroll != null)
        {
            printAttributes(indicatorName, getScrollSpecAttributes(scroll));
        }
    }

    private void reserveAllSupportedIndicators()
    {
        String indName = null;
        for (int i = 0; i < m_supportedInds.size(); i++)
        {
            indName = (String) m_supportedInds.get(i);
            if (indName.equals("text"))
            {
                toggleTextDispReservation(true);
            }
            else
            { 
                // POWER, RFBYPASS, MESSAGE, RECORD
                toggleIndicatorReservation(indName, true);
            }
        }
        printReservedIndicators();
    }

    // gets either the TextDisplay and/or the indicatorDisplay
    // parameter indType indicates whether getTextDisplay and/or
    // getIndicatorDisplay should be called.
    private void retrieveIndDisplays(String indType)
    {
        print(SECTION_DIVIDER);
        debugLog(" retrieveIndDisplays() - begin");

        if (indType.equals("ind") || indType.equals("all"))
        {
            String[] reservedIndsStr = (String[]) m_reservedInds.toArray(new String[m_reservedInds.size()]);

            try
            {
                m_indicatorDisp = m_fpMgr.getIndicatorDisplay(reservedIndsStr);
            }
            catch (IllegalArgumentException iae)
            {
                print("Test Failed: failure to get Indicator Display, caught IllegalArgumentException: " + iae);
                m_test.fail("Test Failed: failure to get Indicator Display, caught IllegalArgumentException: " + iae);
            }
            if (m_indicatorDisp != null)
            {
                print("\t successfully retrieved the Indicator Display for each resreved indicator");
            }
            else
            {
                print("Fail Error: unable to get the Indicator Display for each resreved indicator, IndicatorDisplay is null");
                m_test.fail("Fail Error: unable to get the Indicator Display for each resreved indicator, IndicatorDisplay is null");
            }
        }
        if (indType.equals("text") || indType.equals("all"))
        {
            m_textDisplay = m_fpMgr.getTextDisplay();
            if (m_textDisplay == null)
            {
                print("failure to get Text Display, is it reserved? " + m_textReserved);
                m_test.assertTrue("Test Failed: failure to get TextDispaly even though it is reserved", m_textReserved);
            }
            else
            {
                print("Successfully retrieved the Text Display");
            }
        }

        debugLog(" retrieveIndDisplays() - end");
    }

    private void getIndicatorSpecs(String indicatorName)
    {
        debugLog("getting spec info for " +indicatorName);

        if (m_indicatorDisp == null)
        {
            print("getIndicatorSpecs() for " +indicatorName +" failure: m_indicatorDisp is null so no indicators to report");
            return;
        }

        Hashtable indicatorsHash = m_indicatorDisp.getIndicators();
        Indicator indObj = (Indicator)indicatorsHash.get(indicatorName);
        Hashtable specs = null;
        if (indObj != null)
        {
            specs = new Hashtable();
            specs.put(BLINK, (BlinkSpec) indObj.getBlinkSpec());
            specs.put(BRIGHT, (BrightSpec) indObj.getBrightSpec());
            specs.put(COLOR, (ColorSpec) indObj.getColorSpec());

            printSpecs(indicatorName, specs);

            m_reservedIndSpecs.put(indicatorName, specs);
        }
    }

    private void getAllIndicatorSpecs()
    {
        print(SECTION_DIVIDER);
        if (m_indicatorDisp == null)
        {
            print("getAllIndicatorsSpecs() - IndicatorDisplay is null so no indicators to report");
            return;
        }

        Hashtable indicatorsHash = m_indicatorDisp.getIndicators();
        if (indicatorsHash.size() != m_reservedInds.size())
        {
            print("Fail Error: IndicatorDisplay.getIndicators() did not return the expected number of following resreved indicators: " +m_reservedInds.size() +"; instead it returned " +indicatorsHash.size()); 
            m_test.fail("Fail Error: IndicatorDisplay.getIndicators() did not return the expected number of following resreved indicators: " +m_reservedInds.size() +"; instead it returned " +indicatorsHash.size()); 
            
        }
  
        String indicatorName = null;
        Hashtable specs = new Hashtable();
        for (int i = 0 ; i < m_reservedInds.size(); i++)
        {
            indicatorName = (String)m_reservedInds.get(i);
            getIndicatorSpecs(indicatorName);
        }

/*
        Hashtable indicatorsHash = m_indicatorDisp.getIndicators();
        Enumeration keys = indicatorsHash.keys();
        String indicatorName = null;
        Indicator indicatorObj = null;
        while (keys.hasMoreElements())
        {
            indicatorName = (String) keys.nextElement();
            indicatorObj = (Indicator) indicatorsHash.get(indicatorName);

            // verify indicatorsHash indicates the same list of reserved
            // indicators as tracked by m_reservedInds (tmpResvInd)
            if (tmpResvInd.contains(indicatorName))
            {
                tmpResvInd.removeElement(indicatorName);
            }
            else
            {
                print("Fail Error: "
                        + indicatorName
                        + " was returned by IndicatorDisplay.getIndicators() as a reserved indicator although it is not reserved");
                m_test.fail("Test Failed: "
                        + indicatorName
                        + " was returned by IndicatorDisplay.getIndicators() as a reserved indicator although it is not reserved");
            }

            Hashtable specs = new Hashtable();
            specs.put(BLINK, (BlinkSpec) indicatorObj.getBlinkSpec());
            specs.put(BRIGHT, (BrightSpec) indicatorObj.getBrightSpec());
            specs.put(COLOR, (ColorSpec) indicatorObj.getColorSpec());

            m_reservedIndSpecs.put(indicatorName, specs);
            printSpecs(indicatorName, specs);

        }

        // verify indicatorsHash indicates the same list of reserved
        // indicators as tracked by m_reservedInds (tmpResvInd)
        if (!tmpResvInd.isEmpty())
        {
            String inds = "";
            for (int i = 0; i < tmpResvInd.size(); i++)
            {
                inds = inds + tmpResvInd.get(i) + ", ";
            }

        }
*/
    }

    private Hashtable getTextAttributes(TextDisplay textDisp)
    {
        Hashtable attributes = new Hashtable();

        attributes.put(TEXT + "'s number of columns", new Integer(textDisp.getNumberColumns()).toString());
        attributes.put(TEXT + "'s number of rows", new Integer(textDisp.getNumberRows()).toString());
        attributes.put(TEXT + "'s supported characters", new String(textDisp.getCharacterSet()));

        int mode = textDisp.getMode();
        String dispMode = "String (" + mode + ")";
        if (mode == TextDisplay.TWENTYFOUR_HOUR_CLOCK) 
        { 
            dispMode = "24 Hour Clock (" + mode + ")";
        }
        if (mode == TextDisplay.TWELVE_HOUR_CLOCK) 
        { 
            dispMode = "12 Hour Clock (" + mode + ")";
        }
        attributes.put(TEXT + "'s mode", dispMode);

        return attributes;
    }

    private void printTextDisplay()
    {
        print(SECTION_DIVIDER);
        if (null == m_textDisplay)
        {
            print("printTextDisplay() - text display is null");
            return;
        }

        printAttributes(TEXT, getTextAttributes(m_textDisplay));

        Hashtable specs = new Hashtable();
        specs.put(BLINK, m_textDisplay.getBlinkSpec());
        specs.put(BRIGHT, m_textDisplay.getBrightSpec());
        specs.put(COLOR, m_textDisplay.getColorSpec());
        specs.put(SCROLL, m_textDisplay.getScrollSpec());

        m_reservedIndSpecs.put(TEXT, specs);
        printSpecs(TEXT, specs);
    }

    private void eraseText()
    {
        debugLog(" eraseText() - begin");

        retrieveIndDisplays("text");

        m_textDisplay.eraseDisplay();

        debugLog(" eraseText() - end");
    }

    private BlinkSpec changeBlinkSpec(String indicatorName, BlinkSpec blinkSpec, String attributeToReset)
    {
        debugLog(" changeBlinkSpec(" + indicatorName + ") - begin");

        Hashtable specAttributes = getBlinkSpecAttributes(indicatorName, blinkSpec);

        int maxRate = ((Integer) specAttributes.get(BLINK + "'s max rate")).intValue();
        debugLog(" changeBlinkSpec() - maxCycleRate=" + maxRate);

        int[] rates = { -1, 0, maxRate / 3, maxRate * 2 / 3, maxRate, maxRate + 1 };
        int expRate = ((Integer) specAttributes.get(BLINK + "'s rate")).intValue();
        currentBlinkRateIdx++;
        currentBlinkRateIdx = currentBlinkRateIdx >= rates.length ? 0 : currentBlinkRateIdx;
        expRate = rates[currentBlinkRateIdx];

        int expDuration = ((Integer) specAttributes.get(BLINK + "'s on duration")).intValue();
        currentBlinkDurationIdx++;
        currentBlinkDurationIdx = currentBlinkDurationIdx >= durations.length ? 0 : currentBlinkDurationIdx;
        expDuration = durations[currentBlinkDurationIdx];

        if (attributeToReset.equals(BLINK + "-RATE"))
        {
            try
            {
                blinkSpec.setOnDuration(50);
                m_vbox.write(" changeBlinkSpec(" + indicatorName + ") - setting rate to be " + expRate);
                debugLog(" changeBlinkSpec(" + indicatorName + ") - setting rate to be " + expRate);
                blinkSpec.setIterations(expRate);

                m_test.assertTrue(
                        " changeBlinkSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setIteration() with parameter "
                                + expRate, expRate >= 0 && expRate <= maxRate);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" changeBlinkSpec() - caught IllegalArgumentException when attempting to call setIteration() with parameter "
                        + expRate + "; exception: " + iae);
                m_test.assertTrue(
                        " changeBlinkSpec() - caught IllegalArgumentException when attempting to call setIteration() with parameter "
                                + expRate + "; exception: " + iae, expRate > maxRate || expRate < 0);
            }
        }

        if (attributeToReset.equals(BLINK + "-DURATION"))
        {
            try
            {
                blinkSpec.setIterations(30);
                m_vbox.write(" changeBlinkSpec(" + indicatorName + ") - setting duration to be " + expDuration);
                debugLog(" changeBlinkSpec(" + indicatorName + ") - setting duration to be " + expDuration);
                blinkSpec.setOnDuration(expDuration);

                m_test.assertTrue(
                        " changeBlinkSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setOnDuration() with parameter "
                                + expDuration, expDuration >= 0 && expDuration <= 100);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" changeBlinkSpec() - caught IllegalArgumentException when attempting to call setOnDuration() with parameter "
                        + expDuration + "; exception: " + iae);
                m_test.assertTrue(
                        " changeBlinkSpec() - caught IllegalArgumentException when attempting to call setOnDuration() with parameter "
                                + expDuration + "; exception: " + iae, expDuration > 100 || expDuration < 0);
            }
        }

        printAttributes(indicatorName, getBlinkSpecAttributes(indicatorName, blinkSpec));

        debugLog(" changeBlinkSpec(" + indicatorName + ") - end");

        return blinkSpec;
    }

    private BrightSpec changeBrightSpec(String indicatorName, BrightSpec brightSpec)
    {
        debugLog(" changeBrightSpec(" + indicatorName + ") - begin");

        // maxBrightness == (number of brightness levels - 1) per
        // OC-SP-OCAP-FPEXT-I02-0712220
        Hashtable specAttributes = getBrightSpecAttributes(indicatorName, brightSpec);
        int maxBrightness = ((Integer) specAttributes.get(BRIGHT + " levels")).intValue() - 1;
        debugLog(" changeBrightSpec(" + indicatorName + ") - max brightness is " + maxBrightness);

        int expBrightness = ((Integer) specAttributes.get(BRIGHT + " brightness")).intValue();
        debugLog(" changeBrightSpec(" + indicatorName + ") - current brightness level is " + expBrightness);

        int[] levels = { -1, // invalid - brightness level must be a positive
                             // integer
                BRIGHTOFF, // valid - brightness level of 0 (OFF) is always
                           // valid
                1, // valid - minimum brightness level for any platform is
                   // always valid (may not be visible)
                maxBrightness / 2, // valid - 50% brightness level (could be
                                   // same as 0 or BRIGHTOFF)
                maxBrightness, // valid - 100% brightness level (could be same
                               // as 1)
                maxBrightness + 1 // invalid - brightness level must be less
                                  // than the number of brightness levels
        };

        currentBrightnessIdx++;
        currentBrightnessIdx = currentBrightnessIdx >= levels.length ? 0 : currentBrightnessIdx;
        expBrightness = levels[currentBrightnessIdx];
        try
        {
            m_vbox.write(" changeBrightSpec(" + indicatorName + ") - changing brightness level to " + expBrightness);
            debugLog(" changeBrightSpec(" + indicatorName + ") - changing brightness level to " + expBrightness);
            brightSpec.setBrightness(expBrightness);

            m_test.assertTrue(
                    " changeBrightSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setBrightness() with parameter "
                            + expBrightness, expBrightness >= BRIGHTOFF && expBrightness <= maxBrightness);

        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" changeBrightSpec() - caught IllegalArgumentException when attempting to call setBrightness() with parameter "
                    + expBrightness + "; exception: " + iae);
            m_test.assertTrue(
                    " changeBrightSpec() - caught IllegalArgumentException when attempting to call setBrightness() with parameter "
                            + expBrightness + "; exception: " + iae, expBrightness < BRIGHTOFF
                            || expBrightness > maxBrightness);
        }

        printAttributes(indicatorName, getBrightSpecAttributes(indicatorName, brightSpec));

        debugLog(" changeBrightSpec(" + indicatorName + ") - end");

        return brightSpec;
    }

    private ColorSpec changeColorSpec(String indicatorName, ColorSpec colorSpec)
    {
        debugLog(" changeColorSpec(" + indicatorName + ") - begin");

        Hashtable specAttributes = getColorSpecAttributes(colorSpec);

        byte expColor = ((Byte) specAttributes.get(COLOR + " color")).byteValue();

        currentColorIdx++;
        currentColorIdx = currentColorIdx >= colors.length ? 0 : currentColorIdx;
        expColor = colors[currentColorIdx];

        boolean isValidColor = validColor(indicatorName, expColor);

        try
        {
            m_vbox.write(" changeColorSpec(" + indicatorName + ") - changing color to " + expColor);
            debugLog(" changeColorSpec(" + indicatorName + ") - changing color to " + expColor);
            colorSpec.setColor(expColor);

            m_test.assertTrue(" changeColorSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setColor() with parameter " + expColor, isValidColor);
        }
        catch (IllegalArgumentException iae)
        {
            debugLog(" changeColorSpec() - caught IllegalArgumentException when attempting to call setColor() with parameter "
                    + expColor + "; exception: " + iae);
            m_test.assertTrue(
                    " changeColorSpec() - caught IllegalArgumentException when attempting to call setColor() with parameter "
                            + expColor + "; exception: " + iae, !isValidColor);
        }
        catch (SecurityException se)
        {
            debugLog(" changeColorSpec() - caught SecurityException when attempting to call setColor() on indicator "
                    + indicatorName + "; exception: " + se);
            m_test.fail(" changeColorSpec() - caught SecurityException when attempting to call setColor() on indicator "
                    + indicatorName + "; exception: " + se);
        }

        printAttributes(indicatorName, getColorSpecAttributes(colorSpec));

        debugLog(" changeColorSpec(" + indicatorName + ") - end");

        return colorSpec;
    }

    private ScrollSpec changeScrollSpec(ScrollSpec scrollSpec, String attributeToReset)
    {
        debugLog(" changeScrollSpec() - begin");

        Hashtable specAttributes = getScrollSpecAttributes(scrollSpec);

        int maxHRate = ((Integer) specAttributes.get(SCROLL + "'s max horizontal rate")).intValue();
        int currentHRate = ((Integer) specAttributes.get(SCROLL + "'s horizontal rate")).intValue();
        debugLog(" changeScrollSpec() - maxHRate=" + maxHRate + ", current HorizontalRate=" + currentHRate);

        int[] hRates = { -1, 0, maxHRate / 4, maxHRate / 2, maxHRate * 3 / 4, maxHRate, maxHRate + 1 };
        currentScrollHRateIdx++;
        currentScrollHRateIdx = currentScrollHRateIdx >= hRates.length ? 0 : currentScrollHRateIdx;
        int expHRate = hRates[currentScrollHRateIdx];

        int maxVRate = ((Integer) specAttributes.get(SCROLL + "'s max vertical rate")).intValue();
        int currentVRate = ((Integer) specAttributes.get(SCROLL + "'s vertical rate")).intValue();
        debugLog(" changeScrollSpec() - maxVRate=" + maxVRate + ", current Vertical rate=" + currentVRate);

        int[] vRates = { -1, 0, maxVRate / 4, maxVRate / 2, maxVRate * 3 / 4, maxVRate, maxVRate + 1 };

        currentScrollVRateIdx++;
        currentScrollVRateIdx = currentScrollVRateIdx >= vRates.length ? 0 : currentScrollVRateIdx;
        int expVRate = vRates[currentScrollVRateIdx];

        // duration value must be >= 0 and greater than 100/4
        // (4=number of characters to scroll across horizontally)
        // or number of rows to scroll across during vertical scroll
        int cols = m_textDisplay.getNumberColumns();
        int rows = m_textDisplay.getNumberRows();
        int scrollDurationLimit = (100 / cols) < (100 / rows) ? 100 / cols : 100 / rows;

        currentScrollDurationIdx++;
        currentScrollDurationIdx = currentScrollDurationIdx >= durations.length ? 0 : currentScrollDurationIdx;
        int expDuration = durations[currentScrollDurationIdx];

        if (attributeToReset.equals(SCROLL + "-HRATE"))
        {
            try
            {
                m_vbox.write(" changeScrollSpec() - setting scroll HRate to " + expHRate);
                debugLog(" changeScrollSpec() - setting scroll HRate to " + expHRate);
                scrollSpec.setHorizontalIterations(expHRate);

                m_test.assertTrue(
                        " changeScrollSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setHorizontalIterations() with parameter"
                                + expHRate, expHRate >= 0 && expHRate <= maxHRate);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" changeScrollSpec() - caught IllegalArgumentException when attempting to call setHorizontalIterations() with parameter "
                        + expHRate + "; exception: " + iae);
                m_test.assertTrue(
                        " changeScrollSpec() - caught IllegalArgumentException when attempting to call setHorizontalIterations() with parameter "
                                + expHRate + "; exception: " + iae, expHRate > maxHRate || expHRate < 0);
            }
        }

        if (attributeToReset.equals(SCROLL + "-VRATE"))
        {
            try
            {
                scrollSpec.setHorizontalIterations(30);

                m_vbox.write(" changeScrollSpec() - setting scroll VRate to " + expVRate);
                debugLog(" changeScrollSpec() - setting scroll VRate to " + expVRate);
                scrollSpec.setVerticalIterations(expVRate);

                m_test.assertTrue(
                        " changeScrollSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setVerticalIterations() with parameter"
                                + expVRate, expVRate >= 0 && expVRate <= maxVRate);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" changeScrollSpec() - caught IllegalArgumentException when attempting to call setVerticalIterations() with parameter "
                        + expVRate + "; exception: " + iae);
                m_test.assertTrue(
                        " changeScrollSpec() - caught IllegalArgumentException when attempting to call setVerticalterations() with parameter "
                                + expVRate + "; exception: " + iae, expVRate > maxVRate || expVRate < 0);
            }
        }

        if (attributeToReset.equals(SCROLL + "-DURATION"))
        {
            try
            {
                m_vbox.write(" changeScrollSpec() - setting scroll duration to " + expDuration);
                debugLog(" changeScrollSpec() - setting scroll duration to " + expDuration);
                scrollSpec.setHoldDuration(expDuration);

                m_test.assertTrue(
                        " changeScrollSpec() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setHoldDuration() with parameter "
                                + expDuration, expDuration >= 0 && expDuration <= scrollDurationLimit);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" changeScrollSpec() - caught IllegalArgumentException when attempting to call setHoldDuration() with parameter "
                        + expDuration + "; exception: " + iae);
                m_test.assertTrue(
                        " changeScrollSpec() - caught IllegalArgumentException when attempting to call setHoldDuration() with parameter "
                                + expDuration + "; exception: " + iae, expDuration > scrollDurationLimit
                                || expDuration < 0);
            }
        }

        printAttributes(TEXT, getScrollSpecAttributes(scrollSpec));

        debugLog(" changeScrollSpec() - end");

        return scrollSpec;
    }

    private void compareBlinkSpec(String indicatorName, BlinkSpec expSpec, BlinkSpec actualSpec)
    {
        Hashtable expAttributes = getBlinkSpecAttributes(indicatorName, expSpec);
        Hashtable actualAttributes = getBlinkSpecAttributes(indicatorName, actualSpec);

        Enumeration attributeKeys = expAttributes.keys();
        String attributeName = null;
        Number expVal = null;
        Number actualVal = null;
        while (attributeKeys.hasMoreElements())
        {
            attributeName = (String) attributeKeys.nextElement();
            expVal = (Number) expAttributes.get(attributeName);
            actualVal = (Number) actualAttributes.get(attributeName);
            // debugLog(indicatorName +" " +attributeName +"=" +actualVal
            // +", expecting " +expVal);
            m_test.assertTrue(" TEST FAILED - " + indicatorName + attributeName + " is expected to be " + expVal
                    + " not " + actualVal, expVal.equals(actualVal));
        }
    }

    private void compareBrightSpec(String indicatorName, BrightSpec expSpec, BrightSpec actualSpec)
    {
        Hashtable expAttributes = getBrightSpecAttributes(indicatorName, expSpec);
        Hashtable actualAttributes = getBrightSpecAttributes(indicatorName, actualSpec);

        Enumeration attributeKeys = expAttributes.keys();
        String attributeName = null;
        Number expVal = null;
        Number actualVal = null;
        while (attributeKeys.hasMoreElements())
        {
            attributeName = (String) attributeKeys.nextElement();
            expVal = (Number) expAttributes.get(attributeName);
            actualVal = (Number) actualAttributes.get(attributeName);
            // debugLog(indicatorName +" " +attributeName +"=" +actualVal
            // +", expecting " +expVal);
            m_test.assertTrue(" TEST FAILED - " + indicatorName + attributeName + " is expected to be " + expVal
                    + " not " + actualVal, expVal.equals(actualVal));
        }
    }

    private void compareColorSpec(String indicatorName, ColorSpec expSpec, ColorSpec actualSpec)
    {
        Hashtable expAttributes = getColorSpecAttributes(expSpec);
        Hashtable actualAttributes = getColorSpecAttributes(actualSpec);

        Enumeration attributeKeys = expAttributes.keys();
        String attributeName = null;
        Number expVal = null;
        Number actualVal = null;
        while (attributeKeys.hasMoreElements())
        {
            attributeName = (String) attributeKeys.nextElement();
            expVal = (Number) expAttributes.get(attributeName);
            actualVal = (Number) actualAttributes.get(attributeName);
            // debugLog(indicatorName +" " +attributeName +"=" +actualVal
            // +", expecting " +expVal);
            m_test.assertTrue(" TEST FAILED - " + indicatorName + attributeName + " is expected to be " + expVal
                    + " not " + actualVal, expVal.equals(actualVal));
        }
    }

    private void compareScrollSpec(String indicatorName, ScrollSpec expSpec, ScrollSpec actualSpec)
    {
        Hashtable expAttributes = getScrollSpecAttributes(expSpec);
        Hashtable actualAttributes = getScrollSpecAttributes(actualSpec);

        Enumeration attributeKeys = expAttributes.keys();
        String attributeName = null;
        Number expVal = null;
        Number actualVal = null;
        while (attributeKeys.hasMoreElements())
        {
            attributeName = (String) attributeKeys.nextElement();
            expVal = (Number) expAttributes.get(attributeName);
            actualVal = (Number) actualAttributes.get(attributeName);
            // debugLog(indicatorName +" " +attributeName +"=" +actualVal
            // +", expecting " +expVal);
            m_test.assertTrue(" TEST FAILED - " + indicatorName + attributeName + " is expected to be " + expVal
                    + " not " + actualVal, expVal.equals(actualVal));
        }
    }

    private void resetBlinkSpec(String indicatorName)
    {
        print(SECTION_DIVIDER);
        String str = indicatorName;
        int separatorIdx = str.indexOf(" ");
        indicatorName = str.substring(0, separatorIdx);
        String attributeToReset = str.substring(separatorIdx + 1);

        debugLog(" resetBlinkSpec(" + indicatorName + ") - begin");

        retrieveIndDisplays("ind");
        Indicator indicator = (Indicator) m_indicatorDisp.getIndicators().get(indicatorName);
        if (indicator == null)
        {
            print(" resetBlinkSpec("
                    + indicatorName
                    + ") - unable to retrieve desired indicator from indicatorDisplay.getIndicators(), is it reserved? "
                    + checkReservation(indicatorName));
            m_test.assertTrue(
                    " resetBlinkSpec("
                            + indicatorName
                            + ") - TEST FAILED: unable to retrieve desired indicator from indicatorDisplay.getIndicators() even though it is reserved",
                    !checkReservation(indicatorName));
        }
        else
        {
            BlinkSpec blinkSpec = indicator.getBlinkSpec();

            blinkSpec = changeBlinkSpec(indicatorName, blinkSpec, attributeToReset);

            try
            {
                indicator.setBlinkSpec(blinkSpec);
            }
            catch (IllegalStateException ise)
            {
                debugLog(" resetBlinkSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setBlinkSpec(); exception: " + ise);
                m_test.fail(" resetBlinkSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setBlinkSpec(); exception: " + ise);
            }

            compareBlinkSpec(indicatorName, blinkSpec, indicator.getBlinkSpec());
        }

        debugLog(" resetBlinkSpec(" + indicatorName + ") - end");
    }

    private void resetBrightSpec(String indicatorName)
    {
        print(SECTION_DIVIDER);
        debugLog(" resetBrightSpec(" + indicatorName + ") - begin");

        retrieveIndDisplays("ind");
        Indicator indicator = (Indicator) m_indicatorDisp.getIndicators().get(indicatorName);
        if (indicator == null)
        {
            print(" resetBrightSpec("
                    + indicatorName
                    + ") - unable to retrieve desired indicator from indicatorDisplay.getIndicators(), is it reserved? "
                    + checkReservation(indicatorName));
            m_test.assertTrue(
                    " resetBrightSpec("
                            + indicatorName
                            + ") - TEST FAILED: unable to retrieve desired indicator from indicatorDisplay.getIndicators() even though it is reserved",
                    !checkReservation(indicatorName));
        }
        else
        {
            BrightSpec brightSpec = indicator.getBrightSpec();
            brightSpec = changeBrightSpec(indicatorName, brightSpec);

            try
            {
                indicator.setBrightSpec(brightSpec);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" resetBrightSpec(" + indicatorName
                        + ") - caught IllegalArgumentException when attempting to call setBrightSpec(); exception: "
                        + iae);
                m_test.assertTrue(
                        " resetBrightSpec("
                                + indicatorName
                                + ") - TEST FAILED: caught IllegalArgumentException when attempting to call setBrightSpec(brightSpec) where brightSpec is not null; exception: "
                                + iae, brightSpec == null);
            }
            catch (IllegalStateException ise)
            {
                debugLog(" resetBrightSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setBrightSpec(); exception: " + ise);
                m_test.fail(" resetBrightSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setBrightSpec(); exception: " + ise);
            }

            compareBrightSpec(indicatorName, brightSpec, indicator.getBrightSpec());
        }

        debugLog(" resetBrightSpec(" + indicatorName + ") - end");
    }

    private void resetColorSpec(String indicatorName)
    {
        print(SECTION_DIVIDER);
        debugLog(" resetColorSpec(" + indicatorName + ") - begin");

        retrieveIndDisplays("ind");
        Indicator indicator = (Indicator) m_indicatorDisp.getIndicators().get(indicatorName);
        if (indicator == null)
        {
            print(" resetColorSpec("
                    + indicatorName
                    + ") - unable to retrieve desired indicator from indicatorDisplay.getIndicators(), is it reserved? "
                    + checkReservation(indicatorName));
            m_test.assertTrue(
                    " resetColorSpec("
                            + indicatorName
                            + ") - TEST FAILED: unable to retrieve desired indicator from indicatorDisplay.getIndicators() even though it is reserved",
                    !checkReservation(indicatorName));
        }
        else
        {
            ColorSpec colorSpec = indicator.getColorSpec();
            colorSpec = changeColorSpec(indicatorName, colorSpec);

            try
            {
                indicator.setColorSpec(colorSpec);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" resetColorSpec(" + indicatorName
                        + ") - caught IllegalArgumentException when attempting to call setColorSpec(); exception: "
                        + iae);
                m_test.assertTrue(
                        " resetColorSpec("
                                + indicatorName
                                + ") - TEST FAILED: caught IllegalArgumentException when attempting to call setColorSpec(colorSpec) where colorSpec is not null; exception: "
                                + iae, colorSpec == null);
            }
            catch (IllegalStateException ise)
            {
                debugLog(" resetColorSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setColorSpec(); exception: " + ise);
                m_test.fail(" resetColorSpec(" + indicatorName
                        + ") - caught IllegalStateException when attempting to call setColorSpec(); exception: " + ise);
            }

            compareColorSpec(indicatorName, colorSpec, indicator.getColorSpec());
        }

        debugLog(" resetColorSpec(" + indicatorName + ") - end");
    }

    private void resetTextDisplay(String specName, boolean useSetText)
    {
        print(SECTION_DIVIDER);
        debugLog(" resetTextDisplay() - begin");

        retrieveIndDisplays("text");
        m_textDisplay = m_fpMgr.getTextDisplay();
        if (m_textDisplay != null)
        {
            byte currentMode = (byte) m_textDisplay.getMode();
            byte desiredMode = currentMode;


            String[] desiredText = { "DEFAULT" };

            BlinkSpec desiredBlink = m_textDisplay.getBlinkSpec();
            BrightSpec desiredBright = m_textDisplay.getBrightSpec();
            ColorSpec desiredColor = m_textDisplay.getColorSpec();
            ScrollSpec desiredScroll = m_textDisplay.getScrollSpec();

            BlinkSpec expBlink = desiredBlink;
            BrightSpec expBright = desiredBright;
            ColorSpec expColor = desiredColor;
            ScrollSpec expScroll = desiredScroll;

            if (specName.equals(BRIGHT)) // reset bright spec
            {
                desiredBright = changeBrightSpec(TEXT, expBright);
                expBright = desiredBright;
            }
            if (specName.equals(COLOR)) // reset color spec
            {
                desiredColor = changeColorSpec(TEXT, expColor);
                expColor = desiredColor;
            }
            if (specName.equals(BLINK + "-RATE")) // reset blink spec
            {
                desiredBlink = changeBlinkSpec(TEXT, desiredBlink, specName);
                expBlink = desiredBlink;
            }
            if (specName.equals(BLINK + "-DURATION")) // reset blink spec
            {
                desiredBlink = changeBlinkSpec(TEXT, desiredBlink, specName);
                expBlink = desiredBlink;
            }
            if (specName.startsWith(SCROLL + "-")) // reset scroll spec
            {
                desiredScroll = changeScrollSpec(expScroll, specName);
                expScroll = desiredScroll;
            }

            if (specName.equals("TEXT")) // reset text string displayed
            {
                desiredBright = null;
                desiredColor = null;

                currentTextStrIdx++;
                currentTextStrIdx = currentTextStrIdx >= textStrings.length ? 0 : currentTextStrIdx;
                desiredText = textStrings[currentTextStrIdx];

                debugLog(" resetTextDisplay() - setting text display text to:");
                for (int i = 0; i < desiredText.length; i++)
                    debugLog("  " + desiredText[i]);

                try
                {
                    m_textDisplay.setTextDisplay(desiredText, desiredBlink, desiredScroll, desiredBright, desiredColor);
                    desiredMode = TextDisplay.STRING_MODE;
                }
                catch (IllegalArgumentException iae)
                {
                    debugLog(" resetTextDisplay() - caught IllegalArgumentException when attempting to call setTextDisplay(); exception: "
                            + iae);
                    m_test.assertTrue(
                            " resetTextDisplay() - caught IllegalArgumentException when attempting to setTextDisplay() with a String array of size "
                                    + desiredText.length + " as the text parameter; exception: " + iae,
                            textWrap == false);
                }
                catch (IllegalStateException ise)
                {
                    debugLog(" resetTextDisplay() - caught IllegalStateException when attempting to call setTextDisplay(); exception: "
                            + ise);
                    m_test.fail(" resetTextDisplay() - caught IllegalStateException when attempting to call setTextDisplay(); exception: "
                            + ise);
                }
            }
            if (specName.equals("MODE")) // reset display mode
            {
                desiredBright = null;
                desiredColor = null;

                debugLog(" resetTextDisplay() - current text display clock mode is " + currentMode);

                currentTextModeIdx++;
                currentTextModeIdx = currentTextModeIdx >= clockModes.length ? 0 : currentTextModeIdx;

                desiredMode = clockModes[currentTextModeIdx];

                try
                {
                    debugLog(" resetTextDisplay() - setting text display clock mode to " + desiredMode);
                    m_textDisplay.setClockDisplay(desiredMode, desiredBlink, desiredScroll, desiredBright, desiredColor);
                    m_test.assertTrue(
                            " resetTextDisplay() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setClockDisplay() with mode parameter "
                                    + desiredMode, desiredMode != clockModes[clockModes.length - 1]);
                }
                catch (IllegalArgumentException iae)
                {
                    debugLog(" resetTextDisplay() - caught IllegalArgumentException when attempting to call setClockDisplay() with mode parameter "
                            + desiredMode + "; exception: " + iae);
                    m_test.assertTrue(
                            " resetTextDisplay() - TEST FAILED: caught IllegalArgumentException when attempting to call setClockDisplay() with mode parameter "
                                    + desiredMode + "; exception: " + iae,
                            desiredMode == clockModes[clockModes.length - 1]);
                    desiredMode = currentMode;
                }
                catch (IllegalStateException ise)
                {
                    debugLog(" resetTextDisplay() - caught IllegalStateException when attempting to call setClockDisplay(); exception: "
                            + ise);
                    m_test.fail(" resetTextDisplay() - caught IllegalStateException when attempting to call setClockDisplay(); exception: "
                            + ise);
                    desiredMode = currentMode;
                }
            }

            if (specName.equals("WRAP")) // reset text wrap attribute
            {
                textWrap = !textWrap;
                m_textDisplay.setWrap(textWrap);
                debugLog(" resetTextDisplay() - wrapping reset as " + textWrap);
            }

            if (!(specName.equals("MODE")) && !(specName.equals("TEXT")))
            {
                // set blink, scroll, bright, color via setTextDisplay()
                if (useSetText)
                {
                    try
                    {
                        m_textDisplay.setTextDisplay(desiredText, desiredBlink, desiredScroll, desiredBright, desiredColor);
                    desiredMode = TextDisplay.STRING_MODE;
                    }
                    catch (IllegalArgumentException iae)
                    {
                        debugLog(" resetTextDisplay() - caught IllegalArgumentException when attempting to call setTextDisplay(); exception: " + iae);
                        m_test.assertTrue( " resetTextDisplay() - caught IllegalArgumentException when attempting to setTextDisplay() with a String array of size " + desiredText.length + " as the text parameter; exception: " + iae, textWrap == false);
                    }
                    catch (IllegalStateException ise)
                    {
                        debugLog(" resetTextDisplay() - caught IllegalStateException when attempting to call setTextDisplay(); exception: " + ise);
                        m_test.fail(" resetTextDisplay() - caught IllegalStateException when attempting to call setTextDisplay(); exception: " + ise);
                    }
                }
                // set blink, scroll, bright, color via setClockDisplay()
                else
                {
                    if (desiredMode == TextDisplay.STRING_MODE) 
                    {
                        desiredMode = TextDisplay.TWENTYFOUR_HOUR_CLOCK;
                    }

                    try
                    {
                        m_textDisplay.setClockDisplay(desiredMode, desiredBlink, desiredScroll, desiredBright, desiredColor);
                        m_test.assertTrue(
                                " resetTextDisplay() - TEST FAILED: failure to throw IllegalArgumentException when attempting to call setClockDisplay() with mode parameter " + desiredMode, desiredMode != clockModes[clockModes.length - 1]);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        debugLog(" resetTextDisplay() - caught IllegalArgumentException when attempting to call setClockDisplay() with mode parameter " + desiredMode + "; exception: " + iae);
                        m_test.assertTrue(
                                " resetTextDisplay() - TEST FAILED: caught IllegalArgumentException when attempting to call setClockDisplay() with mode parameter " + desiredMode + "; exception: " + iae, desiredMode == clockModes[clockModes.length - 1]);
                    }
                    catch (IllegalStateException ise)
                    {
                        debugLog(" resetTextDisplay() - caught IllegalStateException when attempting to call setClockDisplay(); exception: "
                                + ise);
                        m_test.fail(" resetTextDisplay() - caught IllegalStateException when attempting to call setClockDisplay(); exception: "
                                + ise);
                    }
                }

            }
            int mode = m_textDisplay.getMode();
            m_test.assertTrue(" resetTextDisplay() - Test Failure: expected text display mode is " + desiredMode
                    + ", actual mode is " + mode, mode == desiredMode);

            compareBlinkSpec(TEXT, expBlink, m_textDisplay.getBlinkSpec());
            compareBrightSpec(TEXT, expBright, m_textDisplay.getBrightSpec());
            compareColorSpec(TEXT, expColor, m_textDisplay.getColorSpec());
            compareScrollSpec(TEXT, expScroll, m_textDisplay.getScrollSpec());

            printAttributes("Text Display", getTextAttributes(m_textDisplay));
        }

        debugLog(" resetTextDisplay() - end");
    }

    private void toggleIndicatorReservation(String indName, boolean reserve)
    {
        debugLog(" toggleIndicatorReservation(" + indName + ") - begin, reserve=" + reserve);

        if (reserve)
        {
            try
            { // successful reservation
                if (m_fpMgr.reserveIndicator(this, indName))
                {
                    if (!m_reservedInds.contains(indName)) m_reservedInds.addElement(indName);
                }
                else
                // reservation failed
                {
                    if (m_reservedInds.contains(indName)) m_reservedInds.removeElement(indName);

                    print("Failure Error: failure to reserve " + indName);
                    m_test.fail("Failure Error: failure to reserve " + indName);
                }
            }
            catch (IllegalArgumentException iae)
            {
                print("FrontPanelManager.reserveIndicator(" + indName + ") failed due to IllegalArgumentException: "
                        + iae);
                m_test.fail("toggleIndicatorReservation() failed - FrontPanelManager.reserveIndicator(" + indName
                        + ") failed due to IllegalArgumentException: " + iae);
            }
        }
        else
        // unreserve
        {
            try
            {
                m_fpMgr.releaseIndicator(indName);

                if (m_reservedInds.contains(indName)) m_reservedInds.removeElement(indName);
            }
            catch (IllegalArgumentException iae)
            {
                debugLog(" toggleIndicatorReservation() - caught IllegalArgumentException when attempting to release indicator "
                        + indName + "; exception: " + iae);
                m_test.fail(" toggleIndicatorReservation() - caught IllegalArgumentException when attempting to release indicator "
                        + indName + "; exception: " + iae);
            }
        }

        debugLog(" toggleIndicatorReservation() " + indName + " - end");
    }

    private void toggleTextDispReservation(boolean reserve)
    {
        debugLog(" toggleTextDispReservation() - begin");

        if (reserve)
        {
            if (m_fpMgr.reserveTextDisplay(this)) // reserved successfully
            {
                m_textReserved = true;
            }
            else
            // text display reservation failed
            {
                m_textReserved = false;

                print("Failure Error: failure to reserve text display");
                m_test.fail("Test Failed: failure to reserve text display");
            }

        }
        else
        {
            m_fpMgr.releaseTextDisplay();

            m_textReserved = false;
        }

        debugLog(" toggleTextDispReservation() - end");
    }

    private boolean checkReservation(String indName)
    {
        if (m_reservedInds.contains(indName)) return true;

        return false;
    }

    private void getAllSupportedInds()
    {
        m_supportedInds.removeAllElements();

        String[] supportedInds = m_fpMgr.getSupportedIndicators();
        for (int i = 0; i < supportedInds.length; i++)
        {
            m_supportedInds.addElement(supportedInds[i]);
        }

        if (m_fpMgr.isTextDisplaySupported())
        {
            m_supportedInds.addElement("text");
        }

        print(SECTION_DIVIDER);

        String indName = null;
        print("Number of Supported Indicator = " + m_supportedInds.size());
        for (int i = 0; i < m_supportedInds.size(); i++)
        {
            indName = (String) m_supportedInds.get(i);
            print("\t" + indName + " supported.");
        }
    }

    private void printReservedIndicators()
    {
        print(SECTION_DIVIDER);

        String reservedIndName = null;
        print("Number of reserved indicators = " +
            (m_textReserved? m_reservedInds.size() + 1: m_reservedInds.size()));
        for (int i = 0; i < m_reservedInds.size(); i++)
        {
            reservedIndName = (String) m_reservedInds.get(i);
            print("\t" + reservedIndName + " reserved.");
        }
        if (m_textReserved)
        {
            print("\t" +TEXT +" reserved.");
        }
    }

    // 
    // Key Handling methods
    // 
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        debugLog("keyPressed: " + e.getKeyText(key) + " (" + key + ")");

        switch (key)
        {
            case OCRcEvent.VK_LIST: 
                getAllSupportedInds();
                reserveAllSupportedIndicators();
                retrieveIndDisplays("all");
                break;

            case OCRcEvent.VK_GUIDE: 
                getAllSupportedInds();
                reserveAllSupportedIndicators();
                retrieveIndDisplays("all");
                getAllIndicatorSpecs();
                printTextDisplay();
                break;

            case OCRcEvent.VK_COLORED_KEY_1: // green diamond
            case OCRcEvent.VK_COLORED_KEY_0: // red circle
            case OCRcEvent.VK_COLORED_KEY_2: // blue square (B)
            case OCRcEvent.VK_COLORED_KEY_3: // yellow triangle (A)
                break;

            case OCRcEvent.VK_0:
            case OCRcEvent.VK_1:
            case OCRcEvent.VK_2:
            case OCRcEvent.VK_3:
            case OCRcEvent.VK_4:
            case OCRcEvent.VK_5:
            case OCRcEvent.VK_6:
            case OCRcEvent.VK_7:
            case OCRcEvent.VK_8:
            case OCRcEvent.VK_9:
                m_testNumber *= 10;
                m_testNumber += key - HRcEvent.VK_0;
                break;

            case OCRcEvent.VK_ENTER:
                debugLog("selectd test " + m_testNumber);
                if (guiState == 0)
                {
                    getTestList(m_testNumber);
                    printTestList();
                    guiState = 1;
                }
                else
                {
                    launchTest(m_testNumber);
                }

                m_testNumber = 0;
                break;

            case OCRcEvent.VK_EXIT:
                m_testNumber = 0;
                guiState = 0;
                printTestGroup();
                break;

            case OCRcEvent.VK_INFO:
                if (guiState == 0)
                    printTestGroup();
                else
                    printTestList();
                break;

            default:
                break;

        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * ResourceClient
     */
    public void notifyRelease(ResourceProxy proxy)
    {
    }

    public void release(ResourceProxy proxy)
    {
    }

    public boolean requestRelease(ResourceProxy proxy, Object requestData)
    {
        return true;
    }

    //
    // For autoxlet automation framework (Driveable interface)
    // 
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);
                m_eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }

    private void getTestList(int testGroupNum)
    {
        debugLog(" getTestList - begin");
        m_testList.removeAllElements();


        switch (testGroupNum)
        {
            case 1:
                getIndicatorSpecs("power");
                m_testList.addElement("toggle POWER indicator reservation");
                m_testList.addElement("reset POWER indicator blink spec (rate)");
                m_testList.addElement("reset POWER indicator blink spec (duration)");
                m_testList.addElement("reset POWER indicator bright spec");
                m_testList.addElement("reset POWER indicator color spec");
                break;

            case 2:
                getIndicatorSpecs("record");
                m_testList.addElement("toggle RECORD indicator reservation");
                m_testList.addElement("reset RECORD indicator blink spec (rate)");
                m_testList.addElement("reset RECORD indicator blink spec (duration)");
                m_testList.addElement("reset RECORD indicator bright spec");
                m_testList.addElement("reset RECORD indicator color spec");
                break;

            case 3:
                getIndicatorSpecs("message");
                m_testList.addElement("toggle MESSAGE indicator reservation");
                m_testList.addElement("reset MESSAGE indicator blink spec (rate)");
                m_testList.addElement("reset MESSAGE indicator blink spec (duration)");
                m_testList.addElement("reset MESSAGE indicator bright spec");
                m_testList.addElement("reset MESSAGE indicator color spec");
                break;

            case 4:
                getIndicatorSpecs("rfbypass");
                m_testList.addElement("toggle RFBYPASS indicator reservation");
                m_testList.addElement("reset RFBYPASS indicator blink spec (rate)");
                m_testList.addElement("reset RFBYPASS indicator blink spec (duration)");
                m_testList.addElement("reset RFBYPASS indicator bright spec");
                m_testList.addElement("reset RFBYPASS indicator color spec");
                break;

            case 5:
                printTextDisplay();
                m_testList.addElement("toggle TEXT DISPLAY reservation");
                m_testList.addElement("reset TEXT DISPLAY text string setting");
                m_testList.addElement("erase TEXT DISPLAY text");
                m_testList.addElement("toggle TEXT DISPLAY text wrap setting");
                m_testList.addElement("reset TEXT DISPLAY blink spec (rate)");
                m_testList.addElement("reset TEXT DISPLAY blink spec (duration)");
                m_testList.addElement("reset TEXT DISPLAY bright spec");
                m_testList.addElement("reset TEXT DISPLAY color spec");
                m_testList.addElement("reset TEXT DISPLAY scroll spec (horizontal rate)");
                m_testList.addElement("reset TEXT DISPLAY scroll spec (vertical rate)");
                m_testList.addElement("reset TEXT DISPLAY scroll spec (hold duration)");
                m_testList.addElement("reset TEXT DISPLAY CLOCK mode setting");

                m_testList.addElement("reset TEXT DISPLAY CLOCK blink spec (rate)");
                m_testList.addElement("reset TEXT DISPLAY CLOCK blink spec (duration)");
                m_testList.addElement("reset TEXT DISPLAY CLOCK bright spec");
                m_testList.addElement("reset TEXT DISPLAY CLOCK color spec");
                m_testList.addElement("reset TEXT DISPLAY CLOCK scroll spec (horizontal rate)");
                m_testList.addElement("reset TEXT DISPLAY CLOCK scroll spec (vertical rate)");
                m_testList.addElement("reset TEXT DISPLAY CLOCK scroll spec (hold duration)");
                break;

            default:
                print("Unsupported test group selection: " + testGroupNum);
                break;
        }

        debugLog(" getTestList - end");
    }

    private void launchTest(int testNumber)
    {
        String testName = (String) m_testList.elementAt(testNumber);
        debugLog(" launchTest(" + testNumber + ") - about to launch test " + testName);
        String indicatorToTest = testName.substring(testName.indexOf(" ") + 1);
        indicatorToTest = indicatorToTest.substring(0, indicatorToTest.indexOf(" "));
        indicatorToTest = indicatorToTest.toLowerCase();

        if (!indicatorToTest.startsWith("text"))
        {
            switch (testNumber)
            {
                case 0:
                    boolean reserved = m_reservedInds.contains(indicatorToTest.toLowerCase());
                    toggleIndicatorReservation(indicatorToTest.toLowerCase(), !reserved);
                    printReservedIndicators();
                    break;
                case 1:
                    resetBlinkSpec(indicatorToTest + " " + BLINK + "-RATE");
                    break;
                case 2:
                    resetBlinkSpec(indicatorToTest + " " + BLINK + "-DURATION");
                    break;
                case 3:
                    resetBrightSpec(indicatorToTest);
                    break;
                case 4:
                    resetColorSpec(indicatorToTest);
                    break;
            }
        }
        else
        {
            switch (testNumber)
            {
                case 0:
                    toggleTextDispReservation(!(m_reservedInds.contains("text")));
                    printReservedIndicators();
                    break;

                case 1:
                    resetTextDisplay("TEXT", true);
                    break;
                case 2:
                    eraseText();
                    break;
                case 3:
                    resetTextDisplay("WRAP", true);
                    break;
                case 4:
                    resetTextDisplay(BLINK + "-RATE", true);
                    break;
                case 5:
                    resetTextDisplay(BLINK + "-DURATION", true);
                    break;
                case 6:
                    resetTextDisplay(BRIGHT, true);
                    break;
                case 7:
                    resetTextDisplay(COLOR, true);
                    break;
                case 8:
                    resetTextDisplay(SCROLL + "-HRATE", true);
                    break;
                case 9:
                    resetTextDisplay(SCROLL + "-VRATE", true);
                    break;
                case 10:
                    resetTextDisplay(SCROLL + "-DURATION", true);
                    break;
                case 11:
                    resetTextDisplay("MODE", true);
                    break;
                case 12:
                    resetTextDisplay(BLINK + "-RATE", false);
                    break;
                case 13:
                    resetTextDisplay(BLINK + "-DURATION", false);
                    break;
                case 14:
                    resetTextDisplay(BRIGHT, false);
                    break;
                case 15:
                    resetTextDisplay(COLOR, false);
                    break;
                case 16:
                    resetTextDisplay(SCROLL + "-HRATE", false);
                    break;
                case 17:
                    resetTextDisplay(SCROLL + "-VRATE", false);
                    break;
                case 18:
                    resetTextDisplay(SCROLL + "-DURATION", false);
                    break;
            }
        }
    }

    // 
    // display info on xlet usage
    // 
    private void printCommonTests()
    {
        print("Press red circle for list of all available indicators and text display");
        print("Press green diamond for list of all reserved indicators and text display");
        print("Press blue square for current status of all available indicators");
        print("Press yellow triangle for current status of TextDisplay\n");

        print("Press GUIDE to reserve all available indicators and text display");
        print("Test INFO to get all available indicators and text display\n");
    }

    private void printTestGroup()
    {
        print(SECTION_DIVIDER);
        printCommonTests();

        print("Test Group 1: Power Indicator tests");
        print("Test Group 2: Record Indicator tests");
        print("Test Group 3: Message Indicator tests");
        print("Test Group 4: RFBypass Indicator tests");
        print("Test Group 5: Text Display tests");
    }

    private void printTestList()
    {
        print(SECTION_DIVIDER);
        printCommonTests();

        for (int i = 0; i < m_testList.size(); i++)
        {
            print("Test " + i + " : " + (String) m_testList.elementAt(i));
        }
    }

    //
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

}
