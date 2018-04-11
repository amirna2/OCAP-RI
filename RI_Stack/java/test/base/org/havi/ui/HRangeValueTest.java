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
 * Tests {@link #HStaticRange}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.7 $, $Date: 2002/11/07 21:14:08 $
 */
public class HRangeValueTest extends HRangeTest
{

    /**
     * Standard constructor.
     */
    public HRangeValueTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HRangeValueTest.class);
    }

    /**
     * The tested component.
     */
    protected HRangeValue hrangevalue;

    /**
     * A ValueProxy subclass allowing HAdjustmentValueTest to get/set the
     * current value on the HRangeValue that is being tested.
     */
    protected HRangeValueProxy proxy = new HRangeValueProxy();

    /**
     * Should be overridden to create subclass of HRangeValue.
     * 
     * @return the instance of HRangeValue to test
     */
    protected HRangeValue createHRangeValue()
    {
        return new HRangeValue();
    }

    /**
     * Overridden to create an HRange.
     * 
     * @return the instance of HStaticRange to test
     */
    protected HRange createHRange()
    {
        return (hrangevalue = createHRangeValue());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HRange
     * <li>implements HAdjustableValue
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HRangeValue.class, HRange.class);
        HAdjustmentValueTest.testAncestry(HRangeValue.class);
    }

    /**
     * Test the 3 constructors of HRangeValue.
     * <ul>
     * <li>HRangeValue()
     * <li>HRangeValue(int orient, int min, int max, int value)
     * <li>HRangeValue(int orient, int min, int max, int value, int x, int y,
     * int width, int height)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HRangeValueTest.class);

        checkConstructor("HRangeValue()", new HRangeValue(), 0, 0, 0, 0, 0, 100, 0, HOrientable.ORIENT_LEFT_TO_RIGHT,
                false);
        checkConstructor("HRangeValue(int orient, int min, int max, int value)", new HRangeValue(
                HOrientable.ORIENT_LEFT_TO_RIGHT, 22, 40, 30), 0, 0, 0, 0, 22, 40, 30,
                HOrientable.ORIENT_LEFT_TO_RIGHT, false);
        checkConstructor("HRangeValue(int orient, int min, int max, int value, " + "int x, int y, int w, int h)",
                new HRangeValue(HOrientable.ORIENT_TOP_TO_BOTTOM, 0, 60, 50, 10, 20, 30, 40), 10, 20, 30, 40, 0, 60,
                50, HOrientable.ORIENT_TOP_TO_BOTTOM, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HRangeValue range, int x, int y, int w, int h, int min, int max,
            int value, int orient, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", range);
        assertEquals(msg + " x-coordinated not initialized correctly", x, range.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, range.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, range.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, range.getSize().height);
        assertEquals(msg + " min not initialized correctly", min, range.getMinValue());
        assertEquals(msg + " max not initialized correctly", max, range.getMaxValue());
        assertEquals(msg + " value not initialized correctly", value, range.getValue());
        assertEquals(msg + " orientation not initialized correctly", orient, range.getOrientation());

        // Check variables NOT exposed in constructors (see java docs for
        // details)
        assertNull(msg + " matte should be unassigned", range.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", range.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", range.getBackgroundMode(), range.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // range.getDefaultSize());
            assertEquals(msg + " default size should not be set", range.NO_DEFAULT_SIZE, range.getDefaultSize()); // changed
                                                                                                                  // Dec
                                                                                                                  // 10,
                                                                                                                  // 2006
                                                                                                                  // siegfried@heintze.com
        else
            assertEquals(msg + " default size initialized incorrectly", range.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", range.getHorizontalAlignment(),
                range.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", range.getVerticalAlignment(), range.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", range.getResizeMode(), range.RESIZE_NONE);
        assertSame(msg + " default look not used", HRangeValue.getDefaultLook(), range.getLook());
        assertEquals(msg + " min thumb offset default is incorrect", 0, range.getThumbMinOffset());
        assertEquals(msg + " max thumb offset default is incorrect", 0, range.getThumbMaxOffset());
        assertEquals(msg + " default behavior is incorrect", HStaticRange.SLIDER_BEHAVIOR, range.getBehavior());
        assertEquals(msg + " block increment value is incorrect", 1, range.getBlockIncrement());
        assertNull(msg + " gain focus sound incorrectly initialized", range.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", range.getLoseFocusSound());
        assertEquals(msg + " border mode not initialized correctly", true, range.getBordersEnabled());
    }

    /**
     * Test (set|get)AdjustMode().
     */
    public void testAdjustMode()
    {
        HAdjustmentValueTest.testAdjustMode(hrangevalue);
    }

    /**
     * Test processHAdjustmentEvent().
     */
    public void testProcessHAdjustmentEvent()
    {
        HAdjustmentValueTest.testProcessHAdjustmentEvent(hrangevalue, proxy);
    }

    /**
     * Test (add|remove)HAdjustmentListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public void testAdjustmentListener()
    {
        HAdjustmentValueTest.testAdjustmentListener(hrangevalue, proxy);
    }

    /**
     * Test (set|get)AdjustmentSound().
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setAdjustmentSound(null)
     * <li>Test that the sound is played when the component is adjusted
     * </ul>
     */
    public void testAdjustmentSound()
    {
        HAdjustmentValueTest.testAdjustmentSound(hrangevalue, proxy);
    }

    /**
     * Test (set|get)BlockIncrement().
     * <ul>
     * <li>The set increment should be the retreived increment
     * <li>The block increment should be used when adjusting more/less
     * <li>Values of <1 should be mapped to 1
     * <li>Test block increment/position with incompatible values: e.g., pos=1,
     * incr=2 and attempt to move decrement.
     * </ul>
     */
    public void testBlockIncrement()
    {
        HAdjustmentValueTest.testBlockIncrement(hrangevalue, proxy);
    }

    /**
     * Test (set|get)UnitIncrement().
     * <ul>
     * <li>The set increment should be the retreived increment
     * <li>The unit increment should be used when adjusting more/less
     * <li>Values of <1 should be mapped to 1
     * <li>Test unit increment/position with incompatible values: e.g., pos=1,
     * incr=2 and attempt to move decrement.
     * </ul>
     */
    public void testUnitIncrement()
    {
        HAdjustmentValueTest.testUnitIncrement(hrangevalue, proxy);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HRangeLooks should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HRangeValue should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HRangeValueTest.class);

        assertSame("Default look should be used", HRangeValue.getDefaultLook(), (new HRangeValue()).getLook());

        HRangeLook save = HRangeValue.getDefaultLook();
        try
        {
            HRangeLook look;
            HRangeValue.setDefaultLook(look = new HRangeLook());
            assertSame("Incorrect look retrieved", look, HRangeValue.getDefaultLook());
            assertSame("Default look should be used", look, (new HRangeValue()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HRangeValue.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HRangeValue.setDefaultLook(save);
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
        checkClass(HRangeValueTest.class);

        return new HRangeValue()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }

            public void processHAdjustmentEvent(org.havi.ui.event.HAdjustmentEvent e)
            {
                ev.validate(e);
            }
        };
    }

    public static class HRangeValueProxy extends HAdjustmentValueTest.ValueProxy
    {
        /**
         * Gets the current value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            current value from.
         */
        public int getCurrentValue(HAdjustmentValue av)
        {
            return ((HRangeValue) av).getValue();
        }

        /**
         * Sets the current value of the specified <code>HAdjustmentValue</code>
         * to the specified value.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to set the
         *            current value on.
         * @param value
         *            the value to set on the specified
         *            <code>HAdjustmentValue</code>.
         */
        public void setCurrentValue(HAdjustmentValue av, int value)
        {
            ((HRangeValue) av).setValue(value);
        }

        /**
         * Gets the minimum value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            minimum value from.
         */
        public int getMinimumValue(HAdjustmentValue av)
        {
            return ((HRangeValue) av).getMinValue();
        }

        /**
         * Gets the maximum value of the specified <code>HAdjustmentValue</code>
         * object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to get the
         *            maximum value from.
         */
        public int getMaximumValue(HAdjustmentValue av)
        {
            return ((HRangeValue) av).getMaxValue();
        }

        /**
         * Sets the min and max values for the specified
         * <code>HAdjustmentValue</code> object instance.
         * 
         * @param av
         *            the <code>HAdjustmentValue</code> instance to set the
         *            value range on.
         */
        public void setValueRange(HAdjustmentValue av, int min, int max)
        {
            ((HRangeValue) av).setRange(min, max);
        }

    }

}
