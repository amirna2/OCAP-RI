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
import org.havi.ui.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.BitSet;
import java.util.Vector;

//import org.cablelabs.havi.ui.*;

/**
 * Tests {@link #HScene}.
 * 
 * @author Aaron Kamienski
 * @version $Id: HSceneTest.java,v 1.8 2002/11/07 21:14:09 aaronk Exp $
 */
public class HSceneTest extends GUITest
{
    /**
     * Standard constructor.
     */
    public HSceneTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HSceneTest.class);
    }

    /** Common access HScene */
    private HScene scene;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        scene = HSceneFactory.getInstance().getDefaultHScene();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
        if (scene != null)
        {
            try
            {
                scene.setVisible(false);
                HSceneFactory.getInstance().dispose(scene);
            }
            catch (Exception ignored)
            {
            }
        }
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Container
     * <li>implements HComponentOrdering
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HScene.class, java.awt.Container.class);
        HComponentOrderingTest.testAncestry(HScene.class);
    }

    /**
     * Ensure that there are no accessible constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HScene.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HScene.class);
    }

    /**
     * Test setVisible()/isVisible()
     * <ul>
     * <li>Check that default to false
     * <li>Ensure NO implicit focus request
     * <li>Set visibility should be retreived visibility
     * </ul>
     */
    public void testSetVisible()
    {
        // Make sure that they keys have been added to the
        // current HEventGroup
        HEventGroup keyEvents = scene.getKeyEvents();
        keyEvents.addKey(KeyEvent.VK_ENTER);
        scene.setKeyEvents(keyEvents);

        assertTrue("Visibility should default to false", !scene.isVisible());

        final boolean key[] = new boolean[1];
        scene.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                key[0] = true;
            }
        });

        // set and retrieve (true)
        scene.setVisible(true);
        assertTrue("Set visibility should be retreived visibility", scene.isVisible());

        // check keyboard focus
        waitForRepaint(scene);
        key[0] = false;

        mouseMove(scene);
        keyClick(KeyEvent.VK_ENTER);
        assertTrue("Keyboard focus should not have been given implicitly to scene", !key[0]);

        // set and retrieve (false)
        scene.setVisible(false);
        assertTrue("Set visibility should be retreived visibility", !scene.isVisible());
    }

    /**
     * Test show()
     * <ul>
     * <li>Ensure made visible
     * <li>Ensure NO implicit focus request
     * <li>Ensure brought to front
     * </ul>
     */
    public void testShow()
    {
        // Make sure that they keys have been added to the
        // current HEventGroup
        HEventGroup keyEvents = scene.getKeyEvents();
        keyEvents.addKey(KeyEvent.VK_ENTER);
        scene.setKeyEvents(keyEvents);

        assertTrue("Visibility should default to false", !scene.isVisible());

        final boolean key[] = new boolean[1];
        scene.addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                key[0] = true;
            }
        });

        // set and retrieve (true)
        scene.show();
        assertTrue("Show should make the scene visible", scene.isVisible());

        // check keyboard focus
        waitForRepaint(scene);
        key[0] = false;

        mouseMove(scene);
        keyClick(KeyEvent.VK_ENTER);
        assertTrue("Keyboard focus should not have been given implicitly to scene", !key[0]);

        /* !!!FINISH!!! */
        // How to test in front?
        // Have multiple scenes and ensure they overlap?
        // fail("Unfinished test - bring to front");
    }

    /**
     * Tests isDoubleBuffered().
     */
    public void xxxtestDoubleBuffered()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests isOpaque().
     * 
     * @see #testBackgroundMode()
     */
    public void xxxtestOpaque()
    {
        fail("Unimplemented test");
    }

    /**
     * Test addWindowListener()/removeWindowListener()/processWindowEvent()
     * <ul>
     * <li>Ensure that WindowListener can be added
     * <li>Ensure that WindowEvents are recieved
     * <li>Ensure that WindowEvents are NOT recieved after removal
     * <li>Ensure that multiple listeners work
     * <li>Ensure that removing a listener which hasn't been added doesn't cause
     * problems
     * </ul>
     */
    public void testWindowListener()
    {
        Frame frame = new Frame();
        SceneListener listener = new SceneListener();

        scene.addWindowListener(listener);

        // do some window manipulation stuff
        int[] sendEvents = new int[] { WindowEvent.WINDOW_OPENED, WindowEvent.WINDOW_ACTIVATED,
                WindowEvent.WINDOW_DEICONIFIED, WindowEvent.WINDOW_ICONIFIED, WindowEvent.WINDOW_DEACTIVATED,
                WindowEvent.WINDOW_CLOSING, WindowEvent.WINDOW_CLOSED };

        int i;
        for (i = 0; i < sendEvents.length; i++)
        {
            scene.processWindowEvent(new WindowEvent2(scene, frame, sendEvents[i]));
        }

        // check results
        Vector events = listener.getEvents();

        assertEquals("not all window events were received", sendEvents.length, events.size());

        for (i = 0; i < sendEvents.length; i++)
        {
            assertEquals("events not recieved in correct order", sendEvents[i],
                    ((WindowEvent) events.elementAt(i)).getID());
        }

        // clear list
        listener.clearEvents();

        // add another listener
        SceneListener secondListener = new SceneListener();
        scene.addWindowListener(secondListener);

        // do some window manipulation stuff
        for (i = 0; i < sendEvents.length; i++)
        {
            scene.processWindowEvent(new WindowEvent2(scene, frame, sendEvents[i]));
        }

        // ensure that the two event lists have the same number of elements
        assertEquals("Multiple window listeners not supported correctly", listener.getEvents().size(),
                secondListener.getEvents().size());

        // ensure that one added twice is called twice
        listener.clearEvents();
        secondListener.clearEvents();
        scene.addWindowListener(listener);
        for (i = 0; i < sendEvents.length; i++)
        {
            scene.processWindowEvent(new WindowEvent2(scene, frame, sendEvents[i]));
        }
        assertEquals("Duplicate window listeners not supported correctly", secondListener.getEvents().size() * 2,
                listener.getEvents().size());

        // remove both listeners
        scene.removeWindowListener(listener);
        scene.removeWindowListener(listener);
        scene.removeWindowListener(secondListener);

        // try to remove one that hasn't been added
        scene.removeWindowListener(new SceneListener());

        // clear list
        listener.clearEvents();

        // do some window manipulation stuff
        for (i = 0; i < sendEvents.length; i++)
        {
            scene.processWindowEvent(new WindowEvent2(scene, frame, sendEvents[i]));
        }

        // ensure that the listeners no longer recieve events
        assertEquals("Listeners shouldn't recieve events after being removed", listener.getEvents().size(), 0);
    }

    /**
     * Tests getFocusOwner().
     * <ul>
     * <li>Returns the component in this scene which has focus
     * <li>Will be null if the scene is NOT active or no components have focus
     * </ul>
     */
    public void testFocusOwner()
    {
        assertNull("There should be no focus owner given no components", scene.getFocusOwner());
        scene.setVisible(true);
        assertNull("There should be no focus owner given no components", scene.getFocusOwner());
        scene.setVisible(false);

        HText text = new HText("Hello");
        text.setSize(100, 100);
        scene.add(text);
        scene.show();
        text.requestFocus();
        delay(100); // kludge
        assertSame("Unexpected focus owner", text, scene.getFocusOwner());

        scene.setActive(false);
        assertNull("There should be no focus owner for inactive scene", scene.getFocusOwner());
    }

    /**
     * Tests dispose().
     * <ul>
     * <li>Calling dispose() again should have no effect. (At least doesn't
     * throw an exception).
     * <li>After calling dispose, should not be able to do anything with the
     * scene.
     * </ul>
     */
    public void testDispose()
    {
        scene.dispose();
        try
        {
            scene.dispose();
        }
        catch (Exception ignored)
        {
            fail("Calling HScene.dispose() more than once should " + "have no effect");
        }
        try
        {
            scene.setVisible(true);
            fail("Exception should be thrown given usage of HScene following" + "disposal");
        }
        catch (IllegalStateException ignored)
        {
        }
    }

    /**
     * Test addShortcut/removeShortcut().
     * <ul>
     * <li>Test that added shortcuts get called (i.e., receive an HActionEvent)
     * <li>A shortcut is added ONLY if the HActionable is a sub-component
     * <li>Test that removed shortcuts do NOT get called
     * <li>A key of VK_UNDEFINED should not be added
     * <li>A key is associated with a maximum of one HActionable shortcut
     * (setting multiple times removes the previous)
     * <li>An HActionable shortcut is associated with at most one key (setting
     * multiple times removes the previous)
     * <li>It is the application's responsibility to ensure that the keyEvents
     * can be generated by this platform and this scene can receive them (how to
     * test?) !!!FINISH!!!
     * </ul>
     */
    public void testShortcuts()
    {
        Shortcut sc = new Shortcut();

        // Make sure that they keys have been added to the
        // current HEventGroup
        HEventGroup keyEvents = scene.getKeyEvents();
        keyEvents.addKey(KeyEvent.VK_A);
        keyEvents.addKey(KeyEvent.VK_B);
        scene.setKeyEvents(keyEvents);

        assertTrue("addShortcut should be unsuccessful " + "if shortcut is not component of scene", !scene.addShortcut(
                KeyEvent.VK_A, sc));
        checkShortcut("Cannot add a shortcut if not a component of a scene", sc, KeyEvent.VK_UNDEFINED);

        scene.add(sc);
        assertTrue("addShortcut should be unsuccessful given VK_UNDEFINED", !scene.addShortcut(KeyEvent.VK_UNDEFINED,
                sc));
        checkShortcut("No shortcut can be defined for VK_UNDEFINED", null, KeyEvent.VK_UNDEFINED);
        // check that shortcut is called
        assertTrue("addShortcut should be successful", scene.addShortcut(KeyEvent.VK_A, sc));
        checkShortcut("Set shortcut should be retrieved shortcut", sc, KeyEvent.VK_A);
        scene.setVisible(true); // should receive focus implicitly
        sc.called = false;
        keyClick(KeyEvent.VK_A);
        assertEquals("Keyboard shortcut should've been called", true, sc.called);

        // check that shortcut is associated with only one key
        assertTrue("addShortcut should be successful", scene.addShortcut(KeyEvent.VK_B, sc));
        checkShortcut("Shortcut can be associated with only one key", null, KeyEvent.VK_A);
        checkShortcut("Shortcut should've moved to new key", sc, KeyEvent.VK_B);
        // check that key is associated with only on shortcut
        Shortcut sc2 = new Shortcut();
        scene.add(sc2);
        assertTrue("addShortcut should be successful", scene.addShortcut(KeyEvent.VK_B, sc2));
        checkShortcut("Keycode can have only one shortcut", sc, KeyEvent.VK_UNDEFINED);
        checkShortcut("Shortcut should be defined", sc2, KeyEvent.VK_B);
        scene.removeShortcut(KeyEvent.VK_B);
        scene.addShortcut(KeyEvent.VK_A, sc);

        // check that shortcut for non-contained component is NOT called
        scene.remove(sc);
        sc.called = false;
        keyClick(KeyEvent.VK_A);
        assertEquals("Keyboard shortcut should NOT have been called", false, sc.called);

        // check that shortcut is not called after removal
        scene.add(sc);
        scene.removeShortcut(KeyEvent.VK_A);
        checkShortcut("Shortcut should be removed", sc, KeyEvent.VK_UNDEFINED);
        sc.called = false;
        keyClick(KeyEvent.VK_A);
        assertEquals("Keyboard shortcut should NOT have been called", false, sc.called);

        // check that shortcut is not called after scene dispose
        assertTrue("addShortcut should be successful", scene.addShortcut(KeyEvent.VK_B, sc));
        checkShortcut("Set shortcut should be retrieved shortcut", sc, KeyEvent.VK_B);
        scene.setVisible(false);
        HSceneFactory.getInstance().dispose(scene);
        sc.called = false;
        keyClick(KeyEvent.VK_B);
        delay(100); // kludge
        scene = null;
        assertEquals("Keyboard shortcut should NOT have been called", false, sc.called);
    }

    /**
     * Tests that the given shortcut/keycode combination exists.
     */
    public void checkShortcut(String msg, HActionable shortcut, int keycode)
    {
        assertSame("Unexpected shortcut: " + msg, (keycode == KeyEvent.VK_UNDEFINED) ? null : shortcut,
                scene.getShortcutComponent(keycode));
        if (shortcut != null) assertEquals("Unexpected keycode: " + msg, keycode, scene.getShortcutKeycode(shortcut));
    }

    /**
     * Test enableShortcuts()/isEnableShortcuts().
     * <ul>
     * <li>Set enabled should be retrieved enabled
     * <li>Ensure that enabling/disabling works
     * </ul>
     */
    public void testEnableShortcuts()
    {
        // Make sure that they keys have been added to the
        // current HEventGroup
        HEventGroup keyEvents = scene.getKeyEvents();
        keyEvents.addKey(KeyEvent.VK_D);
        scene.setKeyEvents(keyEvents);

        // shortcuts should be enabled by default
        assertTrue("Shortcuts should be enabled by default", scene.isEnableShortcuts());

        // set enabled should be retrieved enabled
        scene.enableShortcuts(true);
        assertTrue("Shortcuts should be enabled", scene.isEnableShortcuts());
        scene.enableShortcuts(false);
        assertTrue("Shortcuts should be disabled", !scene.isEnableShortcuts());
        scene.enableShortcuts(true);
        assertTrue("Shortcuts should be enabled", scene.isEnableShortcuts());

        // check that enabling/disabling works
        Shortcut sc = new Shortcut();
        scene.add(sc);
        scene.addShortcut(KeyEvent.VK_D, sc);
        scene.setVisible(true); // should receive focus implicitly

        sc.called = false;
        scene.enableShortcuts(false);
        keyClick(KeyEvent.VK_D);
        assertEquals("Keyboard shortcut should not have been called (disabled)", false, sc.called);

        sc.called = false;
        scene.enableShortcuts(true);
        keyClick(KeyEvent.VK_D);
        assertEquals("Keyboard shortcut should've been called (enabled)", true, sc.called);

        sc.called = false;
        scene.enableShortcuts(false);
        keyClick(KeyEvent.VK_D);
        assertEquals("Keyboard shortcut should not have been called (disabled)", false, sc.called);

        scene.removeShortcut(KeyEvent.VK_D);
    }

    /**
     * Test getShortcutKeycode()/getAllShortcutKeycodes()
     * <ul>
     * <li>Test that added shortcuts can be retrieved
     * </ul>
     */
    public void testShortcutKeycodes()
    {
        assertNotNull("The shortcut keycodes array should be of zero length, not null", scene.getAllShortcutKeycodes());
        assertEquals("The shortcut keycodes array should be of zero length", 0, scene.getAllShortcutKeycodes().length);

        int keys[] = { KeyEvent.VK_A, KeyEvent.VK_HOME, HRcEvent.VK_COLORED_KEY_1, };
        HActionable shortcuts[] = new Shortcut[keys.length];
        for (int i = 0; i < shortcuts.length; ++i)
            shortcuts[i] = new Shortcut();

        for (int i = 0; i < keys.length; ++i)
        {
            scene.add((Component) shortcuts[i]);
            scene.addShortcut(keys[i], shortcuts[i]);
        }

        for (int i = 0; i < keys.length; ++i)
        {
            assertEquals("Set shortcut should be retrievable (" + i + ")", keys[i],
                    scene.getShortcutKeycode(shortcuts[i]));
        }

        int keycodes[] = scene.getAllShortcutKeycodes();

        assertNotNull("Should be able to get all shortcut keys", keycodes);
        assertEquals("Incorrect number of shortcut keycodes retrieved", keys.length, keycodes.length);

        BitSet keySet = new BitSet();
        for (int i = 0; i < keycodes.length; ++i)
            keySet.set(keycodes[i]);
        for (int i = 0; i < keys.length; ++i)
        {
            assertTrue("A set shortcut keycode was not retrieved (" + i + ")", keySet.get(keys[i]));
        }

        Shortcut tmp = new Shortcut();
        scene.add(tmp);
        scene.addShortcut(keys[0], tmp);
        assertEquals("A replaced shortcut should not be mapped", KeyEvent.VK_UNDEFINED,
                scene.getShortcutKeycode(shortcuts[0]));
        assertEquals("The replacing shortcut should map to the right key", keys[0], scene.getShortcutKeycode(tmp));

        scene.removeShortcut(keys[1]);
        assertEquals("A removed shortcut should not be mapped", KeyEvent.VK_UNDEFINED,
                scene.getShortcutKeycode(shortcuts[1]));

        assertEquals("A non-added shortcut should not be mapped", KeyEvent.VK_UNDEFINED,
                scene.getShortcutKeycode(new Shortcut()));
    }

    /**
     * Test getPixelCoordinatesHScreenRectangle().
     * <ul>
     * <li>validate HScreenRectangle returned for various components
     * </ul>
     */
    public void getPixelCoordinates()
    {
        Dimension d = scene.getSize();
        Rectangle r = new Rectangle();

        r.setBounds(0, 0, d.width, d.height);
        assertEquals("Full-scene HScreenRectangle should be (0,0,1,1)", new HScreenRectangle(0, 0, 1, 1),
                scene.getPixelCoordinatesHScreenRectangle(r));

        // Try all 4 quarters and center
        r.setBounds(0, 0, d.width / 2, d.height / 2);
        assertEquals("NW HScreenRectangle should be (0,0,0.5,0.5)", new HScreenRectangle(0, 0, 0.5f, 0.5f),
                scene.getPixelCoordinatesHScreenRectangle(r));

        r.setBounds(d.width / 2, 0, d.width / 2, d.height / 2);
        assertEquals("NE HScreenRectangle should be (0.5,0,0.5,0.5)", new HScreenRectangle(0.5f, 0, 0.5f, 0.5f),
                scene.getPixelCoordinatesHScreenRectangle(r));

        r.setBounds(0, d.height / 2, d.width / 2, d.height / 2);
        assertEquals("SW HScreenRectangle should be (0,0.5,0.5,0.5)", new HScreenRectangle(0, 0.5f, 0.5f, 0.5f),
                scene.getPixelCoordinatesHScreenRectangle(r));

        r.setBounds(d.width / 2, d.height / 2, d.width / 2, d.height / 2);
        assertEquals("SE HScreenRectangle should be (0.5,0.5,0.5,0.5)", new HScreenRectangle(0.5f, 0.5f, 0.5f, 0.5f),
                scene.getPixelCoordinatesHScreenRectangle(r));

        r.setBounds(d.width / 4, d.height / 4, d.width / 2, d.height / 2);
        assertEquals("CENTER HScreenRectangle should be (0.25,0.25,0.5,0.5)", new HScreenRectangle(0.25f, 0.25f, 0.5f,
                0.5f), scene.getPixelCoordinatesHScreenRectangle(r));
    }

    /**
     * Test get/setBackgroundMode().
     * <ul>
     * <li>The set background mode should be the retreived background mode
     * <li>A mode of BACKGROUND_FILL means that isOpaque should return true
     * <li>Invalid values should result in IllegalArgumentException
     * <li>The background mode should be implemented correctly!
     * </ul>
     */
    public void testBackgroundMode()
    {
        scene.setBackground(Color.red);
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        assertEquals("The set BG mode should be the retrieved mode", HScene.BACKGROUND_FILL, scene.getBackgroundMode());
        assertTrue("BACKGROUND_FILL should imply isOpaque()==true", scene.isOpaque());
        scene.setVisible(true);
        TestSupport.checkDisplay(scene, "Background mode fill", new String[] { "Is the background filled with "
                + "the color " + scene.getBackground().toString() + "?" }, "bgm_fill", this);
        scene.setVisible(false);

        scene.setBackgroundMode(HScene.NO_BACKGROUND_FILL);
        assertEquals("The set BG mode should be the retrieved mode", HScene.NO_BACKGROUND_FILL,
                scene.getBackgroundMode());
        scene.setVisible(true);
        TestSupport.checkDisplay(scene, "No Background mode fill",
                new String[] { "The background should NOT be filled " + "with the color "
                        + scene.getBackground().toString() + "?" }, "bgm_nofill", this);
        scene.setVisible(false);
    }

    private static final int[] renderModes = { HScene.IMAGE_NONE, HScene.IMAGE_STRETCH, HScene.IMAGE_CENTER,
            HScene.IMAGE_TILE };

    private static final String[] renderModeNames = { "IMAGE_NONE", "IMAGE_STRETCH", "IMAGE_CENTER", "IMAGE_TILE" };

    /**
     * Test get/setRenderMode().
     * <ul>
     * <li>The set image mode should be the retreived image mode
     * <li>If the mode is supported (returns true), then the mode should be
     * implemented correctly!
     * </ul>
     */
    public void testRenderMode()
    {
        for (int i = 0; i < renderModes.length; ++i)
        {
            int last = scene.getRenderMode();
            if (!scene.setRenderMode(renderModes[i]))
            {
                assertEquals("Unsupported render modes should not be set", last, scene.getRenderMode());
            }
            else
            {
                assertEquals("The set render mode should be the retrieved " + "render mode", renderModes[i],
                        scene.getRenderMode());

                // Without a background image
                scene.setBackgroundImage(null);
                scene.setVisible(true);
                TestSupport.checkDisplay(scene, "Render mode", new String[] { "No background image should be "
                        + "displayed" }, renderModeNames[i] + "_null", this);
                scene.setVisible(false);

                scene.setBackgroundImage(TestSupport.getArrow(2));
                scene.setVisible(true);
                TestSupport.checkDisplay(scene, "Render mode", new String[] { "Image should be displayed with the '"
                        + renderModeNames[i] + "' mode" }, renderModeNames[i], this);
                scene.setVisible(false);
            }
        }
    }

    /**
     * Test getBackgroundImage()/setBackgroundImage().
     * <ul>
     * <li>The set bg image should be the retrieved bg image.
     * </ul>
     */
    public void testBackgroundImage()
    {
        Image img1 = new HVisibleTest.EmptyImage();
        scene.setBackgroundImage(img1);
        assertSame("Set bg image should be retrieved bg image", img1, scene.getBackgroundImage());

        Image img2 = new HVisibleTest.EmptyImage();
        scene.setBackgroundImage(img2);
        assertSame("Set bg image should be retrieved bg image", img2, scene.getBackgroundImage());

        scene.setBackgroundImage(null);
        assertNull("Set bg image should be retrieved bg image", scene.getBackgroundImage());
    }

    /**
     * Test getSceneTemplate().
     * <ul>
     * <li>Validate the entries in the template
     * </ul>
     */
    public void testSceneTemplate()
    {
        HSceneTemplate t = scene.getSceneTemplate();
        assertNotNull("An HSceneTemplate should be returned when requested", t);

        // Check size and position of scene template against actual values
        Dimension d = scene.getSize();
        Point p = scene.getLocation();

        assertEquals("Pixel dimension in HSceneTemplate is incorrect", d,
                t.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_DIMENSION));
        assertEquals("Pixel location in HSceneTemplate is incorrect", p,
                t.getPreferenceObject(HSceneTemplate.SCENE_PIXEL_LOCATION));
    }

    /**
     * Tests getParent()
     * <ul>
     * <li>Should throw a runtime exception.
     * </ul>
     */
    public void testParent()
    {
        boolean getParentAllowed = TestSupport.getProperty("HScene.getParentAllowed", false);
        try
        {
            Container c = scene.getParent();
            if (!getParentAllowed) fail("HScene.getParent() should throw an exception");
        }
        catch (Exception e)
        {
            // expected
            if (getParentAllowed) fail("HScene.getParent() should be allowed");
        }
    }

    /**
     * Tests addAfter().
     * <ul>
     * <li>Add a new component after an existing
     * <li>Add an existing component after an existing
     * <li>Add a new component after a non-existing
     * <li>Check return values
     * </ul>
     */
    public void testAddAfter()
    {
        HComponentOrderingTest.testAddAfter(scene);
    }

    /**
     * Tests addBefore().
     * <ul>
     * <li>Add a new component before an existing
     * <li>Add an existing component before an existing
     * <li>Add a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public void testAddBefore()
    {
        HComponentOrderingTest.testAddBefore(scene);
    }

    /**
     * Tests push().
     * <ul>
     * <li>Push a component past the end
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPush()
    {
        HComponentOrderingTest.testPush(scene);
    }

    /**
     * Tests pop().
     * <ul>
     * <li>Pop a component past the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPop()
    {
        HComponentOrderingTest.testPop(scene);
    }

    /**
     * Tests popToFront().
     * <ul>
     * <li>Pop components to the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPopToFront()
    {
        HComponentOrderingTest.testPopToFront(scene);
    }

    /**
     * Tests pushToBack().
     * <ul>
     * <li>Push components to the back
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPushToBack()
    {
        HComponentOrderingTest.testPushToBack(scene);
    }

    /**
     * Tests popInFrontOf().
     * <ul>
     * <li>Pop an existing component before an existing
     * <li>Attempt to pop a new component before an existing
     * <li>Pop an existing component before a non-existing
     * <li>Attempt to pop a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public void testPopInFrontOf()
    {
        HComponentOrderingTest.testPopInFrontOf(scene);
    }

    /**
     * Tests pushBehind().
     * <ul>
     * <li>Push an existing component before an existing
     * <li>Attempt to push a new component before an existing
     * <li>Push an existing component before a non-existing
     * <li>Attempt to push a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public void testPushBehind()
    {
        HComponentOrderingTest.testPushBehind(scene);
    }

    /**
     * Tests get|setKeyEvents.
     * <ul>
     * <li>Default set of key codes should be returned if set is never called.
     * <li>The set value should be the returned value (same instance).
     * </ul>
     * The semantics should be tested, but how?
     */
    public void testKeyEvents()
    {
        assertNotNull("KeyEvents should represent the default key events", scene.getKeyEvents());

        HEventGroup eg = new HEventGroup();
        scene.setKeyEvents(eg);
        assertSame("The set KeyEvents should be the retrieved", eg, scene.getKeyEvents());

        eg = new HEventGroup();
        scene.setKeyEvents(eg);
        assertSame("The set KeyEvents should be the retrieved", eg, scene.getKeyEvents());

        eg = new HEventGroup();
        eg.addAllNumericKeys();
        eg.removeAllColourKeys();
        eg.removeAllArrowKeys();
        scene.setKeyEvents(eg);
        assertSame("The set KeyEvents should be the retrieved", eg, scene.getKeyEvents());

        /* !!!FINISH!!!! */
        /* Test the semantics of this... */
        // fail("Unfinished test - semantics of setKeyEvents()");
    }

    /**
     * Tests setActive().
     * <ul>
     * <li>Should be true by default (cannot test).
     * <li>Changing from false -> true allows focus to be received (nothing
     * else).
     * <li>Changing from true -> false results in focus lost (if had) and
     * WINDOW_DEACTIVATED.
     * <li>If active==false, calling requestFocus() has no effect.
     * </ul>
     */
    public void testActive()
    {
        HText text = new HText("Hello");
        text.setSize(100, 100);
        scene.add(text);
        scene.show();
        text.requestFocus();
        delay(100); // kludge
        assertSame("Unexpected focus owner", text, scene.getFocusOwner());

        final boolean[] called = new boolean[1];
        scene.addWindowListener(new WindowAdapter()
        {
            public void windowDeactivated(WindowEvent e)
            {
                called[0] = true;
            }
        });

        scene.setActive(false);
        delay(100); // kludge
        assertNull("The scene should have lost focus", scene.getFocusOwner());
        assertTrue("WindowDeactivated should've been called for an inactivated " + "scene", called[0]);

        scene.requestFocus();
        text.requestFocus();
        assertNull("Inactive scene should not be able to get focus", scene.getFocusOwner());
    }

    /**
     * Tests requestFocus(). This isn't really specified to be implemented by
     * HScene. However, previous specs said that requestfocus implicitly made
     * the scene visible; this should not longer be the case.
     */
    public void testRequestFocus()
    {
        scene.setVisible(false); /* Just to be sure. */
        scene.requestFocus();
        delay(100); // kludge
        assertTrue("Requesting focus should not result in implicit " + "showing of a component", !scene.isVisible());
    }

    public static class Shortcut extends HComponent implements HActionable
    {
        public boolean called;

        public Shortcut()
        {
            enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK
                    | AWTEvent.ACTION_EVENT_MASK);
            setVisible(false);
        }

        public void processHActionEvent(HActionEvent evt)
        {
            called = true;
        }

        // HNavigable
        public void setMove(int key, HNavigable c)
        {
        }

        public HNavigable getMove(int key)
        {
            return null;
        }

        public void setFocusTraversal(HNavigable u, HNavigable d, HNavigable l, HNavigable r)
        {
        }

        public int[] getNavigationKeys()
        {
            return null;
        }

        public boolean isSelected()
        {
            return false;
        }

        public void setGainFocusSound(HSound s)
        {
        }

        public void setLoseFocusSound(HSound s)
        {
        }

        public HSound getGainFocusSound()
        {
            return null;
        }

        public HSound getLoseFocusSound()
        {
            return null;
        }

        public void processHFocusEvent(HFocusEvent evt)
        {
        }

        public void addHFocusListener(HFocusListener l)
        {
        }

        public void removeHFocusListener(HFocusListener l)
        {
        }

        // HActionable
        public void addHActionListener(HActionListener l)
        {
        }

        public void removeHActionListener(HActionListener l)
        {
        }

        public HSound getActionSound()
        {
            return null;
        }

        public void setActionSound(HSound sound)
        {
        }

        public void setActionCommand(String str)
        {
        }

        public String getActionCommand()
        {
            return null;
        }
    }

    public class SceneListener extends WindowAdapter
    {
        Vector events;

        public SceneListener()
        {
            events = new Vector();
        }

        public void windowOpened(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowClosing(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowClosed(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowIconified(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowDeiconified(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowActivated(WindowEvent e)
        {
            events.addElement(e);
        }

        public void windowDeactivated(WindowEvent e)
        {
            events.addElement(e);
        }

        public Vector getEvents()
        {
            return (events);
        }

        public void clearEvents()
        {
            events.removeAllElements();
        }
    }

    public static class WindowEvent2 extends WindowEvent
    {
        /**
         * Constructs a new <code>WindowEvent</code> with the given
         * <code>id</code> and <code>scene</code> as the source. The
         * <code>Window</code> parameter is necessary for proper construction,
         * but is never referenced again.
         * 
         * @param scene
         *            the event source
         * @param w
         *            a non-<code>null</code> <code>Window</code> is required to
         *            construct the <code>WindowEvent</code>, but is not
         *            referenced after construction
         * @param id
         *            the event id; should be one of
         *            <ul>
         *            <li> <code>WindowEvent.WINDOW_ACTIVATED</code>
         *            <li> <code>WindowEvent.WINDOW_CLOSED</code>
         *            <li> <code>WindowEvent.WINDOW_CLOSING</code>
         *            <li> <code>WindowEvent.WINDOW_DEACTIVATED</code>
         *            <li> <code>WindowEvent.WINDOW_DEICONIFIED</code>
         *            <li> <code>WindowEvent.WINDOW_ICONIFIED</code>
         *            <li> <code>WindowEvent.WINDOW_OPENED</code>
         *            </ul>
         */
        public WindowEvent2(HScene scene, Window w, int id)
        {
            super(w, id);
            source = scene; // forget about the given Window
        }
    }
}
