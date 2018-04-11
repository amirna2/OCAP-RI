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

package org.cablelabs.impl.manager.animation;

import org.cablelabs.impl.manager.animation.*;

import org.cablelabs.test.*;
import java.awt.*;
import org.havi.ui.HAnimation;
import org.havi.ui.HStaticAnimation;
import org.havi.ui.HState;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Enumeration;
import javax.tv.util.TVTimerSpec;

import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.manager.AnimationManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.animation.*;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;

/**
 * Tests {@link #TimerAnimationMgr}.
 * 
 * @author afh
 */
public class TimerAnimationMgrTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public TimerAnimationMgrTest()
    {

    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(TimerAnimationMgrTest.class);
    }

    protected HAnimation hanimation;

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HStaticAnimation
     * <li>implements HNavigable
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(TimerAnimationMgr.class, AnimationMgr.class);
    }

    /**
     * Test that the <i>TimerAnimationMgr</i> properly supports the lifecycle of
     * applications using animations. The test installs a new
     * CallerContextManager and a dedicated CallerContext that allows it to
     * invoke the destroy method on the animations AnimationTimerSpec which
     * implements the CallbackData interface. It verifies that the destroy
     * method properly stops the animation and removes it from the manager's set
     * of animations.
     * 
     */
    public void testTimerAnimationMgr()
    {
        // Replace the caller context manager and setup a dummy caller context.
        replaceCCMgr();

        // Instantiate the animation.
        HAnimation anim = getAnimationandTestConstructor();

        // Verify the correct animation manager is in place.
        AnimationManager manager = (AnimationManager) ManagerManager.getInstance(AnimationManager.class);
        assertTrue("Incorrect animation manager", manager instanceof TimerAnimationMgr);

        anim.start();
        assertTrue("Animation not started", anim.isAnimated());

        // Verify caller context is in place.
        CallerContext cc = ccmgr.getCurrentContext();
        if ((cc instanceof DummyAnimationContext) == false) fail("Incorrect CallerContext in place");

        // Verify call back data is in place.
        CallbackData cbd = ((DummyAnimationContext) cc).getAnimationCallbackData();
        if ((cbd == null) || (cbd instanceof TVTimerSpec) == false) fail("Incorrect CallbackData in place");

        // Call destroy on call back data.
        cbd.destroy(cc);

        // Verify that the animation is now stopped.
        assertFalse("Animation not stopped", anim.isAnimated());

        // Verify the call back data has been removed.
        assertNull("CallbackData not correctly removed", ((DummyAnimationContext) cc).getAnimationCallbackData());

        restoreCCMgr();
    }

    CCMgr ccmgr = null;

    CallerContextManager save;

    /**
     * Method used to replace and save the CallerContextManager.
     */
    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));
        ccmgr.alwaysReturned = new DummyAnimationContext();
    }

    /**
     * Method used to restore the original CallerContextManager.
     */
    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    /**
     * 
     * </ul>
     */
    public HAnimation getAnimationandTestConstructor()
    {
        Image normal[] = new Image[7];
        Image focus[] = new Image[3];
        HAnimation anim;

        checkConstructor("HAnimation(Image img[], " + "int delay, int mode, int repeat, "
                + "int x, int y, int w, int h)", anim = new HAnimation(normal, 0, HAnimation.PLAY_REPEATING,
                HAnimation.REPEAT_INFINITE, 100, 101, 102, 103), normal, normal, 1, HAnimation.PLAY_REPEATING,
                HAnimation.REPEAT_INFINITE, 100, 101, 102, 103, true);

        return anim;

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
        assertEquals(msg + " should be NORMAL_STATE", HState.NORMAL_STATE, a.getInteractionState());
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

    private class DummyAnimationContext extends DummyContext
    {
        CallbackData getAnimationCallbackData()
        {
            Enumeration e = callbackData.elements();
            while (e.hasMoreElements())
            {
                CallbackData cbd = (CallbackData) e.nextElement();
                if (cbd instanceof TVTimerSpec) return cbd;
            }
            return null;
        }
    }

}
