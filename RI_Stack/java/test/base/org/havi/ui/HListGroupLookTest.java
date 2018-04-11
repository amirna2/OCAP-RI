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

/*
 * HListGroupLookTest.java
 *
 * Created on March 23, 2001, 3:22 PM
 */

package org.havi.ui;

import org.havi.ui.event.*;
import java.awt.*;

import org.cablelabs.test.*;
import junit.framework.*;
import org.cablelabs.gear.util.ImagePortfolio;
import org.cablelabs.gear.util.TextLines;
import org.cablelabs.gear.util.TextRender;

/**
 * Tests {@link #HListGroupLook}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.3 $, $Date: 2002/11/07 21:14:07 $
 */
public class HListGroupLookTest extends AbstractLookTest
{
    /**
     * Standard constructor.
     */
    public HListGroupLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HListGroupLookTest(String s, Object params)
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

        htestlook = new HListGroupLook();

        hlook = new HListGroupLook()
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

        // don't do automatic state transitions
        hvisible = new HListGroup()
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
        return AbstractLookTest.suite(HListGroupLookTest.class);
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
        // Unimplemented
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
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
        // Unimplemented
        return new Dimension(0, 0);
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
        fail("No such thing as state content");
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
        addContent((HListGroup) v);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component to test appropriate
     * scaling of the content. Default implementation calls
     * {@link #addSizedContent(HVisible)}.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addScaledContent(HVisible v)
    {
        v.setForeground(Color.red);
        v.setBackground(Color.white);

        HListGroup lg = (HListGroup) v;
        addContent(lg, 1, true);

        for (int i = 0; i < lg.getNumItems(); ++i)
            lg.getItem(i).setLabel(null);

        // We are only concerned with looking at one element
        Dimension min = lg.getMinimumSize();
        lg.setDefaultSize(min);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component to test appropriate
     * alignment of the content. Default implementation calls
     * {@link #addSizedContent(HVisible)}.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addAlignedContent(HVisible v)
    {
        HListGroup lg = (HListGroup) v;
        addContent(lg, 1, true);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component to test appropriate
     * background rendering of the component. Default implementation calls
     * {@link #addStateContent(HVisible)}.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addBGContent(HVisible v)
    {
        HListGroup lg = (HListGroup) v;
        addContent(lg, 1, true);
    }

    /**
     * Adds content. Sets up the list.
     */
    private void addContent(HListGroup lg, int n, boolean icon)
    {
        HListElement[] data = new HListElement[n];
        for (int i = 0; i < n; ++i)
        {
            data[i] = new HListElement("Element " + i + "\n" + i + "\nElement " + i);
            if (icon) data[i].setIcon(loadNumber(i));
        }
        lg.setListContent(data);
    }

    private void addContent(HListGroup lg, boolean icon)
    {
        addContent(lg, 10, icon);
    }

    private void addContent(HListGroup lg)
    {
        addContent(lg, true);
    }

    /**
     * Scales the component by the given ratio.
     */
    protected void scaleSize(HVisible hvisible, float xScale, float yScale)
    {
        HListGroup lg = (HListGroup) hvisible;

        if (xScale >= 1.0f) xScale *= 2;
        if (yScale >= 1.0f) yScale *= 2;

        // We wish to increase the size of the icon/label so that we
        // can more readily see alignment.
        Dimension iconSize = calcMaxIconSize(lg);
        iconSize.width = (int) (iconSize.width * xScale);
        iconSize.height = (int) (iconSize.height * yScale);
        lg.setIconSize(iconSize);

        Dimension labelSize = calcMaxLabelSize(lg);
        labelSize.width = (int) (labelSize.width * xScale);
        labelSize.height = (int) (labelSize.height * yScale);
        lg.setLabelSize(labelSize);

        // We are only concerned with looking at one element
        Dimension min = lg.getMinimumSize();
        lg.setDefaultSize(min);
    }

    /**
     * Calculates the largest icon size.
     */
    private Dimension calcMaxIconSize(HListGroup lg)
    {
        HListElement[] data = lg.getListContent();

        int maxW = 0, maxH = 0;
        if (data != null) for (int i = 0; i < data.length; ++i)
        {
            Image icon = data[i].getIcon();
            if (icon != null)
            {
                maxW = Math.max(maxW, icon.getWidth(null));
                maxH = Math.max(maxH, icon.getHeight(null));
            }
        }
        return new Dimension(maxW, maxH);
    }

    /**
     * Calculates the largest label size.
     */
    private Dimension calcMaxLabelSize(HListGroup lg)
    {
        HListElement[] data = lg.getListContent();
        Font f = lg.getFont();
        if (f == null)
        {
            f = new Font("Dialog", Font.PLAIN, 12);
            lg.setFont(f);
        }
        FontMetrics metrics = lg.getFontMetrics(f);

        int maxW = 0, maxH = 0, lineCount = 0;
        if (data != null)
        {
            for (int i = 0; i < data.length; ++i)
            {
                String label = data[i].getLabel();
                if (label != null)
                {
                    String[] lines = TextLines.getLines(label);
                    maxW = Math.max(maxW, TextLines.getMaxWidth(lines, metrics));
                    lineCount = Math.max(lineCount, lines.length);
                }
            }
            maxH = TextRender.getFontHeight(metrics) * lineCount;
        }
        return new Dimension(maxW, maxH);
    }

    /**
     * Loads an indexed image. Blocks until the image is fully loaded.
     */
    private Image loadNumber(int i)
    {
        return ImagePortfolio.loadImage(getClass().getResource("/images/numbers_0" + i + ".gif"));
    }

    /**
     * Used to determine if the look being tested supports scaling or not.
     * 
     * @return <code>true</cod> if scaling is supported;
     * <code>false</code> otherwise
     */
    protected boolean isScalingSupported()
    {
        // But not as far as sizing is concerned...
        return true;
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
     * Test default constructor.
     * <ul>
     * <li>HListGroupLook()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HListGroupLook()", (HListGroupLook) hlook);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HListGroupLook look)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", look);
    }

    /**
     * Should be overridden by subclasses to create an instance of HVisible
     * (appropriate for the given test) which overrides the repaint methods and
     * causes <code>repainted[0] = true</code>.
     */
    protected HVisible createRepaintVisible(final boolean[] repainted)
    {
        HListGroup lg = new HListGroup()
        {
            public void repaint()
            {
                repainted[0] = true;
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
        lg.setListContent(((HListGroup) hvisible).getListContent());
        return lg;
    }

    /**
     * Tests hitTest().
     * <ul>
     * <li>This is largely implementation dependent, in the least, ADJUST_NONE
     * should be returned.
     * </ul>
     */
    public void xtestHitTest()
    {
        /* !!!FINISH!!! */
        fail("Unimplemented test");
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
    public void xtestValue()
    {
        /* !!!FINISH!!! */
        fail("Unimplemented test");
    }

    /**
     * Tests showLook(). Tests icon/label size. Should try different
     * orientations.
     * <ul>
     * <li>If icon/label size is 0, it shouldn't be displayed
     * </ul>
     */
    public void testShowLookIconLabelSize()
    {
        HListGroup lg = (HListGroup) hvisible;
        addContent(lg, true);

        /* !!!FINISH!!! */
        // Should do other orientations!

        HScene testScene = getHScene();
        testScene.addNotify();
        Container c = new Container()
        {
        };
        c.setLayout(new org.cablelabs.gear.NullLayout());
        testScene.add(c);

        // NOTE:
        // We add lg to a container with a null layout so that
        // the minimum size of the frame does not affect the lg's size.
        // Otherwise, on the default vertical orientation,
        // the minimum width would affect us.

        try
        {
            c.add(lg);

            // lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            lg.setLabelSize(new Dimension(0, 0));
            lg.setIconSize(null);
            lg.setSize(lg.getPreferredSize());
            c.setSize(c.getPreferredSize());
            testScene.show();
            TestSupport.checkDisplay(lg, "iconSize=0", new String[] { "Are 5 elements displayed?",
                    "Are the icons displayed?", "The labels should NOT be displayed" }, "iconSize=0", this);

            lg.setIconSize(new Dimension(0, 0));
            lg.setLabelSize(null);
            lg.setSize(lg.getPreferredSize());
            lg.invalidate();
            c.setSize(c.getPreferredSize());
            testScene.show();
            TestSupport.checkDisplay(lg, "labelSize=0", new String[] { "Are 5 elements displayed?",
                    "Are the labels displayed?", "The icons should NOT be displayed" }, "labelSize=0", this);
        }
        finally
        {
            testScene.remove(c);
        }
    }

    /**
     * Helper method for testShowLookScrollPosition().
     */
    private void checkScrollPosition(HListGroup lg, int i)
    {
        lg.setScrollPosition(i);
        TestSupport.checkDisplay(lg, "ScrollPosition=" + i, new String[] { "Is element " + i + " the first visible?",
                "Are the elements following " + i + " correctly displayed?" }, "scroll" + i, this);
    }

    /**
     * Tests showLook(). Test that the correct element is scrollPosition. Should
     * try different orientations.
     */
    public void testShowLookScrollPosition()
    {
        HListGroup lg = (HListGroup) hvisible;
        addContent(lg, true);

        /* !!!FINISH!!! */
        // Should do other orientations!

        HScene testScene = getHScene();
        try
        {
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);

            testScene.add(lg);
            lg.setSize(lg.getPreferredSize());
            testScene.show();

            assertTrue("Test written to expect 10 elements", lg.getNumItems() == 10);
            checkScrollPosition(lg, 0);
            checkScrollPosition(lg, 1);
            checkScrollPosition(lg, 5);
            checkScrollPosition(lg, 6);
            checkScrollPosition(lg, 9);
        }
        finally
        {
            testScene.remove(lg);
        }
    }

    /**
     * Helper method for testShowLookCurrent().
     */
    private void checkCurrent(HListGroup lg, int i)
    {
        lg.setSelectionMode(true);
        lg.setCurrentItem(i);
        TestSupport.checkDisplay(lg, "Current=" + i, new String[] {
                "Is element " + i + " the marked as " + "the current?",
                "Are the visible elements displayed " + "correctly?" }, "curr" + i, this);
    }

    /**
     * Tests showLook(). Tests that the correct current item is visible as such.
     * Should try different orientations.
     */
    public void testShowLookCurrent()
    {
        HListGroup lg = (HListGroup) hvisible;
        addContent(lg, true);

        /* !!!FINISH!!! */
        // Should do other orientations!

        HScene testScene = getHScene();
        try
        {
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            testScene.add(lg);
            lg.setSize(lg.getPreferredSize());
            testScene.show();

            assertTrue("Test written to expect 10 elements", lg.getNumItems() == 10);
            checkCurrent(lg, 5);
            checkCurrent(lg, 6);
            checkCurrent(lg, 9);
            checkCurrent(lg, 0);
            checkCurrent(lg, 1);
        }
        finally
        {
            testScene.remove(lg);
        }
    }

    /**
     * Helper method for testShowLookSelection().
     */
    private void checkSelection(HListGroup lg, int i)
    {
        lg.setItemSelected(i, true);
        TestSupport.checkDisplay(lg, "Selected=" + i, new String[] { "Is one element selected?",
                "Is element " + i + " the only " + "selected element?",
                "Are the visible elements displayed " + "correctly?" }, "selected" + i, this);
    }

    /**
     * Helper method for testShowLookSelection().
     */
    private void checkMultiSelection(HListGroup lg, int i)
    {
        lg.setItemSelected(i, false);
        TestSupport.checkDisplay(lg, "MultiSelected=" + i, new String[] { "Are all elements selected but one?",
                "Is element " + i + " the only " + "UN-selected element?",
                "Are the visible elements displayed " + "correctly?" }, "mselected" + i, this);
        lg.setItemSelected(i, true);
    }

    /**
     * Tests showLook(). Tests that the correct selected items are visible as
     * such.
     */
    public void testShowLookSelection()
    {
        HListGroup lg = (HListGroup) hvisible;
        addContent(lg, true);

        /* !!!FINISH!!! */
        // Should do other orientations!

        HScene testScene = getHScene();
        testScene.addNotify();
        try
        {
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            lg.setDefaultSize(lg.getMaximumSize());
            testScene.add(lg);
            lg.setSize(lg.getPreferredSize());
            testScene.show();

            final int n = lg.getNumItems();

            lg.setMultiSelection(false);
            assertTrue("Test written to expect 10 elements", lg.getNumItems() == 10);
            checkSelection(lg, 5);
            checkSelection(lg, 6);
            checkSelection(lg, 9);
            checkSelection(lg, 0);
            checkSelection(lg, 1);

            lg.clearSelection();
            lg.setMultiSelection(true);
            for (int i = 0; i < n; ++i)
                lg.setItemSelected(i, true);

            checkMultiSelection(lg, 1);
            checkMultiSelection(lg, 0);
            checkMultiSelection(lg, 5);
            checkMultiSelection(lg, 9);
            checkMultiSelection(lg, 6);
        }
        finally
        {
            testScene.remove(lg);
        }
    }

    private static final int[] orient = { HOrientable.ORIENT_BOTTOM_TO_TOP, HOrientable.ORIENT_TOP_TO_BOTTOM,
            HOrientable.ORIENT_LEFT_TO_RIGHT, HOrientable.ORIENT_RIGHT_TO_LEFT, };

    private static final String[] orientNames = { "BOTTOM_TO_TOP", "TOP_TO_BOTTOM", "LEFT_TO_RIGHT", "RIGHT_TO_LEFT", };

    /**
     * Tests showLook(). Should test with/without label/icon. Tests that all
     * orientations are drawn correctly.
     */
    public void testShowLookOrientation()
    {
        HListGroup lg = (HListGroup) hvisible;
        addContent(lg, true);

        HScene testScene = getHScene();
        try
        {
            testScene.add(lg);

            final int n = orient.length;
            for (int i = 0; i < n; ++i)
            {
                lg.setOrientation(orient[i]);
                lg.invalidate();
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, orientNames[i], new String[] { "Are 5 elements visible",
                        "Are the elements oriented: " + orientNames[i] + "?", "Is element 0 the first visible?", },
                        orientNames[i], this);
            }
        }
        finally
        {
            testScene.remove(lg);
        }
    }

    /**
     * Tests getElementInsets().
     */
    public void xtestElementInsets()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getPreferredSize().
     * <ul>
     * <li>the preferred size is that set by setDefaultSize rounded down to the
     * nearest element (minimum of one) or the size required to present 5
     * elements if a default size is not set.
     * </ul>
     */
    public void testPreferredSize() throws Exception
    {
        if (SIZE_STANDARD)
            super.testPreferredSize();
        else
        {
            /* !!!FINISH!!! */
            // Should do other orientations!

            HListGroup lg = (HListGroup) hvisible;
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            lg.setLook(hlook);

            HScene testScene = getHScene();
            testScene.addNotify();
            try
            {
                testScene.add(lg);

                addContent(lg, true);

                // Icon and Label
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "prefIconLabel", new String[] { "Are 5 elements visible?",
                        "Are icons visible?", "Are labels visible?", }, "prefIconLabel", this);

                // Icon-only
                lg.setLabelSize(new Dimension(0, 0));
                lg.invalidate();
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "prefIcon", new String[] { "Are 5 elements visible?",
                        "Elements should be sized to " + "display ONLY icons" }, "prefIcon", this);

                // Label-only
                lg.setIconSize(new Dimension(0, 0));
                lg.setLabelSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                lg.invalidate();
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "prefLabel", new String[] { "Are 5 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "prefLabel", this);

                // Label-only (icon=null)
                lg.removeAllItems();
                addContent(lg, false);
                lg.setIconSize(new Dimension(lg.DEFAULT_ICON_WIDTH, lg.DEFAULT_ICON_HEIGHT));
                lg.invalidate();
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "prefLabel2", new String[] { "Are 5 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "prefLabel2", this);

                // Default size... (restore icon/label content)
                lg.removeAllItems();
                addContent(lg, true);

                // Icon and Label (default size)
                showThreePlus(lg);
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "defIconLabel", new String[] { "Are exactly 3 elements visible?",
                        "Are icons visible?", "Are labels visible?", }, "defIconLabel", this);

                // Icon-only (default size)
                lg.setLabelSize(new Dimension(0, 0));
                showThreePlus(lg);
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "defIcon", new String[] { "Are exactly 3 elements visible?",
                        "Elements should be sized to " + "display ONLY icons" }, "defIcon", this);

                // Label-only (default size)
                lg.setIconSize(new Dimension(0, 0));
                lg.setLabelSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                showThreePlus(lg);
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "defLabel", new String[] { "Are exactly 3 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "defLabel", this);

                // Label-only (icon=null) (default size)
                lg.removeAllItems();
                addContent(lg, false);
                lg.setIconSize(new Dimension(lg.DEFAULT_ICON_WIDTH, lg.DEFAULT_ICON_HEIGHT));
                showThreePlus(lg);
                lg.setSize(lg.getPreferredSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "defLabel2", new String[] { "Are exactly 3 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "defLabel2", this);
            }
            finally
            {
                testScene.remove(lg);
            }
        }
    }

    /**
     * Sets the default size to something over 3 - should only show 3.
     */
    private void showThreePlus(HListGroup lg)
    {
        Dimension d = lg.getMinimumSize();
        d.width = (int) (d.width * 3.4f);
        d.height = (int) (d.height * 3.4f);
        lg.setDefaultSize(d);
        lg.invalidate();
    }

    /**
     * Tests getMinimumSize().
     * <ul>
     * <li>the minimum size is the size to present one element or an
     * implementation specific minimum (32 x 32 for example) if no elements are
     * present.
     * <li>calculation of this should be in HaviTestToolkit (?), or at least the
     * calculation of one element's size and that of the impl-specific minimum
     * (?)
     * </ul>
     */
    public void testMinimumSize() throws Exception
    {
        if (SIZE_STANDARD)
            super.testMinimumSize();
        else
        {
            /* !!!FINISH!!! */
            // Should do other orientations!

            HListGroup lg = (HListGroup) hvisible;
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            lg.setLook(hlook);

            /*
             * assertEquals("With no content, minimumSize should be same as "+
             * "maximumSize", hlook.getMaximumSize(lg),
             * hlook.getMinimumSize(lg));
             */

            HScene testScene = getHScene();
            testScene.addNotify();
            Container c = new Container()
            {
            };
            c.setLayout(new org.cablelabs.gear.NullLayout());
            testScene.add(c);
            try
            {
                c.add(lg);
                addContent(lg, true);

                // Icon and Label
                lg.setSize(lg.getMinimumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "minIconLabel", new String[] { "Is only 1 element visible?",
                        "Are icons visible?", "Are labels visible?", }, "minIconLabel", this);

                // Icon-only
                lg.setLabelSize(new Dimension(0, 0));
                lg.setSize(lg.getMinimumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "minIcon", new String[] { "Is only 1 element visible?",
                        "Elements should be sized to " + "display ONLY icons" }, "minIcon", this);

                // Label-only
                lg.setIconSize(new Dimension(0, 0));
                lg.setLabelSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                lg.setSize(lg.getMinimumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "minLabel", new String[] { "Is only 1 element visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "minLabel", this);

                // Label-only (icon=null)
                lg.removeAllItems();
                addContent(lg, false);
                lg.setIconSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                lg.setSize(lg.getMinimumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "minLabel2", new String[] { "Is only 1 element visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "minLabel2", this);
            }
            finally
            {
                testScene.remove(c);
            }
        }
    }

    /**
     * Tests getMaximumSize().
     * <ul>
     * <li>the maximum size is that required to present all elements.
     * <li>icon only (label default=(0,0))
     * <li>label only (no icon)
     * <li>label only (icon default=(0,0))
     * <li>icon and label
     * </ul>
     */
    public void testMaximumSize() throws Exception
    {
        if (SIZE_STANDARD)
            super.testMaximumSize();
        else
        {
            /* !!!FINISH!!! */
            // Should do other orientations!

            HListGroup lg = (HListGroup) hvisible;
            lg.setOrientation(lg.ORIENT_LEFT_TO_RIGHT);
            lg.setLook(hlook);

            /*
             * assertEquals("With no content, maximumSize should be same as "+
             * "minimumSize", hlook.getMinimumSize(lg),
             * hlook.getMaximumSize(lg));
             */

            HScene testScene = getHScene();
            testScene.addNotify();
            Container c = new Container()
            {
            };
            c.setLayout(new org.cablelabs.gear.NullLayout());
            testScene.add(c);
            try
            {
                c.add(lg);
                addContent(lg, true);

                // Icon and Label
                lg.setSize(lg.getMaximumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "maxIconLabel", new String[] { "Are 10 elements visible?",
                        "Are icons visible?", "Are labels visible?", }, "maxIconLabel", this);

                // Icon-only
                lg.setLabelSize(new Dimension(0, 0));
                lg.setSize(lg.getMaximumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "maxIcon", new String[] { "Are 10 elements visible?",
                        "Elements should be sized to " + "display ONLY icons" }, "maxIcon", this);

                // Label-only
                lg.setIconSize(new Dimension(0, 0));
                lg.setLabelSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                lg.setSize(lg.getMaximumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "maxLabel", new String[] { "Are 10 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "maxLabel", this);

                // Label-only (icon=null)
                lg.removeAllItems();
                addContent(lg, false);
                lg.setIconSize(new Dimension(lg.DEFAULT_LABEL_WIDTH, lg.DEFAULT_LABEL_HEIGHT));
                lg.setSize(lg.getMaximumSize());
                testScene.show();
                TestSupport.checkDisplay(lg, "maxLabel2", new String[] { "Are 10 elements visible?",
                        "Elements should be sized to " + "display ONLY labels" }, "maxLabel2", this);
            }
            finally
            {
                testScene.remove(c);
            }
        }
    }

    /**
     * Whether to test sizing based on the description in HListGroup or provided
     * in HLook/HListGroupLook. If true, then test the standard sizing method.
     */
    private static final boolean SIZE_STANDARD = false;
}
