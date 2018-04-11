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
import java.awt.*;
import java.text.AttributedCharacterIterator;

/**
 * Tests {@link #HVisible}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.11 $, $Date: 2002/11/07 21:14:10 $
 */
public class HVisibleTest extends HComponentTest implements HState
{
    private static final long WAIT_TIME = 5000;

    /**
     * Standard constructor.
     */
    public HVisibleTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HVisibleTest.class);
    }

    /**
     * Assert that the two object arrays are the same.
     */
    public static void assertArraySame(String message, Object[] a1, Object[] a2)
    {
        if (a1 == null || a2 == null)
            assertSame("Both arrays should be null", a1, a2);
        else
        {
            assertEquals("Lengths of arrays not equal", a1.length, a2.length);
            assertSame("Array types are different", a1.getClass(), a2.getClass());

            for (int i = 0; i < a1.length; ++i)
                assertSame("Element [" + i + "] does not match in arrays", a1[i], a2[i]);
        }
    }

    /**
     * The tested component.
     */
    protected HVisible hvisible;

    /**
     * Should be overridden to create subclass of HVisible.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return new HVisible();
    }

    /**
     * Overridden to create an HVisible.
     * 
     * @return the instance of HComponent to test
     */
    protected HComponent createHComponent()
    {
        return (hvisible = createHVisible());
    }

    /**
     * Utility method used to enumerate the various HStates.
     */
    public static void foreachState(Callback callback)
    {
        final int FIRST = FIRST_STATE & ~NORMAL_STATE;
        final int END = LAST_STATE & ~NORMAL_STATE;

        for (int i = FIRST; i <= END; ++i)
            callback.callback(i | NORMAL_STATE);
    }

    /**
     * Simple state-based callback interface.
     * 
     * @see #foreachState(Callback)
     */
    public static interface Callback
    {
        public void callback(int state);
    }

    /**
     * Utility method gets a textual description of a state.
     */
    protected String stateToString(int state)
    {
        return TestSupport.getStateName(state);
    }

    /**
     * Returns the given look wrapped in a
     */
    public static HLook wrapLook(HVisible v, HLook l)
    {
        if (v instanceof HStaticText && !(l instanceof HTextLook))
            return new org.cablelabs.gear.havi.decorator.TextLookAdapter(l);
        else if (v instanceof HStaticIcon && !(l instanceof HGraphicLook))
            return new org.cablelabs.gear.havi.decorator.GraphicLookAdapter(l);
        else if (v instanceof HStaticAnimation && !(l instanceof HAnimateLook))
            return new org.cablelabs.gear.havi.decorator.AnimateLookAdapter(l);
        else if (v instanceof HStaticRange && !(l instanceof HRangeLook))
            return new org.cablelabs.gear.havi.decorator.RangeLookAdapter(l);
        else if (v instanceof HListGroup && !(l instanceof HListGroupLook))
            return new org.cablelabs.gear.havi.decorator.ListLookAdapter(l);
        else if (v instanceof HMultilineEntry && !(l instanceof HMultilineEntryLook))
            return new org.cablelabs.gear.havi.decorator.MultilineEntryLookAdapter(l);
        else if (v instanceof HSinglelineEntry && !(l instanceof HSinglelineEntryLook))
            return new org.cablelabs.gear.havi.decorator.SinglelineEntryLookAdapter(l);
        else
            return l;
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new EmptyLook();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HComponent
     * <li>implements HState
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HVisibleTest.class);

        TestUtils.testExtends(HVisible.class, HComponent.class);
        TestUtils.testImplements(HVisible.class, HState.class);
    }

    /**
     * Test the 3 constructors of HVisible.
     * <ul>
     * <li>HVisible()
     * <li>HVisible(HLook hlook)
     * <li>HVisible(HLook hlook, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HVisibleTest.class);

        HLook look = createLook();
        checkConstructor("HVisible()", new HVisible(), null, // no look (use
                                                             // default)
                0, 0, 0, 0, // default loc
                false); // no default size
        checkConstructor("HVisible(HLook)", new HVisible(look), look, // has a
                                                                      // look
                0, 0, 0, 0, // default loc
                false); // no default size
        checkConstructor("HVisible(HLook,int,int,int,int)", new HVisible(look, 10, 10, 255, 255), look, // has
                                                                                                        // a
                                                                                                        // look
                10, 10, 255, 255, // given loc
                true); // has a default size
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, final HVisible v, HLook look, int x, int y, int w, int h,
            boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", v);
        assertSame(msg + "look not initialized correctly", look, v.getLook());
        assertEquals(msg + " x-coordinated not initialized correctly", x, v.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, v.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, v.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, v.getSize().height);
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                String str = stateToString(state);
                assertNull(str + " text content should be null", v.getTextContent(state));
                assertNull(str + " graphic content should be null", v.getGraphicContent(state));
                assertNull(str + " animate content should be null", v.getAnimateContent(state));
                assertNull(str + " content should be null", v.getContent(state));
            }
        });

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, v.getInteractionState());
        assertNull(msg + " matte should be unassigned", v.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", v.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", v.getBackgroundMode(), v.NO_BACKGROUND_FILL);
        if (!defaultSize)
            assertEquals(msg + " default size should not be set", v.NO_DEFAULT_SIZE, v.getDefaultSize());
        // assertNull(msg+" default size should not be set",
        // v.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", v.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", v.getHorizontalAlignment(), v.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", v.getVerticalAlignment(), v.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", v.getResizeMode(), v.RESIZE_NONE);
        assertEquals(msg + " border mode not initialized correctly", true, v.getBordersEnabled());
    }

    /**
     * Tests for unexpected fields and unique fields.
     */
    public void testFields()
    {
        checkClass(HVisibleTest.class);

        TestUtils.testNoAddedFields(HVisible.class, fields1);
        TestUtils.testUniqueFields(HVisible.class, fields1, false, 0, 4);
        TestUtils.testUniqueFields(HVisible.class, fields1, false, 4, 4);
        TestUtils.testUniqueFields(HVisible.class, fields1, false, 8, 3);
        TestUtils.testUniqueFields(HVisible.class, fields1, false, 11, 2);
        TestUtils.testUniqueFields(HVisible.class, fields1, false, 13, 25);

        // This actually is testing HState, however there is not HStateTest
        TestUtils.testNoAddedFields(HState.class, fields2);
        TestUtils.testUniqueFields(HState.class, fields2, false, 0, 9);
    }

    private static final String fields2[] = { "NORMAL_STATE", "FOCUSED_STATE", "ACTIONED_STATE",
            "ACTIONED_FOCUSED_STATE", "DISABLED_STATE", "DISABLED_FOCUSED_STATE", "DISABLED_ACTIONED_STATE",
            "DISABLED_ACTIONED_FOCUSED_STATE", "ALL_STATES", "FOCUSED_STATE_BIT", "ACTIONED_STATE_BIT",
            "DISABLED_STATE_BIT", "FIRST_STATE", "LAST_STATE", };

    private static final String fields1[] = {
            "HALIGN_LEFT", // 0
            "HALIGN_CENTER",
            "HALIGN_RIGHT",
            "HALIGN_JUSTIFY",
            "VALIGN_TOP", // 4
            "VALIGN_CENTER",
            "VALIGN_BOTTOM",
            "VALIGN_JUSTIFY",
            "RESIZE_NONE", // 8
            "RESIZE_PRESERVE_ASPECT",
            "RESIZE_ARBITRARY",
            "NO_BACKGROUND_FILL", // 11
            "BACKGROUND_FILL",
            "TEXT_CONTENT_CHANGE", // 13
            "GRAPHIC_CONTENT_CHANGE", "ANIMATE_CONTENT_CHANGE", "CONTENT_CHANGE", "STATE_CHANGE",
            "CARET_POSITION_CHANGE", "ECHO_CHAR_CHANGE", "EDIT_MODE_CHANGE", "MIN_MAX_CHANGE", "THUMB_OFFSETS_CHANGE",
            "ADJUSTMENT_VALUE_CHANGE", "ORIENTATION_CHANGE", "TEXT_VALUE_CHANGE", "ITEM_VALUE_CHANGE",
            "LIST_CONTENT_CHANGE", "LIST_ICONSIZE_CHANGE", "LIST_LABELSIZE_CHANGE", "LIST_MULTISELECTION_CHANGE",
            "LIST_SCROLLPOSITION_CHANGE", "SIZE_CHANGE", "BORDER_CHANGE", "REPEAT_COUNT_CHANGE",
            "ANIMATION_POSITION_CHANGE", "LIST_SELECTION_CHANGE", "UNKNOWN_CHANGE", "FIRST_CHANGE", // 38
            "LAST_CHANGE", "NO_DEFAULT_WIDTH", "NO_DEFAULT_HEIGHT", "NO_DEFAULT_SIZE", };

    /**
     * Tests isFocusTraversable() == false.
     */
    public void testFocusTraversable()
    {
        HVisible v = hvisible;
        assertEquals("Should be focus traversable only if navigable", (v instanceof HNavigable), v.isFocusTraversable());
    }

    /**
     * Tests paint().
     * <ul>
     * <li>Ensures that the appropriate look is called.
     * <li>Ensures that, without a look, nothing is painted.
     * <li>Ensures that the correct information is passed to the look.
     * </ul>
     */
    public void testPaint() throws Exception
    {
        final HVisible v = hvisible;
        final boolean okay[] = new boolean[1];
        v.setLook(wrapLook(v, new EmptyLook()
        {
            public void showLook(Graphics g, HVisible visible, int state)
            {
                okay[0] = true;

                assertSame("Incorrect Graphics passed to look", null, g);
                assertSame("Incorrect HVisible passed to look", v, visible);
                assertEquals("Incorrect state passed to look", NORMAL_STATE, state);
            }
        }));

        okay[0] = false;
        v.paint((Graphics) null);
        assertTrue("paint() should call look.showLook()", okay[0]);

        v.setLook(null);
        v.paint((Graphics) null);
        // No exceptions should be thrown
    }

    /**
     * Tests update().
     * <ul>
     * <li>The background is not cleared.
     * <li>The current color is set to the background color.
     * <li>paint is called
     * </ul>
     */
    public void testUpdate()
    {
        final Color color[] = new Color[1];
        Graphics g = new EmptyGraphics()
        {
            public void setColor(Color c)
            {
                color[0] = c;
            }

            public void fillRect(int x, int y, int width, int height)
            {
                fail("Background was filled");
            }

            public void clearRect(int x, int y, int width, int height)
            {
                fail("Background was cleared");
            }
        };

        final boolean okay[] = new boolean[1];
        HVisible v = new HVisible()
        {
            public void paint(Graphics g)
            {
                okay[0] = true;
            }
        };

        okay[0] = false;
        color[0] = null;

        v.setBackground(Color.orange);
        v.update(g);
        assertSame("Color should've been set to the background color", Color.orange, color[0]);
        assertTrue("HVisible.paint() should've been called by update", okay[0]);
    }

    /**
     * Tests setTextContent/getTextContent.
     * <ul>
     * <li>Ensures that what is put in is retreived, for all states
     * <li>Ensures that ALL_STATES sets all content
     * <li>Ensures that illegal states (just a STATE_BIT) throw an exception
     * <li>Ensures that content can be removed with <code>null</code>
     * <li>Ensure that look is notified of content change (correctly!)
     * </ul>
     */
    public void testTextContent() throws Exception
    {
        final int hint = HVisible.TEXT_CONTENT_CHANGE;
        final String hintName = "TEXT_CONTENT_CHANGE";

        final HVisible v = hvisible;
        final Object[] save = new Object[9];
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(v, hint, hintName, hcd);

        /*  ****** Set content should be retreived ****** */
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                String str = stateToString(state);
                assertNull("Content should be null [" + str + "]", v.getTextContent(state));

                // Save current content
                saveContent(v, state, hint, save);
                hcd[0] = null;

                // Set the content
                v.setTextContent(str, state);

                // Check that it is set
                assertEquals("Content should be set", str, v.getTextContent(state));

                // Check for HChangeData
                assertNotNull(hintName + " change data expected", hcd[0]);
                checkContentChanged(hcd[0], hint, save);
            }
        });

        /*  ****** ALL_STATES should work ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content
        v.setTextContent("ALL", ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                assertEquals("All content should be the same", "ALL", v.getTextContent(state));
            }
        });

        /*  ***** Removal of content w/ null ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content to null
        v.setTextContent(null, ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                assertNull("All content should be the null", v.getTextContent(state));
            }
        });

        /*  ***** Illegal states cause exception ***** */
        final int bits[] = { FOCUSED_STATE_BIT, ACTIONED_STATE_BIT, DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | ACTIONED_STATE_BIT, ACTIONED_STATE_BIT | DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | DISABLED_STATE_BIT, };
        for (int i = 0; i < bits.length; ++i)
        {
            try
            {
                v.setTextContent("" + i, bits[i]);
                fail("IllegalArgumentException should be thrown when setting " + "content using non-states (" + bits[i]
                        + ")");
            }
            catch (IllegalArgumentException expected)
            {
            }
        }
    }

    /**
     * Tests setGraphicContent/getGraphicContent.
     * <ul>
     * <li>Ensures that ALL_STATES sets all content
     * <li>Ensures that what is put in is retreived, for all states
     * <li>Ensures that content can be removed with <code>null</code>
     * <li>Ensures that illegal states (just a STATE_BIT) throw an exception
     * <li>Ensure that look is notified of content change (correctly!)
     * </ul>
     */
    public void testGraphicContent() throws HInvalidLookException
    {
        final int hint = HVisible.GRAPHIC_CONTENT_CHANGE;
        final String hintName = "GRAPHIC_CONTENT_CHANGE";

        final HVisible v = hvisible;
        final Object[] save = new Object[9];
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(v, hint, hintName, hcd);

        /*  ****** Set content should be retreived ****** */
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                Image img = new EmptyImage();
                assertNull("Content should be null (" + s + ")", v.getGraphicContent(s));

                // Save current content
                saveContent(v, s, hint, save);
                hcd[0] = null;

                // Set the content
                v.setGraphicContent(img, s);

                // Check that it is set
                assertEquals("Content should be set", img, v.getGraphicContent(s));

                // Check for HChangeData
                assertNotNull(hintName + " change data expected", hcd[0]);
                checkContentChanged(hcd[0], hint, save);
            }
        });

        /*  ****** ALL_STATES should work ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content
        final Image img = new EmptyImage();
        v.setGraphicContent(img, ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertEquals("All content should be the same", img, v.getGraphicContent(s));
            }
        });

        /*  ***** Removal of content w/ null ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content to null
        v.setGraphicContent(null, ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertNull("All content should be the null", v.getGraphicContent(s));
            }
        });

        // Illegal states cause exception
        final int bits[] = { FOCUSED_STATE_BIT, ACTIONED_STATE_BIT, DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | ACTIONED_STATE_BIT, ACTIONED_STATE_BIT | DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | DISABLED_STATE_BIT, };
        for (int i = 0; i < bits.length; ++i)
        {
            try
            {
                v.setGraphicContent(img, bits[i]);
                fail("IllegalArgumentException should be thrown when setting " + "content using non-states (" + i + ")");
            }
            catch (IllegalArgumentException expected)
            {
            }
        }
    }

    /**
     * Tests setAnimateContent/getAnimateContent.
     * <ul>
     * <li>Ensures that ALL_STATES sets all content
     * <li>Ensures that what is put in is retreived, for all states
     * <li>Ensures that content can be removed with <code>null</code>
     * <li>Ensures that illegal states (just a STATE_BIT) throw an exception
     * <li>Ensure that look is notified of content change (correctly!)
     * </ul>
     */
    public void testAnimateContent() throws HInvalidLookException
    {
        final int hint = HVisible.ANIMATE_CONTENT_CHANGE;
        final String hintName = "ANIMATE_CONTENT_CHANGE";

        final HVisible v = hvisible;
        final Object[] save = new Object[9];
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(v, hint, hintName, hcd);

        /*  ****** Set content should be retreived ****** */
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                Image img[] = new Image[1];
                assertNull("Content should be null (" + s + ")", v.getAnimateContent(s));

                // Save current content
                saveContent(v, s, hint, save);
                hcd[0] = null;

                // Set the content
                v.setAnimateContent(img, s);

                // Check that it is set
                assertEquals("Content should be set", img, v.getAnimateContent(s));

                // Check for HChangeData
                assertNotNull(hintName + " change data expected", hcd[0]);
                checkContentChanged(hcd[0], hint, save);
            }
        });

        /*  ****** ALL_STATES should work ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content
        final Image img[] = new Image[2];
        v.setAnimateContent(img, ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertEquals("All content should be the same", img, v.getAnimateContent(s));
            }
        });

        /*  ***** Removal of content w/ null ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content to null
        v.setAnimateContent(null, ALL_STATES);
        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertNull("All content should be the null", v.getAnimateContent(s));
            }
        });

        // Illegal states cause exception
        final int bits[] = { FOCUSED_STATE_BIT, ACTIONED_STATE_BIT, DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | ACTIONED_STATE_BIT, ACTIONED_STATE_BIT | DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | DISABLED_STATE_BIT, };
        for (int i = 0; i < bits.length; ++i)
        {
            try
            {
                v.setAnimateContent(img, bits[i]);
                fail("IllegalArgumentException should be thrown when setting " + "content using non-states (" + i + ")");
            }
            catch (IllegalArgumentException expected)
            {
            }
        }
    }

    /**
     * Tests setContent/getContent.
     * <ul>
     * <li>Ensures that ALL_STATES sets all content
     * <li>Ensures that what is put in is retreived, for all states
     * <li>Ensures that content can be removed with <code>null</code>
     * <li>Ensures that illegal states (just a STATE_BIT) throw an exception
     * <li>Ensure that look is notified of content change (correctly!)
     * </ul>
     */
    public void testContent() throws HInvalidLookException
    {
        final int hint = HVisible.CONTENT_CHANGE;
        final String hintName = "CONTENT_CHANGE";

        final HVisible v = hvisible;
        final Object[] save = new Object[9];
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(v, hint, hintName, hcd);

        /*  ****** Set content should be retreived ****** */
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertNull("Content should be null (" + s + ")", v.getContent(s));

                // Save current content
                saveContent(v, s, hint, save);
                hcd[0] = null;

                // Set the content
                v.setContent(stateToString(s), s);

                // Check that it is set
                assertEquals("Content should be set", stateToString(s), v.getContent(s));

                // Check for HChangeData
                assertNotNull(hintName + " change data expected", hcd[0]);
                checkContentChanged(hcd[0], hint, save);
            }
        });

        /*  ****** ALL_STATES should work ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content
        v.setContent("ALL", ALL_STATES);

        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertEquals("All content should be the same", "ALL", v.getContent(s));
            }
        });

        /*  ***** Removal of content w/ null ***** */
        // Save current content
        saveContent(v, ALL_STATES, hint, save);
        hcd[0] = null;

        // Set the content to null
        v.setContent(null, ALL_STATES);
        // Check for HChangeData
        assertNotNull(hintName + " change data expected", hcd[0]);
        checkContentChanged(hcd[0], hint, save);

        // Check that the content was set
        foreachState(new Callback()
        {
            public void callback(int s)
            {
                assertNull("All content should be the null", v.getContent(s));
            }
        });

        // Illegal states cause exception
        final int bits[] = { FOCUSED_STATE_BIT, ACTIONED_STATE_BIT, DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | ACTIONED_STATE_BIT, ACTIONED_STATE_BIT | DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | DISABLED_STATE_BIT, };
        for (int i = 0; i < bits.length; ++i)
        {
            try
            {
                v.setContent("", bits[i]);
                fail("IllegalArgumentException should be thrown when setting " + "content using non-states (" + i + ")");
            }
            catch (IllegalArgumentException expected)
            {
            }
        }
    }

    /**
     * Tests setLook/getLook.
     * <ul>
     * <li>setLook(null) should be allowed
     * <li>getLook() returns whatever setLook() assigned
     * <li>any look should be acceptable
     * </ul>
     */
    public void testLook() throws Exception
    {
        HVisible v = hvisible;

        if (getClass() == HVisibleTest.class) assertNull("No look should be assigned", v.getLook());

        // Should have no affect
        v.setLook(null);

        HLook look = createLook();
        v.setLook(look);
        assertSame("Incorrect look retreived", look, v.getLook());

        // Should have no affect
        v.setLook(null);
        assertNull("Look should've been removed", v.getLook());

        // Make sure it accepts a 'new' look
        look = wrapLook(v, new EmptyLook()
        {
        });
        v.setLook(look);
        assertSame("Incorrect look retreived", look, v.getLook());
    }

    /**
     * Tests get{Preferred|Maximum|Minimum}Size()
     * <ul>
     * <li>The look should be queried.
     * <li>If no look is set, then the current size should be returned.
     * </ul>
     */
    public void testSizing() throws Exception
    {
        HVisible v = hvisible;
        final boolean okay[] = new boolean[3];
        HLook look = wrapLook(v, new EmptyLook()
        {
            public Dimension getPreferredSize(HVisible v)
            {
                okay[0] = true;
                return null;
            }

            public Dimension getMinimumSize(HVisible v)
            {
                okay[1] = true;
                return null;
            }

            public Dimension getMaximumSize(HVisible v)
            {
                okay[2] = true;
                return null;
            }
        });
        v.setLook(look);

        // Make sure that our look is queried
        okay[0] = false;
        v.getPreferredSize();
        assertTrue("look.getPreferredSize() should've been queuried", okay[0]);

        okay[1] = false;
        v.getMinimumSize();
        assertTrue("look.getMinimumSize() should've been queuried", okay[1]);

        okay[2] = false;
        v.getMaximumSize();
        assertTrue("look.getMaximumSize() should've been queuried", okay[2]);

        // Make sure that the actual size is returned
        Dimension d = new Dimension(123, 456);
        v.setSize(d);
        v.setLook(null);

        assertEquals("PreferredSize should be actual", d, v.getPreferredSize());
        assertEquals("MinimumSize should be actual", d, v.getMinimumSize());
        assertEquals("MaximumSize should be actual", d, v.getMaximumSize());
    }

    /**
     * Tests getInteractionState
     * <ul>
     * <li>The state should not change (if clicked, focused, anything)
     * <li>Attempts to set to states not valid for the subclass should cause an
     * IllegalArgumentException
     * <li>If just a STATE_BIT is set, an IllegalArgumentException is thrown
     * </ul>
     */
    public void testInteractionState() throws Exception
    {
        final int hint = HVisible.STATE_CHANGE;
        final String hintName = "STATE_CHANGE";

        final HVisible v = hvisible;
        final HChangeData[] hcd = new HChangeData[1];
        createWidgetChangeLook(v, hint, hintName, hcd);

        assertEquals("Initial state should be NORMAL", NORMAL_STATE, v.getInteractionState());

        // Check that given states are allowed (based on subclass type)
        boolean isNav = v instanceof HNavigable;
        boolean isAct = v instanceof HActionable;
        final boolean[] allowed = { true, // NORMAL_STATE
                isNav, // FOCUSED_STATE
                isAct, // ACTIONED_STATE
                isAct, // ACTIONED_FOCUSED_STATE
                true, // DISABLED_STATE
                isNav, // DISABLED_FOCUSED_STATE
                isAct, // DISABLED_ACTIONED_STATE
                isAct, // DISABLED_ACTIONED_FOCUSED_STATE
        };
        v.setInteractionState(DISABLED_STATE); // don't start in NORMAL_STATE
        for (int i = 0; i < allowed.length; ++i)
        {
            int state = i | NORMAL_STATE;
            String str = stateToString(state);
            try
            {
                // Save current state
                int save = v.getInteractionState();
                hcd[0] = null;

                // Set the state
                v.setInteractionState(state);

                // Check setting of state (if allowed)
                assertTrue("State should NOT be allowed: " + str, allowed[i]);
                assertEquals("Set state should be retreived state", state, v.getInteractionState());

                // Check for HChangeData
                assertNotNull(hintName + " change data expected", hcd[0]);
                checkStateChanged(hcd[0], save);
            }
            catch (IllegalArgumentException e)
            {
                assertTrue("State should be allowed: " + str, !allowed[i]);
            }
        }

        // State bits should not be allowed
        final int bits[] = { FOCUSED_STATE_BIT, ACTIONED_STATE_BIT, DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | ACTIONED_STATE_BIT, ACTIONED_STATE_BIT | DISABLED_STATE_BIT,
                FOCUSED_STATE_BIT | DISABLED_STATE_BIT, };
        for (int i = 0; i < bits.length; ++i)
        {
            try
            {
                v.setInteractionState(bits[i]);
                fail("State bits should not be legal input to " + "setInteractionState()");
            }
            catch (IllegalArgumentException expected)
            {
            }
        }
    }

    /**
     * Tests setTextLayoutManager/getTextLayoutManager
     * <ul>
     * <li>The default should be NOT null
     * <li>Whatever is set should be retreivable
     * <li>setTextLayoutMangager(null) should be allowed
     * </ul>
     */
    public void testTextLayoutManager()
    {
        HVisible v = hvisible;
        assertNotNull("TextLayoutManager should be set", v.getTextLayoutManager());

        HTextLayoutManager tlm = new HTextLayoutManager()
        {
            public void render(String s, Graphics g, HVisible v, Insets i)
            {
            }
        };
        v.setTextLayoutManager(tlm);
        assertSame("Incorrect TextLayoutManager retreived", tlm, v.getTextLayoutManager());

        v.setTextLayoutManager(null);
        assertNull("Should be able to clear TextLayoutManager", v.getTextLayoutManager());
    }

    /**
     * Tests (set|get)BackgroundMode().
     * <ul>
     * <li>Only NO_BACKGROUND_FILL or BACKGROUND_FILL should be accepted
     * <li>The set mode should be the retreived mode
     * </ul>
     */
    public void testBackgroundMode()
    {
        HVisible v = hvisible;

        // Set is retrieved...
        v.setBackgroundMode(v.NO_BACKGROUND_FILL);
        assertEquals("Set bgm should be retrieved", v.NO_BACKGROUND_FILL, v.getBackgroundMode());

        v.setBackgroundMode(v.BACKGROUND_FILL);
        assertEquals("Set bgm should be retrieved", v.BACKGROUND_FILL, v.getBackgroundMode());

        // Do not accept other values
        try
        {
            v.setBackgroundMode(v.NO_BACKGROUND_FILL);
            v.setBackgroundMode(v.NO_BACKGROUND_FILL * 2 + v.BACKGROUND_FILL * 2);

            // Exception should be thrown, or no change made
            assertEquals("Should not accept invalid background mode values", v.NO_BACKGROUND_FILL,
                    v.getBackgroundMode());
        }
        catch (IllegalArgumentException okay)
        {
        }
    }

    /**
     * Tests isOpaque().
     * <ul>
     * <li>Calls the HLook.isOpaque() method.
     * <li>If not look is set, returns false.
     * <li>Otherwise, don't really care about what it returns.
     * </ul>
     */
    public void testOpaque() throws Exception
    {
        HVisible v = hvisible;

        final boolean called[] = { false };
        HLook look = wrapLook(v, new EmptyLook()
        {
            public boolean isOpaque(HVisible v)
            {
                called[0] = true;
                return true;
            }
        });

        // Should return false if not set
        v.setLook(null);
        assertTrue("isOpaque should return false given no HLook", !v.isOpaque());

        // Should question look if set
        v.setLook(look);
        called[0] = false;
        v.isOpaque();
        assertTrue("isOpaque should query getLook().isOpaque()", called[0]);
    }

    /**
     * Tests (set|get)DefaultSize().
     * <ul>
     * <li>The set default size should be the retrieved default size.
     * <li>Values below n implementation-defined minimum should result in an
     * IllegalArgumentException
     * <li>An input of null should result in a NullPointerException
     * </ul>
     */
    public void testDefaultSize()
    {
        HVisible v = hvisible;
        Dimension[] valid = { v.NO_DEFAULT_SIZE, new Dimension(100, 100),
                new Dimension(v.NO_DEFAULT_WIDTH, v.NO_DEFAULT_HEIGHT), new Dimension(v.NO_DEFAULT_WIDTH, 100),
                new Dimension(100, v.NO_DEFAULT_HEIGHT), };

        for (int i = 0; i < valid.length; ++i)
        {
            v.setDefaultSize(valid[i]);
            assertEquals("Set defaultSize should be retreived defaultSize", valid[i], v.getDefaultSize());
        }

        // IllegalArgumentException
        try
        {
            // Assume Short.MIN_VALUE is too small
            v.setDefaultSize(new Dimension(Short.MIN_VALUE, Short.MIN_VALUE));
            fail("Expected an IllegalArgumentException to a VERY small " + "defaultSize");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // NullPointerException
        if (!allowNullDefaultSize())
        {
            try
            {
                v.setDefaultSize(null);
                fail("Expected a NullPointerException to a null defaultSize");
            }
            catch (NullPointerException expected)
            {
            }
        }
    }

    /**
     * Hack for subclasses which allow null passed to setDefaultSize.
     */
    protected boolean allowNullDefaultSize()
    {
        return false;
    }

    /**
     * Tests (get|set)LookData().
     * <ul>
     * <li>Ensure that null is retreived if no data is set
     * <li>Ensure that set data is retreived data
     * </ul>
     */
    public void testLookData()
    {
        HVisible v = hvisible;

        assertNull("No look data should be set", v.getLookData("An unexpected key"));

        v.setLookData("My key", v);
        assertSame("Retrieved lookData should be set lookData", v, v.getLookData("My key"));

        v.setLookData("My key", null);
        assertNull("Look data should be cleared", v.getLookData("My key"));
    }

    /**
     * Tests (set|get)(Vertical|Horizontal)Alignment.
     * <ul>
     * <li>Set value should be retrieved value.
     * <li>Should only accept HALIGN_(LEFT|CENTER|RIGHT|JUSTIFY) or
     * VALIGN_(TOP|CENTER|BOTTOM|JUSTIFY).
     * <li>Unknown values should not be accepted
     * </ul>
     */
    public void testAlignment()
    {
        HVisible v = hvisible;

        int allowedH[] = { v.HALIGN_LEFT, v.HALIGN_CENTER, v.HALIGN_RIGHT, v.HALIGN_JUSTIFY };
        int allowedV[] = { v.VALIGN_TOP, v.VALIGN_CENTER, v.VALIGN_BOTTOM, v.VALIGN_JUSTIFY };

        for (int H = 0; H < allowedH.length; ++H)
        {
            for (int V = 0; V < allowedV.length; ++V)
            {
                v.setHorizontalAlignment(allowedH[H]);
                v.setVerticalAlignment(allowedV[V]);

                assertEquals("Set hAlign should be retrieved hAlign", allowedH[H], v.getHorizontalAlignment());
                assertEquals("Set vAlign should be retrieved vAlign", allowedV[V], v.getVerticalAlignment());
            }
        }

        // Do not accept unknown values
        try
        {
            v.setHorizontalAlignment(v.HALIGN_LEFT);
            v.setHorizontalAlignment(allowedH[0] * 2 + allowedH[1] * 2 + allowedH[2] * 2 + allowedH[3] * 2);
            assertEquals("Invalid hAlign should not be accepted", v.HALIGN_LEFT, v.getHorizontalAlignment());
        }
        catch (IllegalArgumentException expected)
        {
        }

        try
        {
            v.setVerticalAlignment(v.VALIGN_TOP);
            v.setVerticalAlignment(allowedV[0] * 2 + allowedV[1] * 2 + allowedV[2] * 2 + allowedV[3] * 2);
            assertEquals("Invalid vAlign should not be accepted", v.VALIGN_TOP, v.getVerticalAlignment());
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    /**
     * Test (set|get)ResizeMode(). <li>Set value should be retrieved value. <li>
     * Should only accept RESIZE_(NONE|PRESERVE_ASPECT|ARBITRARY). <li>Unknown
     * values should not be accepted
     */
    public void testResizeMode()
    {
        HVisible v = hvisible;
        int allowed[] = { v.RESIZE_PRESERVE_ASPECT, v.RESIZE_ARBITRARY, v.RESIZE_NONE, };
        for (int i = 0; i < allowed.length; ++i)
        {
            v.setResizeMode(allowed[i]);
            assertEquals("Set resizeMode should be retrieved resizeMode", allowed[i], v.getResizeMode());
        }

        // Do not accept unknown values
        try
        {
            v.setResizeMode(v.RESIZE_ARBITRARY);
            v.setResizeMode(allowed[0] * 2 + allowed[1] * 2 + allowed[2] * 2);
            assertEquals("Invalid resizeMode should not be accepted", v.RESIZE_ARBITRARY, v.getResizeMode());
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    /**
     * Tests get/setBordersEnabled().
     * <ul>
     * <li>Default value is true.
     * <li>Set value should be retrieved value.
     * <li>Should only generate a BORDER_CHANGE widgetChanged call for
     * HAnimateLook, HGraphicLook and HTextLook.
     * <li>Look should return insets of 0,0,0,0 for enabled==false.
     * <li>All other looks shall ignore this value and the method shall not be
     * called.
     * <li>The behavior of third party looks is not defined.
     * </ul>
     */
    public void testBordersEnabled()
    {
        // Default
        assertTrue("Borders should be enabled by default", hvisible.getBordersEnabled());

        // Set value is retrieved
        for (int i = 0; i < 4; ++i)
        {
            boolean value = (i & 1) != 0;
            hvisible.setBordersEnabled(value);
            assertEquals("Set bordersEnabled value should be retrieved value", value, hvisible.getBordersEnabled());
        }

        // Individual look types...
        final boolean called[] = new boolean[1];
        HLook looks[] = { new HAnimateLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HGraphicLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HTextLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HSinglelineEntryLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HMultilineEntryLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HRangeLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new HListGroupLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, new EmptyLook()
        {
            public void widgetChanged(HVisible v, HChangeData[] d)
            {
                called[0] = true;
            }
        }, };

        for (int i = 0; i < looks.length; ++i)
        {
            HLook look = looks[i];
            boolean should = look instanceof HAnimateLook || look instanceof HGraphicLook || look instanceof HTextLook;
            String shouldve = should ? "should've" : "should not have";

            try
            {
                hvisible.setLook(look);

                for (int j = 0; i < 4; ++i)
                {
                    boolean value = (j & 1) != 0;

                    hvisible.setBordersEnabled(!value);
                    called[0] = false;
                    hvisible.setBordersEnabled(value);

                    assertEquals("widgetChanged " + shouldve + " been called " + "in response to setBordersEnabled("
                            + value + ")", should, called[0]);

                    // Check for 0-sum insets...
                    if (should)
                    {
                        Insets insets = look.getInsets(hvisible);
                        int sum = insets.right + insets.left + insets.top + insets.bottom;

                        assertEquals("Should insets be zero?", !value, sum == 0);
                    }
                }
            }
            catch (HInvalidLookException e)
            {
                /* Skip looks not valid for this component. */
            }
        }
    }

    /**
     * Tests isEnabled().
     * <ul>
     * <li>See {@link HComponentTest#testEnabled()}.
     * <li>Test that enabled state is reflected in interaction state and vice
     * versa.
     * </ul>
     */
    public void testEnabled()
    {
        super.testEnabled();

        for (int i = 0; i < 2; ++i)
        {
            hvisible.setEnabled(false);
            assertEquals("Interaction state should reflect disabled state", DISABLED_STATE_BIT,
                    (hvisible.getInteractionState() & DISABLED_STATE_BIT));

            hvisible.setEnabled(true);
            assertEquals("Interaction state should reflect enabled state", 0,
                    (hvisible.getInteractionState() & DISABLED_STATE_BIT));
        }

        for (int i = 0; i < 2; ++i)
        {
            hvisible.setInteractionState(DISABLED_STATE);
            assertEquals("Disabled state should reflect interaction state", false, hvisible.isEnabled());

            hvisible.setInteractionState(NORMAL_STATE);
            assertEquals("Enabled state should reflect interaction state", true, hvisible.isEnabled());
        }
    }

    /**
     * Tests the generation of SIZE_CHANGE HLook.widgetChanged() calls.
     * <ul>
     * <li>This hint indicates that the size of an HVisible component has
     * changed.
     * <li>The value for this hint is a java.lang.Dimension which contains the
     * old size.
     * </ul>
     * 
     * @throws InterruptedException
     */
    private static abstract class TestSizeChangedCommand
    {
        public abstract void performTest();

        public abstract void verifyResults(HChangeData hcd);
    }

    private void baseTestSizeChanged(TestSizeChangedCommand testCommand) throws HInvalidLookException,
            InterruptedException
    {
        final HVisible v = hvisible;
        final HChangeData[] hcd = new HChangeData[1];
        final int hint = HVisible.SIZE_CHANGE;
        final String hintName = "STATE_CHANGE";
        createWidgetChangeLook(v, hint, hintName, hcd);

        synchronized (hcd)
        {
            hvisible.setSize(50, 50);
            hcd.wait(WAIT_TIME);
        }

        // setSize
        hcd[0] = null;
        synchronized (hcd)
        {
            testCommand.performTest();
            hcd.wait(WAIT_TIME);
            testCommand.verifyResults(hcd[0]);
        }
    }

    public void testSizeChangedChangePosition() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                Rectangle r = hvisible.getBounds();
                oldSize = r.getSize();
                r.x = 3;
                r.y = 3;
                System.out.println("Current bounds is + " + hvisible.getBounds());
                System.out.println("Setting bounds to " + r);
                hvisible.setBounds(r);
            }

            public void verifyResults(HChangeData hcd)
            {
                System.out.println("Current bounds " + hvisible.getBounds());
                assertNull("setBounds(r) should not generate HChangeData. I fail intermittantly! old=" + oldSize
                        + " new=(" + hcd + ")", hcd);
            }

        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetSizeDimension1() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                Rectangle r = hvisible.getBounds();
                oldSize = r.getSize();
                hvisible.setSize(new Dimension(50, 100));
            }

            public void verifyResults(HChangeData hcd)
            {
                checkSizeChanged(hcd, "setSize(d) old=" + oldSize + " new=(" + hcd + ")", oldSize);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetSizeDimension2() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                Rectangle r = hvisible.getBounds();
                oldSize = r.getSize();
                hvisible.setSize(new Dimension(80, 100));
            }

            public void verifyResults(HChangeData hcd)
            {
                checkSizeChanged(hcd, "setSize(d) old=" + oldSize + " new=(" + hcd + ")", oldSize);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetSizeWidthHeight() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                Rectangle r = hvisible.getBounds();
                oldSize = r.getSize();
                hvisible.setSize(75, 75);
            }

            public void verifyResults(HChangeData hcd)
            {
                checkSizeChanged(hcd, "setSize(w,h) old=" + oldSize + " new=(" + hcd + ")", oldSize);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetBoundsXYWH() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                oldSize = hvisible.getSize();
                hvisible.setBounds(3, 3, 60, 60);
            }

            public void verifyResults(HChangeData hcd)
            {
                checkSizeChanged(hcd, "setBounds(x,y,w,h) old=" + oldSize + " new=(" + hcd + ")", oldSize);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetBoundsRectangle() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            Dimension oldSize;

            public void performTest()
            {
                oldSize = hvisible.getSize();
                hvisible.setBounds(new Rectangle(0, 0, 67, 96));
            }

            public void verifyResults(HChangeData hcd)
            {
                checkSizeChanged(hcd, "setBounds(r) old=" + oldSize + " new=(" + hcd + ")", oldSize);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetLocation() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            public void performTest()
            {
                hvisible.setLocation(10, 10);
            }

            public void verifyResults(HChangeData hcd)
            {
                assertNull("setLocation(10, 10) should not generate HChangeData hcd[0]=(" + hcd + ")", hcd);
            }
        };
        baseTestSizeChanged(cmd);
    }

    public void testSizeChangedSetLocationPoint() throws HInvalidLookException, InterruptedException
    {
        TestSizeChangedCommand cmd = new TestSizeChangedCommand()
        {

            public void performTest()
            {
                hvisible.setLocation(new Point(5, 5));
            }

            public void verifyResults(HChangeData hcd)
            {
                assertNull("setLocation(new Point(5, 5)) should not generate HChangeData=(" + hcd + ")", hcd);
            }
        };
        baseTestSizeChanged(cmd);
    }

    /**
     * Checks the HChangeData for expected SIZE_CHANGE information.
     * 
     * @param hcd
     *            HChangeData passed in HLook.widgetChanged call
     * @param what
     *            string describing what should've produced a widgetChanged
     * @param oldSize
     *            the previous oldSize
     */
    protected void checkSizeChanged(HChangeData hcd, String what, Dimension oldSize)
    {
        assertNotNull("non-null SIZE_CHANGE HChangeData expected for " + what, hcd);
        assertEquals("SIZE_CHANGE HChangeData expected for " + what, HVisible.SIZE_CHANGE, hcd.hint);
        assertTrue("non-null Dimension HChangeData expected for " + what, hcd.data != null
                && hcd.data instanceof Dimension);
        assertEquals("Dimension equal to old size expected for " + what, oldSize, (Dimension) hcd.data);
    }

    /**
     * Locates an HChangeData with the given hint.
     * 
     * @param data
     *            an array of HChangeData
     * @param hint
     *            the HVisible change data hint/type (if <code>hint</code> is
     *            <code>UNKNOWN_CHANGE</code>, then accept any hint
     * @return an HChangeData of the requested type; or <code>null</code> if not
     *         found
     */
    public static HChangeData findChangeData(HChangeData[] data, int hint)
    {
        for (int i = 0; i < data.length; ++i)
        {
            if (data[i] != null && (hint == HVisible.UNKNOWN_CHANGE || data[i].hint == hint)) return data[i];
        }
        return null;
    }

    /**
     * Saves the current content of the given <code>HVisible</code>. This is
     * suitable for inclusion in an <code>HChangeData</code> object or for
     * comparison with one.
     * 
     * @param v
     *            the <code>HVisible</code> whose content is to be saved
     * @param state
     *            the current state of <code>v</code>
     * @param content
     *            the type of content to save; can be one of:
     *            <ul>
     *            <li> <code>TEXT_CONTENT_CHANGE</code>
     *            <li> <code>GRAPHIC_CONTENT_CHANGE</code>
     *            <li> <code>ANIMATE_CONTENT_CHANGE</code>
     *            <li> <code>CONTENT_CHANGE</code>
     *            </ul>
     */
    protected Object[] saveContent(HVisible v, int state, int content, Object[] array)
    {
        if (array == null) array = new Object[9];

        array[0] = new Integer(state);
        for (int i = 0; i < 8;)
        {
            state = i | NORMAL_STATE;
            switch (content)
            {
                case HVisible.TEXT_CONTENT_CHANGE:
                    array[++i] = v.getTextContent(state);
                    break;
                case HVisible.GRAPHIC_CONTENT_CHANGE:
                    array[++i] = v.getGraphicContent(state);
                    break;
                case HVisible.ANIMATE_CONTENT_CHANGE:
                    array[++i] = v.getAnimateContent(state);
                    break;
                case HVisible.CONTENT_CHANGE:
                    array[++i] = v.getContent(state);
                    break;
                default:
                    fail("Unknown content type in saveContent()");
            }
        }
        return array;
    }

    /**
     * Check that the change data information matches the desired state.
     * 
     * @param cd
     *            the <code>HChangeData</code> to check
     * @param state
     *            the expected state
     */
    protected void checkStateChanged(HChangeData cd, int state)
    {
        assertNotNull("HChangeData expected to be non-null", cd);
        assertEquals("HChangeData hint not as expected", HVisible.STATE_CHANGE, cd.hint);
        assertNotNull("HChangeData expected a state change info", cd.data);
        assertEquals("State in HChangeData not as expected", state, ((Integer) cd.data).intValue());
    }

    /**
     * Check that the change data information matches the desired
     * <code>hint</code> and <code>data</code> array.
     * 
     * @param cd
     *            the <code>HChangeData</code> to check
     * @param hint
     *            the expected <code>HVisible</code> hint (should be one of:
     *            <ul>
     *            <li> <code>TEXT_CONTENT_CHANGE</code>
     *            <li> <code>GRAPHIC_CONTENT_CHANGE</code>
     *            <li> <code>ANIMATE_CONTENT_CHANGE</code>
     *            <li> <code>CONTENT_CHANGE</code>
     *            </ul>
     *            Should compare equal to <code>cd.data</code>
     * @param data
     *            the <code>Object[9]</code> that is expected to compare equal
     *            to <code>cd.data</code>
     */
    protected void checkContentChanged(HChangeData cd, int hint, Object data[])
    {
        assertNotNull("HChangeData expected to be non-null", cd);
        assertEquals("HChangeData hint not as expected", hint, cd.hint);

        Object[] cddata = (Object[]) cd.data;
        for (int i = 0; i < data.length; ++i)
        {
            assertEquals("Content in HChangeData.data[" + i + "] not as expected", data[i], cddata[i]);
        }
    }

    /**
     * Create and set an HLook that (wrapped appropriately for the given
     * <code>HVisible</code>) overrides <code>widgetChanged()</code> to check
     * for the desired hint.
     * 
     * @param v
     *            the <code>HVisible</code> that this look should be assigned to
     * @param hint
     *            the expected hint (e.g., {@link HVisible#CONTENT_CHANGE})
     * @param hintName
     *            the textual representation of the hint
     * @param hcd
     *            where the look should save the <code>HChangeData</code> object
     *            in question
     * @return the <code>HLook</code> that was created and set on the given
     *         <code>HVisible</code> <code>v</code>
     */
    public static HLook createWidgetChangeLook(HVisible v, final int hint, final String hintName,
            final HChangeData[] hcd) throws HInvalidLookException
    {
        HLook look = wrapLook(v, new EmptyLook()
        {
            public void widgetChanged(HVisible vis, HChangeData[] data)
            {
                assertTrue("The HChangeData array should be of length>0", data.length > 0);

                // Find the right HChangeData
                HChangeData found = findChangeData(data, hint);
                synchronized (hcd)
                {
                    if (found != null) hcd[0] = found;
                    hcd.notifyAll();
                }
            }
        });
        v.setLook(look);
        return look;
    }

    /** Dummy look. */
    public static class EmptyLook implements HLook
    {
        public void showLook(Graphics g, HVisible v, int state)
        {
        }

        public Dimension getPreferredSize(HVisible v)
        {
            return v.getSize();
        }

        public Dimension getMinimumSize(HVisible v)
        {
            return v.getSize();
        }

        public Dimension getMaximumSize(HVisible v)
        {
            return v.getSize();
        }

        public void widgetChanged(HVisible v, HChangeData[] d)
        {
        }

        public boolean isOpaque(HVisible v)
        {
            return false;
        }

        public Insets getInsets(HVisible v)
        {
            return insets;
        }

        private Insets insets = new Insets(0, 0, 0, 0);
    }

    /** Dummy Graphics device. */
    public static class EmptyGraphics extends Graphics
    {
        public Graphics create()
        {
            return this;
        }

        public void translate(int x, int y)
        {
        }

        public Color getColor()
        {
            return null;
        }

        public void setColor(Color c)
        {
        }

        public void setPaintMode()
        {
        }

        public void setXORMode(Color c1)
        {
        }

        public Font getFont()
        {
            return null;
        }

        public void setFont(Font font)
        {
        }

        public FontMetrics getFontMetrics()
        {
            return null;
        }

        public FontMetrics getFontMetrics(Font f)
        {
            return null;
        }

        public Rectangle getClipBounds()
        {
            return null;
        }

        public void clipRect(int x, int y, int width, int height)
        {
        }

        public void setClip(int x, int y, int width, int height)
        {
        }

        public Shape getClip()
        {
            return null;
        }

        public void setClip(Shape clip)
        {
        }

        public void copyArea(int x, int y, int width, int height, int dx, int dy)
        {
        }

        public void drawLine(int x1, int y1, int x2, int y2)
        {
        }

        public void fillRect(int x, int y, int width, int height)
        {
        }

        public void clearRect(int x, int y, int width, int height)
        {
        }

        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
        {
        }

        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
        {
        }

        public void drawOval(int x, int y, int width, int height)
        {
        }

        public void fillOval(int x, int y, int width, int height)
        {
        }

        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
        {
        }

        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
        {
        }

        public void drawPolyline(int xPoints[], int yPoints[], int nPoints)
        {
        }

        public void drawPolygon(int xPoints[], int yPoints[], int nPoints)
        {
        }

        public void fillPolygon(int xPoints[], int yPoints[], int nPoints)
        {
        }

        public void drawString(String str, int x, int y)
        {
        }

        public boolean drawImage(Image img, int x, int y, java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public boolean drawImage(Image img, int x, int y, int width, int height, java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public boolean drawImage(Image img, int x, int y, Color bgcolor, java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                Color bgcolor, java.awt.image.ImageObserver observer)
        {
            return false;
        }

        public void dispose()
        {
        }

        public void drawString(AttributedCharacterIterator iterator, int x, int y)
        {
            // TODO Auto-generated method stub

        }
    }

    /** Dummy Image. */
    public static class EmptyImage extends Image
    {
        public int getWidth(java.awt.image.ImageObserver observer)
        {
            return 0;
        }

        public int getHeight(java.awt.image.ImageObserver observer)
        {
            return 0;
        }

        public java.awt.image.ImageProducer getSource()
        {
            return null;
        }

        public Graphics getGraphics()
        {
            return null;
        }

        public Object getProperty(String name, java.awt.image.ImageObserver observer)
        {
            return UndefinedProperty;
        }

        public void flush()
        {
        }
    }
}
