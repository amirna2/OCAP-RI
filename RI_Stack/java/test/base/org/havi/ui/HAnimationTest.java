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

/**
 * Tests {@link #HAnimation}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.5 $, $Date: 2002/11/07 21:14:06 $
 */
public class HAnimationTest extends HStaticAnimationTest
{
    /**
     * Standard constructor.
     */
    public HAnimationTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HAnimationTest.class);
    }

    /**
     * The tested component.
     */
    protected HAnimation hanimation;

    /**
     * Should be overridden to create subclass of HAnimation.
     * 
     * @return the instance of HAnimation to test
     */
    protected HAnimation createHAnimation()
    {
        return new HAnimation();
    }

    /**
     * Overridden to create an HAnimation.
     * 
     * @return the instance of HStaticAnimation to test
     */
    protected HStaticAnimation createHStaticAnimation()
    {
        return (hanimation = createHAnimation());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HStaticAnimation
     * <li>implements HNavigable
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HAnimation.class, HStaticAnimation.class);
        HNavigableTest.testAncestry(HAnimation.class);
    }

    /**
     * Test the 5 constructors of HAnimation.
     * <ul>
     * <li>HAnimation()
     * <li>HAnimation(Image img[], int delay, int mode, int repeat)
     * <li>HAnimation(Image img[], int delay, int mode, int repeat, int x, int
     * y, int w, int h)
     * <li>HAnimation(Image normal[], Image focus[], int delay, int mode, int
     * repeat)
     * <li>HAnimation(Image normal[], Image focus[], int delay, int mode, int
     * repeat, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HAnimationTest.class);

        Image normal[] = new Image[7];
        Image focus[] = new Image[3];

        checkConstructor("HAnimation()", new HAnimation(), null, null, 1, HAnimation.PLAY_REPEATING,
                HAnimation.REPEAT_INFINITE, 0, 0, 0, 0, false);
        checkConstructor("HAnimation(Image img[], " + "int delay, int mode, int repeat)", new HAnimation(normal, 3,
                HAnimation.PLAY_ALTERNATING, 7), normal, normal, 3, HAnimation.PLAY_ALTERNATING, 7, 0, 0, 0, 0, false);
        checkConstructor("HAnimation(Image img[], " + "int delay, int mode, int repeat, "
                + "int x, int y, int w, int h)", new HAnimation(normal, 0, HAnimation.PLAY_REPEATING,
                HAnimation.REPEAT_INFINITE, 100, 101, 102, 103), normal, normal, 1, HAnimation.PLAY_REPEATING,
                HAnimation.REPEAT_INFINITE, 100, 101, 102, 103, true);
        checkConstructor("HAnimation(Image normal[], Image focus[], " + "int delay, int mode, int repeat",
                new HAnimation(normal, focus, 3, HAnimation.PLAY_ALTERNATING, 7), normal, focus, 3,
                HAnimation.PLAY_ALTERNATING, 7, 0, 0, 0, 0, false);
        checkConstructor("HAnimation(Image normal[], Image focus[], " + "int delay, int mode, int repeat, "
                + "int x, int y, int w, int h)", new HAnimation(normal, focus, 0, HAnimation.PLAY_REPEATING, 1, 100,
                101, 102, 103), normal, focus, 1, HAnimation.PLAY_REPEATING, 1, 100, 101, 102, 103, true);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    public void checkConstructor(String msg, HAnimation a, Image normal[], Image focus[], int delay, int mode,
            int repeat, int x, int y, int w, int h, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", a);
        assertEquals(msg + " x-coordinated not initialized correctly", x, a.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, a.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, a.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, a.getSize().height);
        assertEquals(msg + " delay not initialized correctly", delay, a.getDelay());
        assertEquals(msg + " repeat count not initialized correctly", repeat, a.getRepeatCount());
        assertEquals(msg + " playback mode not initialized correctly", mode, a.getPlayMode());
        assertSame(msg + " NORMAL animation Images not initialized correctly", normal,
                a.getAnimateContent(HState.NORMAL_STATE));
        assertSame(msg + " FOCUS animation Images not initialized correctly", focus,
                a.getAnimateContent(HState.FOCUSED_STATE));

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, a.getInteractionState());
        assertNull(msg + " matte should be unassigned", a.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", a.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", a.getBackgroundMode(), a.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // a.getDefaultSize());
            assertEquals(msg + " default size should not be set", a.NO_DEFAULT_SIZE, a.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", a.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", a.getHorizontalAlignment(), a.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", a.getVerticalAlignment(), a.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", a.getResizeMode(), a.RESIZE_NONE);
        assertSame(msg + " default look not used", a.getDefaultLook(), a.getLook());
        assertEquals(msg + " initial position not correct", 0, a.getPosition());
        assertTrue(msg + " should be stopped by default", !a.isAnimated());
        assertNull(msg + " gain focus sound incorrectly initialized", a.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", a.getLoseFocusSound());
    }

    /**
     * Test {set|get}Move/setFocusTraversal
     * <ul>
     * <li>The set move should be the retreived move
     * <li>Setting a move to null should remove the traversal
     * <li>setFocusTraversal should set the correct keys
     * </ul>
     */
    public void testMove()
    {
        HNavigableTest.testMove(hanimation);
    }

    /**
     * Test isSelected
     * <ul>
     * <li>Should be getInteractionState()==FOCUS_STATE
     * </ul>
     */
    public void testSelected()
    {
        HNavigableTest.testSelected(hanimation);
    }

    /**
     * Test {get|set}{Lose|Gain}FocusSound.
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests set{Lose|Gain}Sound(null)
     * <li>Test that the sound is played when the component gains|loses focus
     * </ul>
     */
    public void testFocusSound()
    {
        HNavigableTest.testFocusSound(hanimation);
    }

    /**
     * Tests getNavigationKeys().
     */
    public void testNavigationKeys()
    {
        HNavigableTest.testNavigationKeys(hanimation);
    }

    /**
     * Tests add/removeHFocusListener().
     */
    public void testFocusListener()
    {
        HNavigableTest.testFocusListener(hanimation);
    }

    /**
     * Tests proper state traversal as a result of focus events.
     */
    public void testProcessHFocusEvent()
    {
        HNavigableTest.testProcessHFocusEvent(hanimation);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HAnimateLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HAnimations should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HAnimationTest.class);

        assertSame("Default look should be used", HAnimation.getDefaultLook(), (new HAnimation()).getLook());

        HAnimateLook save = HAnimation.getDefaultLook();

        try
        {
            HAnimateLook look;

            HAnimation.setDefaultLook(look = new HAnimateLook());
            assertSame("Incorrect look retrieved", look, HAnimation.getDefaultLook());
            assertSame("Default look should be used", look, (new HAnimation()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HAnimation.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HAnimation.setDefaultLook(save);
        }
    }

    /**
     * Create an HComponent of the appropriate class type that, in response to
     * HAVi Events, will set the generated[0] element to true.
     * <p>
     * The special component should (where appropriate) override:
     * <ul>
     * <li>processHFocusEvent
     * <li>processHTextEvent
     * <li>processHKeyEvent
     * </ul>
     * <p>
     * This is necessary because HNavigable and HTextValue components are not
     * required to support HFocusListeners.
     * 
     * @param ev
     *            a helper object used to test the event generation
     * @see #testProcessEvent
     */
    protected HComponent createSpecialComponent(final EventCheck ev)
    {
        checkClass(HAnimationTest.class);

        return new HAnimation()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }
        };
    }

    /**
     * Creates an instance of HStaticAnimation which pushes TimeStart objects
     * onto the given startStop Vector when start (true) or stop (false) is
     * called.
     * <p>
     * Should be overridden by subclasses.
     */
    protected HStaticAnimation createSpecialAnimation(final java.util.Vector startStop)
    {
        checkClass(HAnimationTest.class);

        return new HAnimation()
        {
            public void start()
            {
                if (!isAnimated())
                {
                    long time = System.currentTimeMillis();
                    startStop.addElement(new TimeStart(true, time));
                }
                super.start();
            }

            public void stop()
            {
                if (isAnimated())
                {
                    long time = System.currentTimeMillis();
                    startStop.addElement(new TimeStart(false, time));
                }
                super.stop();
            }
        };
    }

}
