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
import java.awt.*;
import java.lang.reflect.*;

/**
 * Tests {@link #HDefaultTextLayoutManager}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.5 $, $Date: 2002/11/07 21:14:06 $
 */
public class HDefaultTextLayoutManagerTest extends GUITest
{
    /**
     * Standard constructor.
     */
    public HDefaultTextLayoutManagerTest(String str)
    {
        super(str);
    }

    /**
     * Parameterized contructor.
     */
    public HDefaultTextLayoutManagerTest(String str, Object params)
    {
        super(str);
        this.params = params;
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(HDefaultTextLayoutManagerTest.class));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Parameter(s) passed to parameterized tests.
     */
    protected Object params;

    /**
     * Defines a TestSuite.
     */
    public static TestSuite suite(Class testClass) throws Exception
    {
        TestSuite suite = TestUtils.suite(testClass);

        // Now add our other tests.
        Constructor c = testClass.getConstructor(new Class[] { String.class, // name
                Object.class // params
        });

        // Alignment tests
        for (int h = 0; h < hAlignConst.length; ++h)
        {
            for (int v = 0; v < vAlignConst.length; ++v)
            {
                TestCase tc = (TestCase) c.newInstance(new Object[] { "xTestRender", new int[] { h, v } });
                suite.addTest(tc);
            }
        }

        // Ellipsis tests
        for (int h = 0; h < hAlignConst.length; ++h)
        {
            TestCase tc = (TestCase) c.newInstance(new Object[] { "xTestEllipsis", new Integer(h) });
            suite.addTest(tc);
        }

        return suite;
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>implements HTextLayoutManager
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testImplements(HDefaultTextLayoutManager.class, HTextLayoutManager.class);
    }

    /**
     * Test the 2 constructors of HVisible.
     * <ul>
     * <li>HDefaultTextLayoutManager()
     * <li>HDefaultTextLayoutManager(int hAlign, int vAlign)
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HDefaultTextLayoutManager()", new HDefaultTextLayoutManager());
    }

    /**
     * Checks for proper initialization of the object.
     */
    private void checkConstructor(String msg, HDefaultTextLayoutManager tlm)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", tlm);
    }

    /**
     * Tests getMaximumSize().
     * <ul>
     * <li>Maximum width and height of all content in all states should be
     * returned.
     * <li>Unless Short.MAX_VALUE is returned (which is allowed).
     * </ul>
     */
    public void xtestMaximumSize()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getPreferredSize().
     * <ul>
     * <li>the maximum width and maximum height of all preferred sizes are
     * returned
     * </ul>
     */
    public void xtestPreferredSize()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getMinimumSize().
     * <ul>
     * <li>the maximum width and maximum height of all minimum sizes are
     * returned
     * </ul>
     */
    public void xtestMinimumSize()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests fields.
     * <ul>
     * <li>for any exposed non-final fields
     * <li>for any added (unexpected) public constant fields.
     * </ul>
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(HDefaultTextLayoutManager.class);
        TestUtils.testNoAddedFields(HDefaultTextLayoutManager.class, null);
    }

    private static final int[] hAlignConst = new int[] { HVisible.HALIGN_LEFT, HVisible.HALIGN_CENTER,
            HVisible.HALIGN_RIGHT, HVisible.HALIGN_JUSTIFY, };

    private static final int[] vAlignConst = new int[] { HVisible.VALIGN_TOP, HVisible.VALIGN_CENTER,
            HVisible.VALIGN_BOTTOM, HVisible.VALIGN_JUSTIFY, };

    private static final String[] hAlignName = new String[] { "LEFT", "CENTER", "RIGHT", "JUSTIFY", };

    private static final String[] vAlignName = new String[] { "TOP", "CENTER", "BOTTOM", "JUSTIFY", };

    /**
     * Tests render().
     * <ul>
     * <li>Test proper color of text
     * <li>Test proper font of text
     * <li>Test all 16 combinations of text rendering
     * <li>Test replacement of missing characters with '!' (???)
     * <li>Test multiline markup
     * </ul>
     */
    public void xTestRender() throws Exception
    {
        HScene testScene = getHScene();

        HDefaultTextLayoutManager tlm = new HDefaultTextLayoutManager();
        HVisible vis = new HVisible();

        final int hAlignIndex = ((int[]) params)[0];
        final int vAlignIndex = ((int[]) params)[1];

        int hAlign = hAlignConst[hAlignIndex];
        int vAlign = vAlignConst[vAlignIndex];

        final Insets insets = new Insets(30, 4, 4, 30);

        vis.setHorizontalAlignment(hAlign);
        vis.setVerticalAlignment(vAlign);

        vis.setTextLayoutManager(tlm);
        vis.setDefaultSize(new Dimension(200, 200));
        vis.setForeground(Color.red);
        vis.setLook(new HTextLook()
        {
            public void showLook(Graphics g, HVisible visible, int state)
            {
                String content = visible.getTextContent(state);
                HTextLayoutManager text = visible.getTextLayoutManager();

                // Draw the background
                g.setColor(Color.white);
                g.fillRect(0, 0, (int) visible.getSize().width, (int) visible.getSize().height);

                // Draw the inner bounds area
                Insets insets = getInsets(visible);
                g.setColor(Color.lightGray);
                g.fillRect(insets.left, insets.right, visible.getSize().width - insets.left - insets.right,
                        visible.getSize().height - insets.top - insets.bottom);

                // Draw the text
                text.render(content, g, visible, getInsets(visible));
            }

            public Insets getInsets(HVisible visible)
            {
                return insets;
            }
        });

        vis.setTextContent("Hello, world!\n" + "Hola, mundo!\n" + "Bonjour, monde!\n" + "Hallo, Welt!\n"
                + "Ciao, mondo!\n", HState.ALL_STATES);

        testScene.add(vis);

        try
        {
            vis.setSize(vis.getPreferredSize());
            testScene.show();
            String name = hAlignName[hAlignIndex] + "_" + vAlignName[vAlignIndex];
            TestSupport.checkDisplay(vis, name, new String[] { "Is the foreground text red?",
                    "Is the text displayed strictly within the lightGray box?",
                    "Is the text aligned horizontally: " + hAlignName[hAlignIndex] + "?",
                    "Is the text aligned vertically: " + vAlignName[vAlignIndex] + "?",
                    "Are insets " + insets + " used for the white background?",
                    "Is 'Hello, world!' displayed on line 1?", "Is 'Hola, mundo!' displayed on line 2?",
                    "Is 'Bonjour, monde!' displayed on line 3?", "Is 'Hallo, Welt!' displayed on line 4?",
                    "Is 'Ciao, mondo!' displayed on line 5?", }, "render_" + name, this);
        }
        finally
        {
            testScene.remove(vis);
        }
    }

    /**
     * Tests render(). Truncated strings should display an ellipsis (...).
     */
    public void xTestEllipsis() throws Exception
    {
        HScene testScene = getHScene();
        HDefaultTextLayoutManager tlm = new HDefaultTextLayoutManager();
        HVisible vis = new HVisible();

        final int hAlignIndex = ((Integer) params).intValue();
        int hAlign = hAlignConst[hAlignIndex];
        vis.setHorizontalAlignment(hAlign);

        vis.setTextLayoutManager(tlm);
        vis.setLook(new HTextLook());
        vis.setDefaultSize(new Dimension(50, 50));
        StringBuffer longText = new StringBuffer("Really, ");
        for (int i = 0; i < 100; ++i)
            longText.append("really, ");
        longText.append("long text!");
        vis.setTextContent(longText.toString(), vis.ALL_STATES);

        testScene.add(vis);
        try
        {
            vis.setSize(vis.getPreferredSize());
            testScene.show();
            String name = hAlignName[hAlignIndex];
            TestSupport.checkDisplay(vis, name, new String[] {
                    "Is only one line of text displayed?" + " [" + name + "]",
                    "Is the text truncated with an ellipsis (\"...\")?" + " [" + name + "]" }, "ellipsis_" + name, this);
        }
        finally
        {
            testScene.remove(vis);
        }
    }
}
