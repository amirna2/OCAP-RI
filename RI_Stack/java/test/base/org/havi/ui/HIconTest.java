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
 * Tests {@link #HIcon}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $, $Date: 2002/11/07 21:14:07 $
 */
public class HIconTest extends HStaticIconTest
{
    /**
     * Standard constructor.
     */
    public HIconTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HIconTest.class);
    }

    /**
     * The tested component.
     */
    protected HIcon hicon;

    /**
     * Should be overridden to create subclass of HIcon.
     * 
     * @return the instance of HIcon to test
     */
    protected HIcon createHIcon()
    {
        return new HIcon();
    }

    /**
     * Overridden to create an HIcon.
     * 
     * @return the instance of HStaticIcon to test
     */
    protected HStaticIcon createHStaticIcon()
    {
        return (hicon = createHIcon());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HStaticIcon
     * <li>implements HNavigable
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HIcon.class, HStaticIcon.class);
        HNavigableTest.testAncestry(HIcon.class);
    }

    /**
     * Test the 3 constructors of HIcon.
     * <ul>
     * <li>HIcon()
     * <li>HIcon(Image img)
     * <li>HIcon(Image img, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HIconTest.class);

        Image normal = new EmptyImage();
        Image focused = new EmptyImage();
        checkConstructor("HIcon()", new HIcon(), 0, 0, 0, 0, null, null, false);
        checkConstructor("HIcon(Image img)", new HIcon(normal), 0, 0, 0, 0, normal, normal, false);
        checkConstructor("HIcon(Image img, int x, int y, int w, int h)", new HIcon(normal, 23, 25, 27, 29), 23, 25, 27,
                29, normal, normal, true);
        checkConstructor("HIcon(Image img, Image img, " + "int x, int y, int w, int h)", new HIcon(normal, focused, 23,
                25, 27, 29), 23, 25, 27, 29, normal, focused, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HIcon icon, int x, int y, int w, int h, Image imgNormal, Image imgFocus,
            boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", icon);
        assertEquals(msg + " x-coordinated not initialized correctly", x, icon.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, icon.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, icon.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, icon.getSize().height);
        assertSame(msg + " NORMAL Image not initialized correctly", imgNormal,
                icon.getGraphicContent(HState.NORMAL_STATE));
        assertSame(msg + " FOCUSED Image not initialized correctly", imgFocus,
                icon.getGraphicContent(HState.FOCUSED_STATE));

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, icon.getInteractionState());
        assertNull(msg + " matte should be unassigned", icon.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", icon.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", icon.getBackgroundMode(), icon.NO_BACKGROUND_FILL);
        if (!defaultSize)
            assertEquals(msg + " default size should not be set", icon.NO_DEFAULT_SIZE, icon.getDefaultSize()); // changed
                                                                                                                // siegfried@heintze.com
                                                                                                                // Dec
                                                                                                                // 6,
                                                                                                                // 2006
        // assertNull(msg+" default size should not be set",
        // icon.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", icon.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", icon.getHorizontalAlignment(),
                icon.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", icon.getVerticalAlignment(), icon.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", icon.getResizeMode(), icon.RESIZE_NONE);
        assertSame(msg + " default look not used", HIcon.getDefaultLook(), icon.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", icon.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", icon.getLoseFocusSound());
        assertEquals(msg + " border mode not initialized correctly", true, icon.getBordersEnabled());
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
        HNavigableTest.testMove(hicon);
    }

    /**
     * Test isSelected
     * <ul>
     * <li>Should be getInteractionState()==FOCUSED_STATE
     * </ul>
     */
    public void testSelected()
    {
        HNavigableTest.testSelected(hicon);
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
        HNavigableTest.testFocusSound(hicon);
    }

    /**
     * Tests getNavigationKeys().
     */
    public void testNavigationKeys()
    {
        HNavigableTest.testNavigationKeys(hicon);
    }

    /**
     * Tests add/removeHFocusListener().
     */
    public void testFocusListener()
    {
        HNavigableTest.testFocusListener(hicon);
    }

    /**
     * Tests proper state traversal as a result of focus events.
     */
    public void testProcessHFocusEvent()
    {
        HNavigableTest.testProcessHFocusEvent(hicon);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HGraphicLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HIcons should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HIconTest.class);

        assertSame("Default look should be used", HIcon.getDefaultLook(), (new HIcon()).getLook());

        HGraphicLook save = HIcon.getDefaultLook();

        try
        {
            HGraphicLook look;

            HIcon.setDefaultLook(look = new HGraphicLook());
            assertSame("Incorrect look retrieved", look, HIcon.getDefaultLook());
            assertSame("Default look should be used", look, (new HIcon()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HIcon.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HIcon.setDefaultLook(save);
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
        checkClass(HIconTest.class);

        return new HIcon()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }
        };
    }
}
