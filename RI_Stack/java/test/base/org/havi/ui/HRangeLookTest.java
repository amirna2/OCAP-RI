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

package org.havi.ui;

import org.cablelabs.test.*;
import junit.framework.*;

import org.havi.ui.event.*;
import java.awt.*;

import org.w3c.dom.*;
import org.cablelabs.gear.test.XMLTestConfig;
import org.cablelabs.gear.test.XMLTestConfig.TestData;

/**
 * Tests {@link #HRangeLook}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski (rewrite)
 * @version $Revision: 1.6 $, $Date: 2002/11/07 21:14:08 $
 */
public class HRangeLookTest extends AbstractLookTest
{
    /**
     * Standard constructor.
     */
    public HRangeLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HRangeLookTest(String s, Object params)
    {
        super(s, params);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        htestlook = new HRangeLook();

        hlook = new HRangeLook()
        {
            public void fillBackground(Graphics g, HVisible visible, int state)
            {
                super.fillBackground(g, visible, state);
                backgroundCall = callIndex++;
            }

            public void renderBorders(Graphics g, HVisible visible, int state)
            {
                super.renderBorders(g, visible, state);
                borderCall = callIndex++;
            }

            public void renderVisible(Graphics g, HVisible visible, int state)
            {
                super.renderVisible(g, visible, state);
                visibleCall = callIndex++;
            }
        };

        // so we can be in ALL states
        // don't do automatic state transitions
        hvisible = new HRangeValue()
        {
            public void processHActionEvent(HActionEvent e)
            {
            }

            public void processHFocusEvent(HFocusEvent e)
            {
            }
        };
    }

    /**
     * Suite.
     */
    public static TestSuite suite() throws Exception
    {
        return AbstractLookTest.suite(HRangeLookTest.class);
    }

    /**
     * Calculates the maximum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the maximum dimensions of the content in question
     */
    protected Dimension getContentMaxSize(HVisible v)
    {
        return HaviTestToolkit.getToolkit().getContentMaxSize(v);
    }

    /**
     * Calculates the minimum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the minimum dimensions of the content in question
     */
    protected Dimension getContentMinSize(HVisible v)
    {
        return HaviTestToolkit.getToolkit().getContentMinSize(v);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addStateContent(HVisible v)
    {
        // none
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addSizedContent(HVisible v)
    {
        // none
    }

    /**
     * Used to determine if the look being tested supports scaling or not.
     * 
     * @return <code>true</cod> if scaling is supported;
     * <code>false</code> otherwise
     */
    protected boolean isScalingSupported()
    {
        return false;
    }

    /**
     * Used to determine if state-specific content should be tested.
     * 
     * @return <code>true</code> if state-specific content is drawn and should
     *         be tested; <code>false</code> otherwise
     */
    protected boolean hasStateSpecificContent()
    {
        return false;
    }

    /**
     * Used to determine if the look uses any of the content from the HVisible.
     * 
     * @return <code>true</code> if the look uses content; <code>false</code>
     *         otherwise
     */
    protected boolean usesContent()
    {
        return false;
    }

    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HAdjustableLook
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testImplements(htestlook.getClass(), HAdjustableLook.class);
    }

    /**
     * Test the single constructor of HRangeLook.
     * <ul>
     * <li>HRangeLook()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HRangeLook()", (HRangeLook) hlook);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HRangeLook l)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", l);

        // Check variables NOT exposed in constructors
    }

    /**
     * Should be overridden by subclasses to create an instance of HVisible
     * (appropriate for the given test) which overrides the repaint methods and
     * causes <code>repainted[0] = true</code>.
     */
    protected HVisible createRepaintVisible(final boolean[] repainted)
    {
        return new HRangeValue()
        {
            public void repaint()
            {
                if (repainted != null) repainted[0] = true;
            }

            public void repaint(int x, int y, int w, int h)
            {
                repaint();
            }

            public void repaint(long ms)
            {
                repaint();
            }

            public void repaint(long ms, int x, int y, int w, int h)
            {
                repaint();
            }
        };
    }

    /**
     * Tests widgetChanged().
     * <ul>
     * <li>Ensures that at least a repaint is invoked when UNKNOWN_CHANGE is
     * sent.
     * </ul>
     */
    public void testWidgetChanged() throws Exception
    {
        super.testWidgetChanged();
    }

    /**
     * Tests hitTest().
     * <ul>
     * <li>Test that the correct hitTest value is returned for a variety of
     * range configurations, and with a variety of hit points.
     * </ul>
     */
    public void testHitTest()
    {
        // load the "hittest" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "hittest");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);

        // iterate through all test cases testing hitTest()
        for (int i = 0; i < testCases.length; i++)
        {

            // Get the input templates to test with (2 total)
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            assertTrue("must have exactly 2 input templates for each testcase in this test, \n"
                    + "one is the HRangeValue to test with, and the other is the point to test",
                    (inputData.length == 2));

            // find the "range" input data
            TestData[] rangeData = XMLTestConfig.subsetTestData(inputData, "range");

            // make sure that one is found
            assertTrue("test case " + i + "should have an \"Input\" tag with the " + "command \"range\"",
                    (rangeData.length == 1));

            // make sure that it's object is an HRangeValue
            assertTrue("the \"range\" input in test case " + i + " should contain an HRangeValue object.",
                    (rangeData[0].data instanceof HRangeValue));

            HRangeValue range = (HRangeValue) rangeData[0].data;

            // find the "hitpoint" input data
            TestData[] hitPointData = XMLTestConfig.subsetTestData(inputData, "hitpoint");

            // make sure that one is found
            assertTrue("test case " + i + "should have an \"Input\" tag with the " + "command \"hitpoint\"",
                    (hitPointData.length == 1));

            // make sure that it's object is a Point
            assertTrue("the \"hitpoint\" input in test case " + i + " should contain a Point object.",
                    (hitPointData[0].data instanceof Point));

            Point hitPoint = (Point) hitPointData[0].data;

            // execute the hitTest
            int actualHitValue = ((HRangeLook) range.getLook()).hitTest(range, hitPoint);

            // get result template to compare against
            TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);

            for (int k = 0; k < resultData.length; k++)
            {
                if (resultData[k].command.equals("compareequal") && resultData[k].misc.equals("hitvalue"))
                {
                    assertTrue("the \"hitvalue\" result data in test case " + i
                            + " should contain an object of type Integer", (resultData[k].data instanceof Integer));

                    assertEquals("the incorrect hitValue was returned in test case" + i,
                            ((Integer) resultData[k].data).intValue(), actualHitValue);
                }
                // else if(resultData.command.equals("comparecompatible")
            }
        }
    }

    /**
     * Tests getValue().
     * <ul>
     * <li>If outside the component, should return null.
     * <li>Should not change the component.
     * <li>Returns the value of the component which corresponds to the pointer
     * position specified.
     * </ul>
     */
    public void xxxtestValue()
    {
        /* !!!FINISH!!! */
        fail("Unimplemented test");
    }

    /**
     * Take a snapshot of the current "look" for verification...
     */
    private void doSnapshot(String title, String[] snapStrs, String file, HStaticRange range)
    {

        HScene testScene = getHScene();
        testScene.add(range);
        try
        {
            range.setSize(range.getPreferredSize());
            testScene.show();
        }
        catch (Exception e)
        {
            System.err.println(e);
        }

        try
        {
            mouseMove(testScene, 0, 0);
            TestSupport.checkDisplay(range, title, snapStrs, file, this);
        }
        catch (Exception e2)
        {
            System.err.println(e2);
        }
        finally
        {
            testScene.remove(range);
        }
    }

    /**
     * Tests showLook().
     * <ul>
     * <li>Should test correct display of HStaticRange.
     * <li>Should probably be a parameterized test.
     * </ul>
     */
    public void testShowLook()
    {
        HRange range = new HRange(HOrientable.ORIENT_LEFT_TO_RIGHT, 0, 100, 0);

        // Horizontal, left to right orientation look tests...
        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setBackground(Color.blue);
        range.setForeground(Color.yellow);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot(
                "ShowLook: ->Horizontal range, left to right orientation",
                new String[] { "Should see 200x30 horizontal blue range with yellow thumb at minimum position (left)" },
                "ShowLook_L2R_Pos_min", range);

        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setValue(50);
        range.setBackground(Color.green);
        range.setForeground(Color.pink);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Horizontal range, left to right orientation",
                new String[] { "Should see 200x30 horizontal green range with pink thumb at middle position" },
                "ShowLook_L2R_Pos_half", range);

        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setValue(100);
        range.setBackground(Color.red);
        range.setForeground(Color.cyan);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Horizontal range, left to right orientation",
                new String[] { "Should see 200x30 horizontal red range with cyan thumb at maximum position (right)" },
                "ShowLook_L2R_Pos_max", range);

        // Horizontal, right to left orientation look tests...
        range.setOrientation(HOrientable.ORIENT_RIGHT_TO_LEFT);
        range.setValue(100);
        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setBackground(Color.blue);
        range.setForeground(Color.yellow);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot(
                "ShowLook: ->Horizontal range, right to left orientation",
                new String[] { "Should see 200x30 horizontal blue range with yellow thumb at maximum position (left)" },
                "ShowLook_R2L_Pos_max", range);

        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setValue(50);
        range.setBackground(Color.green);
        range.setForeground(Color.pink);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Horizontal range, right to left orientation",
                new String[] { "Should see 200x30 horizontal green range with pink thumb at middle position" },
                "ShowLook_R2L_Pos_half", range);

        range.setBounds(0, 0, 200, 30);
        range.setThumbOffsets(3, 3);
        range.setValue(0);
        range.setBackground(Color.red);
        range.setForeground(Color.cyan);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Horizontal range, right to left orientation",
                new String[] { "Should see 200x30 horizontal red range with cyan thumb at minimum position (right)" },
                "ShowLook_R2L_Pos_min", range);

        // Vertical, top to bottom orientation look tests...
        range.setOrientation(HOrientable.ORIENT_TOP_TO_BOTTOM);
        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(0);
        range.setBackground(Color.blue);
        range.setForeground(Color.pink);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, top to bottom orientation",
                new String[] { "Should see vertical blue range with pink thumb at minimum position (top)" },
                "ShowLook_T2B_Pos_min", range);

        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(50);
        range.setBackground(Color.yellow);
        range.setForeground(Color.green);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, top to bottom orientation",
                new String[] { "Should see vertical yellow range with green thumb at middle position" },
                "ShowLook_T2B_Pos_half", range);

        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(100);
        range.setBackground(Color.pink);
        range.setForeground(Color.red);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, top to bottom orientation",
                new String[] { "Should see vertical pink range with red thumb at maximum position (bottom)" },
                "ShowLook_T2B_Pos_max", range);

        // Vertical, top to bottom orientation look tests...
        range.setOrientation(HOrientable.ORIENT_BOTTOM_TO_TOP);
        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(100);
        range.setBackground(Color.blue);
        range.setForeground(Color.pink);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, bottom to top orientation",
                new String[] { "Should see vertical blue range with pink thumb at maximum position (top)" },
                "ShowLook_B2T_Pos_max", range);

        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(50);
        range.setBackground(Color.yellow);
        range.setForeground(Color.green);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, bottom to top orientation",
                new String[] { "Should see vertical yellow range with green thumb at middle position" },
                "ShowLook_B2T_Pos_half", range);

        range.setBounds(0, 0, 30, 200);
        range.setThumbOffsets(3, 3);
        range.setValue(0);
        range.setBackground(Color.pink);
        range.setForeground(Color.red);
        range.setBackgroundMode(HVisible.BACKGROUND_FILL);
        doSnapshot("ShowLook: ->Vertical range, bottom to top orientation",
                new String[] { "Should see vertical pink range with red thumb at minimum position (bottom)" },
                "ShowLook_B2T_Pos_min", range);
    }
}
