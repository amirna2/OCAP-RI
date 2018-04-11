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
import org.cablelabs.test.GUITest;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import java.io.*;

import org.cablelabs.impl.util.MPEEnv;

/**
 * Provides a support framework for testing HAVi components.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.23 $, $Date: 2002/06/03 21:32:24 $
 */
public abstract class TestSupport extends Assert implements HState
{
    static String baseDirectory;
    static
    {
        //
        // if the snfs directory exists, assume that
        // we should use that for the base directory to
        // load classes from
        //
        if ((new File("/snfs")).exists())
        {
            baseDirectory = "/snfs";
        }
        else
        {
            baseDirectory = "/syscwd";
        }
    }

    public static String getBaseDirectory()
    {
        return baseDirectory;
    }

    /*  *********************** State ******************** */

    /**
     * Sets the interaction state on the given component.
     */
    public static void setInteractionState(Object o, int state)
    {
        ((HVisible) o).setInteractionState(state);
    }

    /**
     * Gets the interaction state for the given component.
     */
    public static int getInteractionState(Object o)
    {
        return ((HVisible) o).getInteractionState();
    }

    /**
     * Check for an expected transition. ???
     */
    public static void checkState(Object o, int oldState, int newState)
    {
        assertEquals(getStateName(oldState) + " -> " + getStateName(newState) + " transition "
                + ((oldState == newState) ? "un" : "") + "expected", newState, getInteractionState(o));
    }

    /**
     * Map a state id to a state name.
     * 
     * @param state
     *            the state constant (see {@link HState})
     * @return a String representation of the given state, or "?" if no such
     *         state exists.
     */
    public static String getStateName(int state)
    {
        switch (state)
        {
            case NORMAL_STATE:
                return "NORMAL_STATE";
            case FOCUSED_STATE:
                return "FOCUSED_STATE";
            case ACTIONED_STATE:
                return "ACTIONED_STATE";
            case ACTIONED_FOCUSED_STATE:
                return "ACTIONED_FOCUSED_STATE";
            case DISABLED_STATE:
                return "DISABLED_STATE";
            case DISABLED_FOCUSED_STATE:
                return "DISABLED_FOCUSED_STATE";
            case DISABLED_ACTIONED_STATE:
                return "DISABLED_ACTIONED_STATE";
            case DISABLED_ACTIONED_FOCUSED_STATE:
                return "DISABLED_ACTIONED_FOCUSED_STATE";
            case ALL_STATES:
                return "ALL_STATES";
            default:
                String str = "";
                String or = "";
                if ((state & FOCUSED_STATE_BIT) != 0)
                {
                    str += "FOCUSED";
                    or = "|";
                }
                if ((state & ACTIONED_STATE_BIT) != 0)
                {
                    str += or + "ACTIONED";
                    or = "|";
                }
                if ((state & DISABLED_STATE_BIT) != 0)
                {
                    str += or + "DISABLED";
                    or = "|";
                }
                if ((state &= ~(ALL_STATES | NORMAL_STATE)) != 0)
                {
                    str += or + "0x" + Integer.toHexString(state);
                }
                return str;
        }
    }

    /**
     * Utility method used to enumerate the various HStates.
     */
    public static void foreachState(Callback callback)
    {
        HVisibleTest.foreachState(callback);
    }

    /**
     * Simple state-based callback interface.
     * 
     * @see #foreachState(Callback)
     */
    public static interface Callback extends HVisibleTest.Callback
    {
    }

    /*  *********************** Looks ******************** */
    /**
     * Will call <code>v.setLook(l)</code>. If <code>v</code> does not accept
     * the given look, then it will be wrapped in an appropriate adapter.
     */
    public static void setLook(Object o, HLook l)
    {
        HVisible v = (HVisible) o;
        try
        {
            if (v instanceof HStaticText)
                v.setLook(new org.cablelabs.gear.havi.decorator.TextLookAdapter(l));
            else if (v instanceof HStaticIcon)
                v.setLook(new org.cablelabs.gear.havi.decorator.GraphicLookAdapter(l));
            else if (v instanceof HStaticAnimation)
                v.setLook(new org.cablelabs.gear.havi.decorator.AnimateLookAdapter(l));
            else if (v instanceof HStaticRange)
                v.setLook(new org.cablelabs.gear.havi.decorator.RangeLookAdapter(l));
            else
                v.setLook(l);
        }
        catch (Exception ignored)
        {
            ignored.printStackTrace();
        }
    }

    /*  *********************** Properties ******************** */
    private static Properties props;

    /**
     * Loads and returns a Properties object that defines variables that affect
     * testing.
     */
    public synchronized static Properties getProperties()
    {
        if (props == null)
        {
            props = new Properties();

            try
            {
                String file = MPEEnv.getSystemProperty("HAVITEST.PROPERTIES", "/havitest.properties");
                InputStream is = new BufferedInputStream(TestSupport.class.getResource(file).openStream());
                props.load(is);
                is.close();
            }
            catch (Exception ignored)
            {
                // Allow no file (same as empty file)
                // ignored.printStackTrace();
            }
        }

        return props;
    }

    public static String getProperty(String property)
    {
        return getProperties().getProperty(property);
    }

    public static boolean getProperty(String property, boolean defValue)
    {
        String val = getProperties().getProperty(property);

        return (val == null) ? defValue : val.equals("true");
    }

    public static int getProperty(String property, int defValue)
    {
        String val = getProperties().getProperty(property);

        return (val == null) ? defValue : Integer.parseInt(val);
    }

    public static boolean isPropertyDefined(String property)
    {
        return getProperties().getProperty(property) != null;
    }

    /*  ****************** Addl. Assert Support ****************** */

    /**
     * Asserts that the given Dimension is >= the given Dimension. ???
     */
    public static void assertGreaterEqual(String msg, Dimension d1, Dimension d2)
    {
        assertTrue(msg + " " + d1.width + " < " + d2.width + ", expected >=", d1.width >= d2.width);
        assertTrue(msg + " " + d1.height + " < " + d2.height + ", expected >=", d1.height >= d2.height);
    }

    /**
     * Asserts that the given Insets is >= the given Insets. ???
     */
    public static void assertGreaterEqual(String msg, Insets i1, Insets i2)
    {
        assertTrue(msg + " " + i1.top + " < " + i2.top + ", expected >=", i1.top >= i2.top);
        assertTrue(msg + " " + i1.left + " < " + i2.left + ", expected >=", i1.left >= i2.left);
        assertTrue(msg + " " + i1.bottom + " < " + i2.bottom + ", expected >=", i1.bottom >= i2.bottom);
        assertTrue(msg + " " + i1.right + " < " + i2.right + ", expected >=", i1.right >= i2.right);
    }

    private static String toString(HScreenRectangle r)
    {
        return "HScreenRectangle[" + r.x + "," + r.y + "," + r.width + "x" + r.height + "]";
    }

    private static String toString(HScreenPoint p)
    {
        return "HScreenPoint[" + p.x + "," + p.y + "]";
    }

    private static String toString(HScreenDimension d)
    {
        return "HScreenDimension[" + d.width + "x" + d.height + "]";
    }

    private static float THRESHOLD = 0.00001f;

    public static void assertEquals(String message, HScreenRectangle expected, HScreenRectangle actual)
    {
        assertTrue(message + " - expected: " + toString(expected) + ", was: " + toString(actual), areEqual(expected,
                actual));
    }

    public static void assertEquals(String message, HScreenPoint expected, HScreenPoint actual)
    {
        assertTrue(message + " - expected: " + toString(expected) + ", was: " + toString(actual), areEqual(expected,
                actual));
    }

    public static void assertEquals(String message, HScreenDimension expected, HScreenDimension actual)
    {
        assertTrue(message + " - expected: " + toString(expected) + ", was: " + toString(actual), areEqual(expected,
                actual));
    }

    public static void assertEqual(String message, HScreenConfigTemplate expected, HScreenConfigTemplate actual,
            String[] prefs, boolean[] isObjPref)
    {
        assertTrue(message, areEqual(expected, actual, prefs, isObjPref));
    }

    public static void assertEqual(String message, HSceneTemplate expected, HSceneTemplate actual, String[] prefs)
    {
        assertTrue(message, areEqual(expected, actual, prefs));
    }

    /*  *********************** Other Equivalence Checks ******************** */

    public static boolean areEqual(HScreenRectangle expected, HScreenRectangle actual)
    {
        return ((expected.x >= actual.x - THRESHOLD && expected.x <= actual.x + THRESHOLD)
                && (expected.y >= actual.y - THRESHOLD && expected.y <= actual.y + THRESHOLD)
                && (expected.width >= actual.width - THRESHOLD && expected.width <= actual.width + THRESHOLD) && (expected.height >= actual.height
                - THRESHOLD && expected.height <= actual.height + THRESHOLD));
    }

    public static boolean areEqual(HScreenPoint expected, HScreenPoint actual)
    {
        return ((expected.x >= actual.x - THRESHOLD && expected.x <= actual.x + THRESHOLD) && (expected.y >= actual.y
                - THRESHOLD && expected.y <= actual.y + THRESHOLD));
    }

    public static boolean areEqual(HScreenDimension expected, HScreenDimension actual)
    {
        return ((expected.width >= actual.width - THRESHOLD && expected.width <= actual.width + THRESHOLD) && (expected.height >= actual.height
                - THRESHOLD && expected.height <= actual.height + THRESHOLD));
    }

    public static boolean areEqual(HScreenConfigTemplate expected, HScreenConfigTemplate actual, String[] prefs,
            boolean[] isObjPref)
    {
        boolean same = true;
        Class type = expected.getClass();

        for (int i = 0; i < prefs.length; i++)
        {
            int prefValue = getVal(type, prefs[i]);

            if (!(actual.getPreferencePriority(prefValue) == expected.getPreferencePriority(prefValue)))
            {
                same = false;
                break;
            }

            if (isObjPref[i])
            {
                Object expectedObject = expected.getPreferenceObject(prefValue);
                Object actualObject = actual.getPreferenceObject(prefValue);

                if (expectedObject == actualObject)
                {
                    continue;
                }
                else if (expectedObject instanceof HScreenRectangle)
                {
                    if (!areEqual((HScreenRectangle) actualObject, (HScreenRectangle) expectedObject))
                    {
                        same = false;
                        break;
                    }
                }
                else
                {
                    if (!actualObject.equals(expectedObject))
                    {
                        same = false;
                        break;
                    }
                }
            }
        }

        return (same);
    }

    public static boolean areEqual(HSceneTemplate expected, HSceneTemplate actual, String[] prefs)
    {
        boolean same = true;

        if (expected == null || actual == null)
        {
            if (expected == actual) // if they're both null
                return true;
            else
                return false;
        }

        Class type = expected.getClass();

        for (int i = 0; i < prefs.length; i++)
        {
            int prefValue = getVal(type, prefs[i]);

            if (!(actual.getPreferencePriority(prefValue) == expected.getPreferencePriority(prefValue)))
            {
                same = false;
                break;
            }

            Object expectedObject = expected.getPreferenceObject(prefValue);
            Object actualObject = actual.getPreferenceObject(prefValue);

            if (expectedObject == actualObject)
            {
                continue;
            }
            else if (expectedObject instanceof HGraphicsConfiguration)
            {
                if (!areEqual(((HGraphicsConfiguration) actualObject).getConfigTemplate(),
                        ((HGraphicsConfiguration) expectedObject).getConfigTemplate(),
                        HGraphicsConfigTemplateTest.allFields, HGraphicsConfigTemplateTest.isObjPref))
                {
                    same = false;
                    break;
                }
            }
            else if (expectedObject instanceof HScreenDimension)
            {
                if (!areEqual((HScreenDimension) actualObject, (HScreenDimension) expectedObject))
                {
                    same = false;
                    break;
                }
            }
            else if (expectedObject instanceof HScreenPoint)
            {
                if (!areEqual((HScreenPoint) actualObject, (HScreenPoint) expectedObject))
                {
                    same = false;
                    break;
                }
            }
            else
            {
                if (!actualObject.equals(expectedObject))
                {
                    same = false;
                    break;
                }
            }
        }

        return (same);
    }

    private static int getVal(Class type, String name)
    {
        int val = -1;

        try
        {
            val = type.getField(name).getInt(null);
        }
        catch (Exception e)
        {
        }

        return val;
    }

    /*  *********************** Other Support ******************** */

    /**
     * Delays execution for the given about of milliseconds.
     * 
     * @param ms
     *            time to delay in milliseconds
     */
    public static void delay(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }

    /**
     * Check the display of a component.
     */
    public static void checkDisplay(Component v, String title, String[] questions, String resource, GUITest test)
    {
        GUITest.delay(100); // kludge! (in case repaint already started)
        GUITest.waitForRepaint(v);
        GUITest.delay(100); // kludge!

        byte[] snapshot = GUITest.getSnapshot(v);
        assertNotNull("No snapshot taken");
        Dimension d = v.getSize();
        test.assertImage(title, questions, snapshot, d.width, d.height, resource);
    }

    private static org.cablelabs.gear.util.ImagePortfolio arrows;

    /**
     * Returns an image. Valid indices are 1-4.
     */
    public static synchronized Image getArrow(int i)
    {
        if (arrows == null)
        {
            arrows = new org.cablelabs.gear.util.ImagePortfolio();

            arrows.addImage(new String[] { "arrows1", "arrows2", "arrows3", "arrows4" }, new String[] {
                    "/images/arrows1.gif", "/images/arrows2.gif", "/images/arrows3.gif", "/images/arrows4.gif" },
                    TestSupport.class);
        }
        try
        {
            return arrows.getImage("arrows" + i);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static org.cablelabs.gear.util.ImagePortfolio stateImgs;

    /**
     * Returns an image.
     */
    public static synchronized Image getState(int state)
    {
        if (stateImgs == null)
        {
            stateImgs = new org.cablelabs.gear.util.ImagePortfolio();

            stateImgs.addImage(new String[] { getStateName(NORMAL_STATE), getStateName(FOCUSED_STATE),
                    getStateName(ACTIONED_STATE), getStateName(ACTIONED_FOCUSED_STATE), getStateName(DISABLED_STATE),
                    getStateName(DISABLED_FOCUSED_STATE), getStateName(DISABLED_ACTIONED_STATE),
                    getStateName(DISABLED_ACTIONED_FOCUSED_STATE), }, new String[] { "/images/state_n.gif",
                    "/images/state_f.gif", "/images/state_a.gif", "/images/state_af.gif", "/images/state_d.gif",
                    "/images/state_df.gif", "/images/state_da.gif", "/images/state_daf.gif", }, TestSupport.class);
        }
        try
        {
            return stateImgs.getImage(getStateName(state));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /** Dummy HSound. */
    public static class EmptySound extends HSound
    {
        public EmptySound()
        {
        }

        public void dispose()
        {
        }

        public void load(String str)
        {
        }

        public void loop()
        {
        }

        public void play()
        {
        }

        public void stop()
        {
        }
    }

    public static class StateLook implements HLook
    {
        public void showLook(Graphics g, HVisible v, int state)
        {
            Dimension d = v.getSize();
            String name = getName(state);

            // Paint bg color
            g.setColor(getColor(state));
            g.fillRect(0, 0, d.width, d.height);

            // Center text in visible
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(name);
            int h = fm.getHeight() - fm.getDescent();

            g.setColor(Color.white);
            g.drawString(name, (d.width - w) / 2, (d.height + h) / 2);
        }

        private Color getColor(int state)
        {
            Color color;
            switch (state)
            {
                case HState.NORMAL_STATE:
                    color = Color.green;
                    break;
                case HState.FOCUSED_STATE:
                    color = Color.blue;
                    break;
                case HState.ACTIONED_FOCUSED_STATE:
                    color = Color.red;
                    break;
                case HState.ACTIONED_STATE:
                    color = Color.yellow.darker();
                    break;
                default:
                    color = Color.lightGray;
                    break;
            }
            return color;
        }

        private String getName(int state)
        {
            String name;
            switch (state)
            {
                case HState.NORMAL_STATE:
                    name = "NORMAL";
                    break;
                case HState.FOCUSED_STATE:
                    name = "FOCUS";
                    break;
                case HState.ACTIONED_FOCUSED_STATE:
                    name = "ACTION";
                    break;
                case HState.ACTIONED_STATE:
                    name = "NORMAL_ACTION";
                    break;
                default:
                    name = "?";
                    break;
            }
            return name;
        }

        public Dimension getPreferredSize(HVisible v)
        {
            FontMetrics fm = v.getFontMetrics(v.getFont());
            return new Dimension(fm.stringWidth("NORMAL_ACTION") + 10, fm.getHeight() + 10);
        }

        public Dimension getMinimumSize(HVisible v)
        {
            return getPreferredSize(v);
        }

        public Dimension getMaximumSize(HVisible v)
        {
            return getPreferredSize(v);
        }

        public Insets getInsets(HVisible v)
        {
            return null;
        }

        public boolean isOpaque(HVisible v)
        {
            return false;
        }

        public void widgetChanged(HVisible v, HChangeData[] changes)
        {
            v.repaint();
        }
    }

    public static class DebugListener extends MouseAdapter implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
            System.out.println(e);
        }

        public void focusLost(FocusEvent e)
        {
            System.out.println(e);
        }

        public void mouseEntered(MouseEvent e)
        {
            System.out.println(e);
        }

        public void mouseExited(MouseEvent e)
        {
            System.out.println(e);
        }
    }
}
