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

import junit.framework.*;
import org.cablelabs.test.*;

import org.havi.ui.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import org.davic.resources.*;
import java.lang.reflect.*;

/**
 * Tests HEventMulticaster.
 * 
 * @author Aaron Kamienski
 * @version $Id: HEventMulticasterTest.java,v 1.2 2002/06/03 21:32:13 aaronk Exp
 *          $
 */
public class HEventMulticasterTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HEventMulticasterTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HEventMulticasterTest(String s, int index, Class listenerClass)
    {
        super(s);
        lookup = index;
        this.listenerClass = listenerClass;
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
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
     * Parameter(s) passed to parameterized tests.
     */
    protected Object params;

    /**
     * The parameterized listener class.
     */
    protected Class listenerClass;

    /**
     * The parameterized listener method variable.
     */
    protected int lookup;

    /**
     * Defines a TestSuite.
     */
    public static TestSuite suite()
    {
        TestSuite suite = TestUtils.suite(HEventMulticasterTest.class);

        suite.addTest(new HEventMulticasterTest("actionPerformed", ACTIONED, HActionListener.class));
        suite.addTest(new HEventMulticasterTest("focusGained", FOCUSGAINED, HFocusListener.class));
        suite.addTest(new HEventMulticasterTest("focusLost", FOCUSLOST, HFocusListener.class));
        suite.addTest(new HEventMulticasterTest("keyPressed", PRESSED, HKeyListener.class));
        suite.addTest(new HEventMulticasterTest("keyReleased", RELEASED, HKeyListener.class));
        suite.addTest(new HEventMulticasterTest("keyTyped", TYPED, HKeyListener.class));
        suite.addTest(new HEventMulticasterTest("selectionChanged", SELECTIONCHANGED, HItemListener.class));
        suite.addTest(new HEventMulticasterTest("currentItemChanged", CURRENTITEMCHANGED, HItemListener.class));
        suite.addTest(new HEventMulticasterTest("report", CONFIGURED, HScreenConfigurationListener.class));
        suite.addTest(new HEventMulticasterTest("report", RELOCATED, HScreenLocationModifiedListener.class));
        suite.addTest(new HEventMulticasterTest("caretMoved", CARETMOVED, HTextListener.class));
        suite.addTest(new HEventMulticasterTest("textChanged", TEXTCHANGED, HTextListener.class));
        suite.addTest(new HEventMulticasterTest("statusChanged", RESOURCE, ResourceStatusListener.class));
        // Adding a frame fails FIXME(sh)
        //        
        // suite.addTest(new HEventMulticasterTest("windowOpened",
        // OPENED,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowClosing",
        // CLOSING,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowClosed",
        // CLOSED,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowIconified",
        // ICONED,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowDeiconified",
        // DEICONED,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowActivated",
        // ACTIVE,
        // WindowListener.class));
        // suite.addTest(new HEventMulticasterTest("windowDeactivated",
        // DEACTIVE,
        // WindowListener.class));
        //        
        return suite;
    }

    protected void runTest() throws Throwable
    {
        if (listenerClass == null)
            super.runTest();
        else
            xTest();
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    private static final Class[] classes = { HActionListener.class, HAdjustmentListener.class,
            HBackgroundImageListener.class, HFocusListener.class, HItemListener.class, HKeyListener.class,
            HScreenConfigurationListener.class, HScreenLocationModifiedListener.class, HTextListener.class,
            ResourceStatusListener.class, WindowListener.class, };

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object
     * <li>implements HBackgroundImageListener
     * <li>implements HScreenConfigurationListener
     * <li>implements HScreenLocationModifiedListener
     * <li>java.awt.event.WindowListener
     * <li>implements HActionListener
     * <li>implements HAdjustmentListener
     * <li>implements HFocusListener
     * <li>implements HItemListener
     * <li>implements HTextListener
     * <li>implements HKeyListener
     * <li>implements org.davic.resources.ResourceStatusListener
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HEventMulticaster.class, Object.class);
        for (int i = 0; i < classes.length; ++i)
            TestUtils.testImplements(HEventMulticaster.class, classes[i]);
    }

    /**
     * There are no public constructors. There is one protected constructor:
     * <ul>
     * <li>HEventMulticaster(EventListener a, EventListener b)
     * </ul>
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HEventMulticaster.class);

        EventListener a = new WindowAdapter()
        {
        }, b = new WindowAdapter()
        {
        };
        checkConstructor("HEventMulticaster(EventListener, EventListener)", new HEventMulticaster(a, b), a, b);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    protected void checkConstructor(String msg, HEventMulticaster em, EventListener a, EventListener b)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", em);
        assertSame(msg + " a not initialized correctly", a, em.a);
        assertSame(msg + " b not initialized correctly", b, em.b);
    }

    protected Adapter createAdapter()
    {
        return new Adapter();
    }

    protected Class testedClass()
    {
        return HEventMulticaster.class;
    }

    /**
     * Tests add/remove and event dispatch for a particular interface.
     */
    public void xTest() throws Exception
    {
        EventListener el = null;
        Adapter[] adapters = new Adapter[] { createAdapter(), createAdapter(), createAdapter(), };

        // Add listeners
        Method add = testedClass().getMethod("add", new Class[] { listenerClass, listenerClass });
        el = (EventListener) add.invoke(null, new Object[] { null, adapters[0] });
        assertTrue("Listener should be an " + listenerClass.getName(), !(el instanceof HEventMulticaster));
        el = (EventListener) add.invoke(null, new Object[] { el, adapters[1] });
        assertTrue("Listeners should be an HEventMulticaster", el instanceof HEventMulticaster);
        el = (EventListener) add.invoke(null, new Object[] { el, adapters[2] });
        assertTrue("Listeners should be an HEventMulticaster", el instanceof HEventMulticaster);

        // Invoke listeners
        doit(el, lookup);

        // Check that listeners were called
        for (int i = 0; i < adapters.length; ++i)
        {
            assertEquals("Listener method should be called once", 1, adapters[i].called[lookup]);
            for (int j = 0; j < adapters[i].called.length; ++j)
                if (j != lookup) assertEquals("Other listeners should not be called", 0, adapters[i].called[j]);
        }

        // Remove listeners
        Method remove = testedClass().getMethod("remove", new Class[] { listenerClass, listenerClass });
        // No particular order
        el = (EventListener) remove.invoke(null, new Object[] { el, adapters[1] });
        assertTrue("Listeners should be an HEventMulticaster", el instanceof HEventMulticaster);
        el = (EventListener) remove.invoke(null, new Object[] { el, adapters[2] });
        assertTrue("Listener should be an " + listenerClass.getName(), !(el instanceof HEventMulticaster));
        el = (EventListener) remove.invoke(null, new Object[] { el, adapters[0] });

        assertNull("Listeners should be null now", el);
    }

    /**
     * Calls the appropriate method on the multicaster.
     */
    protected void doit(EventListener el, int what)
    {
        switch (what)
        {
            case ACTIONED:
                ((HActionListener) el).actionPerformed(new HActionEvent(new HGraphicButton(),
                        HActionEvent.ACTION_PERFORMED, ""));
                break;
            case ADJUSTED:
                ((HAdjustmentListener) el).valueChanged(new HAdjustmentEvent(new HRangeValue(),
                        HAdjustmentEvent.ADJUST_MORE));
                break;
            case FOCUSLOST:
                ((HFocusListener) el).focusLost(new HFocusEvent(new HIcon(), HFocusEvent.FOCUS_LOST));
                break;
            case FOCUSGAINED:
                ((HFocusListener) el).focusGained(new HFocusEvent(new HIcon(), HFocusEvent.FOCUS_GAINED));
                break;
            case PRESSED:
                ((HKeyListener) el).keyPressed(new HKeyEvent(new HSinglelineEntry(), HKeyEvent.KEY_PRESSED, 0, 0, 0,
                        '\0'));
                break;
            case RELEASED:
                ((HKeyListener) el).keyReleased(new HKeyEvent(new HSinglelineEntry(), HKeyEvent.KEY_RELEASED, 0, 0, 0,
                        '\0'));
                break;
            case TYPED:
                ((HKeyListener) el).keyTyped(new HKeyEvent(new HSinglelineEntry(), HKeyEvent.KEY_TYPED, 0, 0, 0, '\0'));
                break;
            case SELECTIONCHANGED:
                ((HItemListener) el).selectionChanged(new HItemEvent(new HListGroup(), HItemEvent.ITEM_CLEARED, null));
                break;
            case CURRENTITEMCHANGED:
                ((HItemListener) el).currentItemChanged(new HItemEvent(new HListGroup(), HItemEvent.ITEM_SET_NEXT, null));
                break;
            case CONFIGURED:
                ((HScreenConfigurationListener) el).report(new HScreenConfigurationEvent(""));
                break;
            case RELOCATED:
                ((HScreenLocationModifiedListener) el).report(new HScreenLocationModifiedEvent(""));
                break;
            case TEXTCHANGED:
                ((HTextListener) el).textChanged(new HTextEvent(new HSinglelineEntry(), HTextEvent.TEXT_START_CHANGE));
                break;
            case CARETMOVED:
                ((HTextListener) el).caretMoved(new HTextEvent(new HSinglelineEntry(), HTextEvent.CARET_NEXT_CHAR));
                break;
            case RESOURCE:
                ((ResourceStatusListener) el).statusChanged(new ResourceStatusEvent(""));
                break;
            case OPENED:
                ((WindowListener) el).windowOpened(new WindowEvent(new Frame(), WindowEvent.WINDOW_OPENED));
                break;
            case CLOSING:
                ((WindowListener) el).windowClosing(new WindowEvent(new Frame(), WindowEvent.WINDOW_CLOSING));
                break;
            case CLOSED:
                ((WindowListener) el).windowClosed(new WindowEvent(new Frame(), WindowEvent.WINDOW_CLOSED));
                break;
            case ICONED:
                ((WindowListener) el).windowIconified(new WindowEvent(new Frame(), WindowEvent.WINDOW_ICONIFIED));
                break;
            case DEICONED:
                ((WindowListener) el).windowDeiconified(new WindowEvent(new Frame(), WindowEvent.WINDOW_DEICONIFIED));
                break;
            case ACTIVE:
                ((WindowListener) el).windowActivated(new WindowEvent(new Frame(), WindowEvent.WINDOW_ACTIVATED));
                break;
            case DEACTIVE:
                ((WindowListener) el).windowDeactivated(new WindowEvent(new Frame(), WindowEvent.WINDOW_DEACTIVATED));
                break;
        }
    }

    public static final int ACTIONED = 0;

    public static final int ADJUSTED = 1;

    public static final int FOCUSLOST = 2, FOCUSGAINED = 3;

    public static final int PRESSED = 4, RELEASED = 5, TYPED = 6;

    public static final int SELECTIONCHANGED = 7, CURRENTITEMCHANGED = 8;

    public static final int CONFIGURED = 9;

    public static final int RELOCATED = 10;

    public static final int CARETMOVED = 11, TEXTCHANGED = 12;

    public static final int RESOURCE = 13;

    public static final int OPENED = 14, CLOSING = 15, CLOSED = 16, ICONED = 17, DEICONED = 18, ACTIVE = 19,
            DEACTIVE = 20;

    /**
     * Adapter class used in tests.
     */
    protected static class Adapter implements HActionListener, HAdjustmentListener, HFocusListener, HItemListener,
            HKeyListener, HScreenConfigurationListener, HScreenLocationModifiedListener, HTextListener,
            ResourceStatusListener, WindowListener
    {
        public int[] called = new int[21];

        public void actionPerformed(ActionEvent e)
        {
            ++called[ACTIONED];
        }

        public void valueChanged(HAdjustmentEvent e)
        {
            ++called[ADJUSTED];
        }

        public void focusGained(FocusEvent e)
        {
            ++called[FOCUSGAINED];
        }

        public void focusLost(FocusEvent e)
        {
            ++called[FOCUSLOST];
        }

        public void keyPressed(KeyEvent e)
        {
            ++called[PRESSED];
        }

        public void keyReleased(KeyEvent e)
        {
            ++called[RELEASED]; // don't care
        }

        public void keyTyped(KeyEvent e)
        {
            ++called[TYPED]; // don't care
        }

        public void selectionChanged(HItemEvent e)
        {
            ++called[SELECTIONCHANGED];
        }

        public void currentItemChanged(HItemEvent e)
        {
            ++called[CURRENTITEMCHANGED];
        }

        public void report(HScreenConfigurationEvent e)
        {
            ++called[CONFIGURED];
        }

        public void report(HScreenLocationModifiedEvent e)
        {
            ++called[RELOCATED];
        }

        public void caretMoved(HTextEvent e)
        {
            ++called[CARETMOVED];
        }

        public void textChanged(HTextEvent e)
        {
            ++called[TEXTCHANGED];
        }

        public void statusChanged(ResourceStatusEvent event)
        {
            ++called[RESOURCE];
        }

        public void windowOpened(WindowEvent e)
        {
            ++called[OPENED];
        }

        public void windowClosing(WindowEvent e)
        {
            ++called[CLOSING];
        }

        public void windowClosed(WindowEvent e)
        {
            ++called[CLOSED];
        }

        public void windowIconified(WindowEvent e)
        {
            ++called[ICONED];
        }

        public void windowDeiconified(WindowEvent e)
        {
            ++called[DEICONED];
        }

        public void windowActivated(WindowEvent e)
        {
            ++called[ACTIVE];
        }

        public void windowDeactivated(WindowEvent e)
        {
            ++called[DEACTIVE];
        }
    }
}
