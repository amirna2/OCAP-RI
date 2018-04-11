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

import org.havi.ui.event.*;
import org.cablelabs.test.*;
import junit.framework.*;
import java.awt.*;

/**
 * Tests {@link #HComponent}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.11 $, $Date: 2002/11/07 21:14:06 $
 */
public class HComponentTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HComponentTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HComponentTest.class);
    }

    /**
     * Check that this class is the correct class.
     */
    protected final void checkClass(Class cl)
    {
        assertEquals("The calling method should be overridden by a subclass", cl, getClass());
    }

    /**
     * Returns the class currently being tested.
     */
    protected Class getTestedClass()
    {
        return hcomponent.getClass();
    }

    /** The tested component. */
    protected HComponent hcomponent;

    /**
     * Should be overridden to create subclass of HComponent.
     * 
     * @return the instance of HComponent to test
     */
    protected HComponent createHComponent()
    {
        return new HComponent()
        {
        };
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hcomponent = createHComponent();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Component
     * <li>implements HMatteLayer
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HComponentTest.class);

        TestUtils.testExtends(HComponent.class, Component.class);
        HMatteLayerTest.testAncestry(HComponent.class);
    }

    /**
     * Test the 2 constructors of HComponent.
     * <ul>
     * <li>HComponent()
     * <li>HComponent(int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HComponentTest.class);

        checkConstructor("HComponent()", new HComponent()
        {
        }, 0, 0, 0, 0);
        checkConstructor("HComponent(int,int,int,int)", new HComponent(10, 10, 255, 255)
        {
        }, 10, 10, 255, 255);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HComponent v, int x, int y, int w, int h)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", v);
        assertEquals(msg + " x-coordinated not initialized correctly", x, v.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, v.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, v.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, v.getSize().height);

        // Check variables not exposed in constructors
        assertNull(msg + " matte should be unassigned", v.getMatte());
        assertTrue(msg + " enabled should be true", v.isEnabled());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(getTestedClass());
    }

    /**
     * Tests isDoubleBuffered(). Should return false.
     */
    public void testDoubleBuffered()
    {
        try
        {
            hcomponent.isDoubleBuffered();
            fail("Expected exception - isDoubleBuffered shall not be used per MHP 11.4.1.2");
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Tests isEnabled/setEnabled()
     * <ul>
     * <li>Set value should be returned.
     * <li>Enabled by default. (In constructor?)
     * <li>When disabled, only focus events are generated/received (actually
     * tested in testProcessEvent().
     * </ul>
     */
    public void testEnabled()
    {
        assertTrue("Component should be enabled by default", hcomponent.isEnabled());

        for (int i = 0; i < 4; ++i)
        {
            boolean value = (i & 1) != 0;

            hcomponent.setEnabled(value);
            assertEquals("isEnabled() should return previous setEnabled value", value, hcomponent.isEnabled());
        }
    }

    /**
     * Tests isOpaque. Should return false.
     */
    public void testOpaque() throws Exception
    {
        assertTrue("Double-buffering shouldn't be on for this component", !hcomponent.isOpaque());
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
     */
    protected HComponent createSpecialComponent(final EventCheck eventCheck)
    {
        fail("createSpecialComponent() should be overridden by " + this.getClass().getName());
        return null;
    }

    /**
     * Tests processEvent().
     * <ul>
     * <li>Generates HAVi events depending upon the type of event and type of
     * component.
     * <li>Tests generation of HFocusEvent, HActionEvent, HKeyEvent, HTextEvent,
     * HAdjustmentEvent, and HItemEvent.
     * </ul>
     */
    public void testProcessEvent()
    {
        doTestProcessEvent(true);
        doTestProcessEvent(false);
    }

    private void doTestProcessEvent(boolean enabled)
    {
        HaviTestToolkit tk = HaviTestToolkit.getToolkit();
        final AWTEvent[] theEvent = new AWTEvent[1];
        AWTEvent[] events = null;
        HComponent special = null; // used when cannot listen...
        String should = enabled ? "should've" : "should not have";

        if (hcomponent instanceof HNavigationInputPreferred)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HFocusEvent) theEvent[0] = e;
                }
            });
            special.setEnabled(enabled);

            events = tk.createFocusEvent(special, true);
            assertNotNull("Do not know how to generate HFocusEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertNotNull("HFocusEvent FOCUS_GAINED should've been generated " + "from " + events[i], theEvent[0]);
                assertEquals("HFocusEvent FOCUS_GAINED should've been generated " + "from " + events[i],
                        HFocusEvent.FOCUS_GAINED, theEvent[0].getID());
            }
            events = tk.createFocusEvent(special, false);
            assertNotNull("Do not know how to generate HFocusEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertNotNull("HFocusEvent FOCUS_LOST should've been generated " + "from " + events[i], theEvent[0]);
                assertEquals("HFocusEvent FOCUS_LOST should've been generated " + "from " + events[i],
                        HFocusEvent.FOCUS_LOST, theEvent[0].getID());
            }
            events = tk.createFocusTransferEvent(special);
            assertNotNull("Do not know how to generate focus transfer events", events);
            // Make sure that HKeyEvent's aren't generated...
            // HNavigationInputPreferred says so...
            // (no HKeyEvents if keys map to FOCUS_TRANSFER events.)
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HFocusEvent) theEvent[0] = e;
                    assertTrue("HKeyEvent's should not be generated when " + "FOCUS_TRANSFER events are",
                            !(e instanceof HKeyEvent));
                }
            });
            special.setEnabled(enabled);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertTrue("HFocusEvent FOCUS_TRANSFER should've been generated " + "from " + events[i],
                        theEvent[0] != null);
                assertEquals("HFocusEvent FOCUS_TRANSFER should've been generated " + "from " + events[i],
                        HFocusEvent.FOCUS_TRANSFER, theEvent[0].getID());
            }
        }
        if (hcomponent instanceof HActionInputPreferred)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HActionEvent) theEvent[0] = e;
                }
            });
            special.setEnabled(enabled);

            events = tk.createActionEvent(special);
            assertNotNull("Do not know how to generate HActionEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertEquals("HActionEvent " + should + " been generated " + "from " + events[i], enabled,
                        theEvent[0] != null);
            }
        }
        if (hcomponent instanceof HKeyboardInputPreferred)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HKeyEvent)
                    {
                        theEvent[0] = e;
                        assertEquals("Only KEY_PRESSED events should " + "be generated", HKeyEvent.KEY_PRESSED,
                                e.getID());
                    }
                }
            });
            special.setEnabled(enabled);
            ((HKeyboardInputPreferred) special).setEditMode(true);
            events = tk.createKeyEvent(special);
            assertNotNull("Do not know how to generate HKeyEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertEquals("HKeyEvent " + should + " been generated " + "from " + events[i], enabled,
                        theEvent[0] != null);
            }
        }
        if (hcomponent instanceof HTextValue)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HTextEvent) theEvent[0] = e;
                }
            });
            special.setEnabled(enabled);
            events = tk.createTextEvent(special);
            assertNotNull("Do not know how to generate HTextEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertEquals("HTextEvent " + should + " been generated " + "from " + events[i], enabled,
                        theEvent[0] != null);
            }
        }
        if (hcomponent instanceof HAdjustmentInputPreferred)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HAdjustmentEvent) theEvent[0] = e;
                }
            });
            special.setEnabled(enabled);
            ((HAdjustmentInputPreferred) special).setAdjustMode(true);
            events = tk.createAdjustmentEvent(special);
            assertNotNull("Do not know how to generate HAdjustmentEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertEquals("HAdjustmentEvent " + should + " been generated " + "from " + events[i], enabled,
                        theEvent[0] != null);
            }
        }
        if (hcomponent instanceof HSelectionInputPreferred)
        {
            special = createSpecialComponent(new EventCheck()
            {
                public void validate(AWTEvent e)
                {
                    if (e instanceof HItemEvent) theEvent[0] = e;
                }
            });
            special.setEnabled(enabled);
            ((HSelectionInputPreferred) special).setSelectionMode(true);
            events = tk.createItemEvent(special);
            assertNotNull("Do not know how to generate HItemEvents", events);
            for (int i = 0; i < events.length; ++i)
            {
                theEvent[0] = null;
                special.processEvent(events[i]);
                assertEquals("HItemEvent " + should + " been generated " + "from " + events[i], enabled,
                        theEvent[0] != null);
            }
        }
    }

    /**
     * Tests getMatte/setMatte
     * <ul>
     * <li>The set matte should be the retreived matte
     * <li>A null matte should be allowed
     * </ul>
     */
    public void testMatte() throws HMatteException
    {
        HMatteLayerTest.testMatte(hcomponent);
    }

    /**
     * Interface used with "special components" to test the correctness of event
     * generation.
     */
    protected static interface EventCheck
    {
        void validate(java.awt.AWTEvent e);
    }
}
