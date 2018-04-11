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

import java.awt.*;
import java.lang.reflect.*;
import org.cablelabs.test.*;
import junit.framework.*;

/**
 * Abstract class used as a base class for testing looks.
 */
public abstract class AbstractLookTest extends GUITest
{
    /**
     * Standard constructor.
     */
    public AbstractLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public AbstractLookTest(String s, Object params)
    {
        super(s);
        this.params = params;
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(AbstractLookTest.class));
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
                TestCase tc = (TestCase) c.newInstance(new Object[] { "xTestShowLookAlignment", new int[] { h, v } });
                suite.addTest(tc);
            }
        }

        // Small Alignment tests
        for (int h = 0; h < hAlignConst.length; ++h)
        {
            for (int v = 0; v < vAlignConst.length; ++v)
            {
                TestCase tc = (TestCase) c.newInstance(new Object[] { "xTestShowLookAlignmentSmall", new int[] { h, v } });
                suite.addTest(tc);
            }
        }

        // State-specific content tests
        for (int s = 0; s < 8; ++s)
            suite.addTest((TestCase) c.newInstance(new Object[] { "xTestShowLookStates",
                    new Integer(s | HVisible.NORMAL_STATE) }));

        return suite;
    }

    /**
     * Variables used to test the calling order of an HExtendedLook
     */
    protected int callIndex = 1;

    protected int backgroundCall = 0;

    protected int borderCall = 0;

    protected int visibleCall = 0;

    private void reset()
    {
        callIndex = 1;
        backgroundCall = 0;
        borderCall = 0;
        visibleCall = 0;
    }

    /**
     * Look with which to test ancestry.
     */
    protected HLook htestlook;

    /**
     * Look to be tested.
     */
    protected HLook hlook;

    /**
     * HVisible that will have the look assigned to it.
     */
    protected HVisible hvisible;

    /**
     * A font to be used with each HVisible.
     */
    protected Font font;

    /**
     * Setup. The variables <code>hlook</code> and <code>hvisible</code> should
     * be set up here.
     * <p>
     * It is expected that any fonts or colors be setup for hvisible as well.
     * Any backgrounds that components will be displayed on will be lightGray.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        font = new Font("dialog", Font.PLAIN, 16);
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Calculates the maximum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the maximum dimensions of the content in question
     */
    protected abstract Dimension getContentMaxSize(HVisible v);

    /**
     * Calculates the minimum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the minimum dimensions of the content in question
     */
    protected abstract Dimension getContentMinSize(HVisible v);

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component to test appropriate
     * sizing of the component.
     * 
     * @param v
     *            the component to add content to
     */
    protected abstract void addSizedContent(HVisible v);

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
        addSizedContent(v);
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
        addSizedContent(v);
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
        addScaledContent(v);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component.
     * 
     * @param v
     *            the component to add content to
     */
    protected abstract void addStateContent(HVisible v);

    /**
     * Used to determine if the look being tested supports scaling or not.
     * 
     * @return <code>true</cod> if scaling is supported;
     * <code>false</code> otherwise
     */
    protected abstract boolean isScalingSupported();

    /**
     * Used to determine if the look uses any of the content from the HVisible.
     * 
     * @return <code>true</code> if the look uses content; <code>false</code>
     *         otherwise
     */
    protected boolean usesContent()
    {
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
        return true;
    }

    /**
     * Adds the given insets the width/height of the given dimension and returns
     * the new dimension.
     * 
     * @param d
     *            original dimension
     * @param insets
     *            the insets to add
     * @return a new dimension that is the sum of the original and the given
     *         insets
     */
    protected Dimension addInsets(Dimension d, Insets insets)
    {
        return new Dimension(d.width + insets.left + insets.right, d.height + insets.top + insets.bottom);
    }

    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HExtendedLook
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testImplements(htestlook.getClass(), HExtendedLook.class);
    }

    /**
     * Tests the constructors of this HLook class.
     */
    public abstract void testConstructors();

    /**
     * Tests the fields of this look:
     * <ul>
     * <li>No public non-static non-final fields.
     * <li>No added public fields
     * </ul>
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(hlook.getClass());
        TestUtils.testNoAddedFields(hlook.getClass(), new String[0]);
    }

    /**
     * Tests getInsets().
     * <ul>
     * <li>should return a non-null object
     * <li>offsets should be >= 0 (??)
     * </ul>
     */
    public void testInsets() throws Exception
    {
        hvisible.setLook(hlook);

        assertNotNull("getInsets() unexpectedly returned null", hlook.getInsets(hvisible));
        TestSupport.assertGreaterEqual("Insets are expected to be >= 0", hlook.getInsets(hvisible), new Insets(0, 0, 0,
                0));
    }

    /**
     * Tests isOpaque().
     * <ul>
     * <li>This value is implementation-dependent and in general cannot be
     * tested
     * <li>Ensure that if HVisible.getBackgroundMode()==NO_BACKGROUND_FILL,
     * isOpaque() returns false
     * </ul>
     */
    public void testOpaque() throws Exception
    {
        hvisible.setLook(hlook);
        hvisible.setBackgroundMode(HVisible.NO_BACKGROUND_FILL);
        assertTrue("In NO_BACKGROUND_FILL mode, isOpaque() should return false", !hlook.isOpaque(hvisible));
    }

    /**
     * Should be overridden by subclasses to create an instance of HVisible
     * (appropriate for the given test) which overrides the repaint methods and
     * causes <code>repainted[0] = true</code>.
     */
    protected HVisible createRepaintVisible(final boolean[] repainted)
    {
        return new HVisible()
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
    }

    /**
     * Tests HExtended look
     * 
     * @throws Exception
     */
    public void testExtendedLook() throws Exception
    {
        hvisible.setLook(hlook);
        reset();

        HScene testScene = getHScene();
        testScene.setBackground(Color.black);
        testScene.add(hvisible);

        hlook.showLook(testScene.getGraphics(), hvisible, HState.NORMAL_STATE);

        assertEquals("'fillBackground' should be 1st method called by 'showLook'", 1, backgroundCall);
        assertEquals("'renderVisible' should be 2nd method called by 'showLook'", 2, visibleCall);
        assertEquals("'renderBorder' should be 3rd method called by 'showLook'", 3, borderCall);
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
        boolean[] repainted = new boolean[] { false };
        HVisible v = hvisible = createRepaintVisible(repainted);
        v.setLook(hlook);

        HChangeData[] data = new HChangeData[] { new HChangeData(HVisible.UNKNOWN_CHANGE, new Integer(
                HVisible.UNKNOWN_CHANGE)), };

        repainted[0] = false;
        hlook.widgetChanged(v, data);
        assertTrue("The widget should've been repainted", repainted[0]);
    }

    /**
     * Tests getMinimumSize() for a component.
     * <ol>
     * <li>scaled content
     * <li>unscaled content
     * <li>no content, defaultSize
     * <li>no content or defaultSize
     * </ol>
     * More specifically:
     * <ol>
     * <li>If the HLook is an HTextLook and HVisible.getTextLayoutManager()
     * returns an HDefaultTextLayoutManager, then its equivalent method should
     * be used + insets.
     * <li>If the HLook supports the scaling of its content and content is set
     * then the return value is the size of the smallest piece of content plus
     * the insets.
     * <li>If the HLook does not support scaling of content or no scaling is
     * requested, and content is set then the return value is the size of the
     * largest piece of content plus the insets.
     * <li>If no content is available but a default preferred size has been set
     * using setDefaultSize, then getDefaultSize plus the insets is returned.
     * <li>If there is no content or default size set then the return value is
     * an implementation-specific minimum size plus the insets.
     * </ol>
     */
    public void testMinimumSize() throws Exception
    {
        final int[] called = new int[1];
        final Dimension dummy = new Dimension(333, 334);
        HTextLayoutManager tlm;
        if (hlook instanceof HTextLook)
            tlm = new EmptyTextLayout(); // Don't delegate yet
        else
            tlm = new EmptyDefaultTextLayout(called, dummy); // Shouldn't
                                                             // delegate

        hvisible.setLook(hlook);
        hvisible.setTextLayoutManager(tlm);
        hvisible.setSize(new Dimension(227, 111)); // shouldn't matter

        // no content/default size
        // implementation-specific (just check for >= Insets)
        called[0] = 0;
        Insets insets = hlook.getInsets(hvisible);
        TestSupport.assertGreaterEqual("Invalid min size for no content/defaultSize", hlook.getMinimumSize(hvisible),
                new Dimension(insets.left + insets.right, insets.top + insets.bottom));
        assertEquals("Should not delegate to HDefaultTextLayoutManger ", 0, called[0]);

        // no content, has a default size
        called[0] = 0;
        hvisible.setDefaultSize(new Dimension(123, 127));
        assertEquals("Invalid min size given a defaultSize", addInsets(hvisible.getDefaultSize(),
                hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManger ", 0, called[0]);

        // unscaled content
        addSizedContent(hvisible);
        if (!isScalingSupported()) hvisible.setResizeMode(HVisible.RESIZE_PRESERVE_ASPECT);

        called[0] = 0;
        if (usesContent())
            assertEquals("Invalid min size for unscaled content", addInsets(getContentMaxSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        else
            assertEquals("Invalid min size for (unsupported) unscaled content", addInsets(hvisible.getDefaultSize(),
                    hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManger ", 0, called[0]);

        // scaled content
        called[0] = 0;
        hvisible.setResizeMode(HVisible.RESIZE_ARBITRARY);
        if (isScalingSupported())
            assertEquals("Invalid min size for scaled content", addInsets(getContentMinSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        else if (!usesContent())
            assertEquals("Invalid min size (unsupported) scaled content", addInsets(hvisible.getDefaultSize(),
                    hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        else
            assertEquals("Invalid min size for (unsupported) scaled content", addInsets(getContentMaxSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getMinimumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManger ", 0, called[0]);

        // HTextLook/HDefaultTextLayoutManager
        hvisible.setTextLayoutManager(new EmptyDefaultTextLayout(called, dummy));
        called[0] = 0;
        Dimension size = hlook.getMinimumSize(hvisible);
        assertEquals("getMinimumSize should delegate to HDefaultTextLayoutManager?", hlook instanceof HTextLook,
                called[0] != 0);
        // If delegated, should add insets
        if (hlook instanceof HTextLook)
        {
            assertEquals("getMinimumSize should add insets to " + "HDefaultTextLayoutManager.getMinimumSize()",
                    addInsets(dummy, hlook.getInsets(hvisible)), size);
        }
    }

    /**
     * Tests getMaximumSize() for a component.
     * <ol>
     * <li>HTextLook/HDefaultTextLayoutManager
     * <li>scaled content
     * <li>unscaled content
     * <li>no content
     * </ol>
     * More specifically:
     * <ol>
     * <li>If the HLook is an HTextLook and HVisible.getTextLayoutManager()
     * returns an HDefaultTextLayoutManager, then its equivalent method should
     * be used + insets.
     * <li>If the HLook supports the scaling of its content, then the return
     * value is the current size of the HVisible.
     * <li>If the HLook does not support scaling of content or no scaling is
     * requested, and content is set then the return value is the size of the
     * largest piece of content plus insets.
     * <li>If there is no content set then a maximum size of [Short.MAX_VALUE,
     * Short.MAX_VALUE] is returned as a Dimension.
     * </ol>
     */
    public void testMaximumSize() throws Exception
    {
        final int[] called = new int[1];
        final Dimension dummy = new Dimension(333, 334);
        HTextLayoutManager tlm;
        if (hlook instanceof HTextLook)
            tlm = new EmptyTextLayout(); // Don't delegate yet
        else
            tlm = new EmptyDefaultTextLayout(called, dummy); // Shouldn't
                                                             // delegate

        hvisible.setLook(hlook);
        hvisible.setTextLayoutManager(tlm);

        // Set default size (should not be used)
        hvisible.setDefaultSize(new Dimension(123, 124));
        // Set size (only should be used for scaled content)
        hvisible.setSize(new Dimension(103, 104));

        // no content
        called[0] = 0;
        assertEquals("Invalid max size for no content", new Dimension(Short.MAX_VALUE, Short.MAX_VALUE),
                hlook.getMaximumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        // unscaled content
        addSizedContent(hvisible);
        if (!isScalingSupported()) hvisible.setResizeMode(HVisible.RESIZE_PRESERVE_ASPECT);

        called[0] = 0;
        if (usesContent())
            assertEquals("Invalid max size for unscaled content", addInsets(getContentMaxSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getMaximumSize(hvisible));
        else
            assertEquals("Invalid max size for (unsupported) unscaled content", new Dimension(Short.MAX_VALUE,
                    Short.MAX_VALUE), hlook.getMaximumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        // scaled content
        called[0] = 0;
        hvisible.setResizeMode(HVisible.RESIZE_ARBITRARY);
        if (isScalingSupported())
            assertEquals("Invalid max size for scaled content", hvisible.getSize(), hlook.getMaximumSize(hvisible));
        else if (!usesContent())
            assertEquals("Invalid max size for (unsupported) scaled content", new Dimension(Short.MAX_VALUE,
                    Short.MAX_VALUE), hlook.getMaximumSize(hvisible));
        else
            assertEquals("Invalid max size for (unsupported) scaled content", addInsets(getContentMaxSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getMaximumSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        // HTextLook/HDefaultTextLayoutManager
        hvisible.setTextLayoutManager(new EmptyDefaultTextLayout(called, dummy));
        called[0] = 0;
        Dimension size = hlook.getMaximumSize(hvisible);
        assertEquals("getMaximumSize should delegate to HDefaultTextLayoutManager?", hlook instanceof HTextLook,
                called[0] != 0);
        // If delegated, should add insets
        if (hlook instanceof HTextLook)
        {
            assertEquals("getMaximumSize should add insets to " + "HDefaultTextLayoutManager.getMaximumSize()",
                    addInsets(dummy, hlook.getInsets(hvisible)), size);
        }
    }

    /**
     * Tests getPreferredSize() for a component.
     * <ol>
     * <li>defaultSize
     * <li>HTextLook/HDefaultTextLayoutManager
     * <li>unscaled content
     * <li>scaled content
     * <li>no content or defaultSize
     * </ol>
     * More specifically:
     * <ol>
     * <li>If a default preferred size has been set for this HVisible, then the
     * return value is the defualtSize plus insets.
     * <li>If the HLook is an HTextLook and HVisible.getTextLayoutManager()
     * returns an HDefaultTextLayoutManager, then its equivalent method should
     * be used + insets.
     * <li>If this HLook does not support scaling of content or no scaling is
     * requested, and content is present then the return value is the size of
     * the largest piece of content plus insets.
     * <li>If this HLook supports the scaling of its content and content is set
     * then the return value is the current size of the HVisible.
     * <li>If there is no content and no default size set then the return value
     * is the current size of the HVisible.
     * </ol>
     */
    public void testPreferredSize() throws Exception
    {
        final int[] called = new int[1];
        final Dimension dummy = new Dimension(333, 334);
        HTextLayoutManager tlm;
        if (hlook instanceof HTextLook)
            tlm = new EmptyTextLayout(); // Don't delegate yet
        else
            tlm = new EmptyDefaultTextLayout(called, dummy); // Shouldn't
                                                             // delegate

        hvisible.setLook(hlook);
        hvisible.setSize(103, 104);
        hvisible.setTextLayoutManager(tlm);

        // No content, no default size
        called[0] = 0;
        assertEquals("Invalid preferred size given no content/defaultSize", hvisible.getSize(),
                hlook.getPreferredSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        addSizedContent(hvisible);

        // Unscaled content
        if (!isScalingSupported()) hvisible.setResizeMode(HVisible.RESIZE_PRESERVE_ASPECT);

        hvisible.setDefaultSize(HVisible.NO_DEFAULT_SIZE);

        called[0] = 0;
        if (usesContent())
            assertEquals("Invalid preferred size for unscaled content", addInsets(getContentMaxSize(hvisible),
                    hlook.getInsets(hvisible)), hlook.getPreferredSize(hvisible));
        else
            assertEquals("Invalid preferred size for (unsupported) unscaled content", hvisible.getSize(),
                    hlook.getPreferredSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        // Scaled content
        called[0] = 0;
        hvisible.setResizeMode(HVisible.RESIZE_ARBITRARY);
        if (isScalingSupported())
            assertEquals("Invalid preferred size for scaled content", hvisible.getSize(),
                    hlook.getPreferredSize(hvisible));
        else if (!usesContent())
            assertEquals("Invalid preferred size for (unsupported) scaled content", hvisible.getSize(),
                    hlook.getPreferredSize(hvisible));
        else
            assertEquals("Invalid preferred size for (unsupported) scaled content", addInsets(
                    getContentMaxSize(hvisible), hlook.getInsets(hvisible)), hlook.getPreferredSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);

        // HTextLook/HDefaultTextLayoutManager
        hvisible.setTextLayoutManager(new EmptyDefaultTextLayout(called, dummy));
        called[0] = 0;
        Dimension size = hlook.getPreferredSize(hvisible);
        assertEquals("getPreferredSize should delegate to HDefaultTextLayoutManager?", hlook instanceof HTextLook,
                called[0] != 0);
        // If delegated, should add insets
        if (hlook instanceof HTextLook)
        {
            assertEquals("getPreferredSize should add insets to " + "HDefaultTextLayoutManager.getPreferredSize()",
                    addInsets(dummy, hlook.getInsets(hvisible)), size);
        }

        // Default Size (1st choice regardless of content, scaling, or
        // HTextLook/HDefaultTLM)
        Dimension defaultSize = new Dimension(123, 124);
        hvisible.setDefaultSize(defaultSize);
        called[0] = 0;
        assertEquals("Invalid preferred size given defaultSize", addInsets(hvisible.getDefaultSize(),
                hlook.getInsets(hvisible)), hlook.getPreferredSize(hvisible));
        assertEquals("Should not delegate to HDefaultTextLayoutManager ", 0, called[0]);
    }

    private static final int[] hAlignConst = new int[] { HVisible.HALIGN_LEFT, HVisible.HALIGN_CENTER,
            HVisible.HALIGN_RIGHT, HVisible.HALIGN_JUSTIFY, };

    private static final int[] vAlignConst = new int[] { HVisible.VALIGN_TOP, HVisible.VALIGN_CENTER,
            HVisible.VALIGN_BOTTOM, HVisible.VALIGN_JUSTIFY, };

    private static final String[] hAlignName = new String[] { "LEFT", "CENTER", "RIGHT", "JUSTIFY", };

    private static final String[] vAlignName = new String[] { "TOP", "CENTER", "BOTTOM", "JUSTIFY", };

    /**
     * Tests the proper rendering of aligned content.
     * 
     * @param scaleName
     *            the name of the scaling
     * @param xScale
     *            how to scale the component width
     * @param yScale
     *            how to scale the component height
     */
    public void xTestShowLookAlignment() throws Exception
    {
        xTestShowLookAlignmentSized("", 2.0f, 2.0f);
    }

    /**
     * Tests the proper rendering of aligned content.
     * 
     * @param scaleName
     *            the name of the scaling
     * @param xScale
     *            how to scale the component width
     * @param yScale
     *            how to scale the component height
     */
    public void xTestShowLookAlignmentSmall() throws Exception
    {
        xTestShowLookAlignmentSized("Small_", 0.8f, 0.8f);
    }

    /**
     * Tests the proper rendering of aligned content.
     * 
     * @param scaleName
     *            the name of the scaling
     * @param xScale
     *            how to scale the component width
     * @param yScale
     *            how to scale the component height
     */
    public void xTestShowLookAlignmentSized(String scaleName, float xScale, float yScale) throws Exception
    {
        if (!usesContent()) return;

        final int hAlignIndex = ((int[]) params)[0];
        final int vAlignIndex = ((int[]) params)[1];

        int hAlign = hAlignConst[hAlignIndex];
        int vAlign = vAlignConst[vAlignIndex];

        HScene testScene = getHScene();

        hvisible.setHorizontalAlignment(hAlign);
        hvisible.setVerticalAlignment(vAlign);
        hvisible.setLook(hlook);
        addAlignedContent(hvisible);

        testScene.setBackground(Color.lightGray);
        try
        {

            testScene.add(hvisible);
            // Increase size so we can better make out alignment
            // Decrease size so we can check proper alignment
            scaleSize(hvisible, xScale, yScale);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();

            Insets insets = hlook.getInsets(hvisible);
            String name = scaleName + hAlignName[hAlignIndex] + "_" + vAlignName[vAlignIndex];

            TestSupport.checkDisplay(hvisible, name, new String[] {
                    "Is content aligned horizontally: " + hAlignName[hAlignIndex] + " ?",
                    "Is content aligned vertically: " + vAlignName[vAlignIndex] + " ?",
                    "Is the top border spacing " + insets.top + " pixels?",
                    "Is the left border spacing " + insets.left + " pixels?",
                    "Is the bottom border spacing " + insets.bottom + " pixels?",
                    "Is the right border spacing " + insets.right + " pixels?", }, "align_" + name, this);
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    /**
     * Scales the component by the given ratio.
     */
    protected void scaleSize(HVisible hvisible, float xScale, float yScale)
    {
        Dimension d = getContentMaxSize(hvisible);
        hvisible.setDefaultSize(new Dimension((int) (d.width * xScale), (int) (d.height * yScale)));
    }

    /**
     * Tests the proper rendering of scaled content (if supported).
     */
    public void testShowLookScaling() throws Exception
    {
        if (!isScalingSupported()) return;

        HScene testScene = getHScene();

        hvisible.setLook(hlook);
        addScaledContent(hvisible);

        testScene.setBackground(Color.black);
        testScene.add(hvisible);

        // Set up content
        String ask[] = new String[] { "The entire image is visible?", null, // stretch?
                                                                            // "The image is not stretched horizontally?",
                null, // stretch? "The image is not stretched vertically?",
                null, // size? "The image is smaller than total area?",
        };

        try
        {
            hvisible.setResizeMode(HVisible.RESIZE_NONE);
            ask[1] = "The image is not stretched horizontally?";
            ask[2] = "The image is not stretched vertically?";
            ask[3] = "The image is smaller than total area?";
            scaleSize(hvisible, 1.25f, 1.25f);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            TestSupport.checkDisplay(hvisible, "No scaling should occur", ask, "RESIZE_NONE", this);

            hvisible.setResizeMode(HVisible.RESIZE_ARBITRARY);
            ask[1] = "The image is stretched horizontally?";
            ask[2] = "The image is stretched vertically?";
            ask[3] = "The image is fills the total area?";
            TestSupport.checkDisplay(hvisible, "Scaling should occur in all directions", ask, "RESIZE_ARBITRARY", this);

            hvisible.setResizeMode(HVisible.RESIZE_PRESERVE_ASPECT);
            ask[1] = "The image is stretched horizontally?";
            ask[2] = "The image is stretched vertically?";
            ask[3] = "The image only fills horizontally?";
            scaleSize(hvisible, 1.0f, 1.25f);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            TestSupport.checkDisplay(hvisible, "Scaling should preserve w:h ratio", ask, "RESIZE_PRESERVE_V", this);

            hvisible.setResizeMode(HVisible.RESIZE_PRESERVE_ASPECT);
            ask[1] = "The image is stretched horizontally?";
            ask[2] = "The image is stretched vertically?";
            ask[3] = "The image only fills vertically?";
            scaleSize(hvisible, 1.25f, 1.0f);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            TestSupport.checkDisplay(hvisible, "Scaling should preserve w:h ratio", ask, "RESIZE_PRESERVE_H", this);
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    /**
     * Tests the proper rendering of background modes.
     */
    public void testShowLookBackground() throws Exception
    {
        hvisible.setLook(hlook);
        addBGContent(hvisible);
        Dimension d = getContentMaxSize(hvisible);

        HScene testScene = getHScene();
        testScene.setBackground(Color.black);
        hvisible.setBackground(Color.orange);
        testScene.add(hvisible);
        scaleSize(hvisible, 1.25f, 1.25f);

        try
        {
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            hvisible.setBackgroundMode(HVisible.BACKGROUND_FILL);
            TestSupport.checkDisplay(hvisible, "BG should be filled", new String[] { "The background is orange?" },
                    "BGFILL", this);

            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            hvisible.setBackgroundMode(HVisible.NO_BACKGROUND_FILL);
            TestSupport.checkDisplay(hvisible, "BG should not be filled", new String[] { "The background is black?" },
                    "NOBGFILL", this);
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    /**
     * Tests the proper rendering of state-specific content.
     * 
     * Takes an Integer(state) as its parameter.
     */
    public void xTestShowLookStates() throws Exception
    {
        if (!hasStateSpecificContent()) return;

        int state = ((Integer) params).intValue();
        try
        {
            hvisible.setInteractionState(state);
        }
        catch (IllegalArgumentException e)
        {
            // If an invalid state, forget about it...
            return;
        }

        addStateContent(hvisible);
        hvisible.setLook(hlook);
        hvisible.setDefaultSize(getContentMaxSize(hvisible));

        HScene testScene = getHScene();
        testScene.setBackground(Color.lightGray);
        try
        {
            testScene.add(hvisible);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();

            String stateName = TestSupport.getStateName(state);
            String name = stateName + " content";

            TestSupport.checkDisplay(hvisible, name, new String[] { "Is content for " + stateName + " state "
                    + "displayed?" }, stateName, this);
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    private class EmptyTextLayout implements HTextLayoutManager
    {
        public EmptyTextLayout()
        {
        }

        public void render(java.lang.String markedUpString, java.awt.Graphics g, HVisible v, java.awt.Insets insets)
        {
        }
    };

    private class EmptyDefaultTextLayout extends HDefaultTextLayoutManager
    {
        final int[] called;

        final Dimension dummy;

        public EmptyDefaultTextLayout(final int called[], Dimension dummy)
        {
            this.called = called;
            this.dummy = dummy;
        }

        public Dimension getMinimumSize(HVisible v)
        {
            ++called[0];
            return new Dimension(dummy);
        }

        public Dimension getMaximumSize(HVisible v)
        {
            ++called[0];
            return new Dimension(dummy);
        }

        public Dimension getPreferredSize(HVisible v)
        {
            ++called[0];
            return new Dimension(dummy);
        }
    };
}
