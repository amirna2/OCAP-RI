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
import junit.framework.*;

import java.awt.*;
import org.w3c.dom.*;

import org.cablelabs.gear.test.XMLTestConfig;
import org.cablelabs.gear.test.XMLTestConfig.TestData;

/**
 * Tests {@link #HGraphicsConfigTemplate}.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @author Todd Earles
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:14 $
 */
public class HGraphicsConfigTemplateTest extends TestCase
{
    HGraphicsConfigTemplate template;

    /**
     * Standard constructor.
     */
    public HGraphicsConfigTemplateTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HGraphicsConfigTemplateTest.class);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        template = new HGraphicsConfigTemplate();
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
     * <li>extends HScreenConfigTemplate
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HGraphicsConfigTemplate.class, HScreenConfigTemplate.class);
    }

    /**
     * Test the single constructor of HGraphicsConfigTemplate.
     * <ul>
     * <li>HGraphicsConfigTemplate()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HGraphicsConfigTemplate()", new HGraphicsConfigTemplate());
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HGraphicsConfigTemplate gct)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", gct);

        // Check variables exposed in constructors
        HScreenConfigTemplateTest.checkInitialPriorities(gct, allFields, isObjPref);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HGraphicsConfigTemplate.class);
    }

    /**
     * Tests for no unexpected fields and that expected ones are unique.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HGraphicsConfigTemplate.class, fields);
        TestUtils.testUniqueFields(HGraphicsConfigTemplate.class, allFields, false);
    }

    private static final String[] fields = { "VIDEO_MIXING", "MATTE_SUPPORT", "IMAGE_SCALING_SUPPORT" };

    public static final String[] allFields = { "ZERO_BACKGROUND_IMPACT", "ZERO_GRAPHICS_IMPACT", "ZERO_VIDEO_IMPACT",
            "INTERLACED_DISPLAY", "FLICKER_FILTERING", "VIDEO_GRAPHICS_PIXEL_ALIGNED", "PIXEL_ASPECT_RATIO",
            "PIXEL_RESOLUTION", "SCREEN_RECTANGLE", "VIDEO_MIXING", "MATTE_SUPPORT", "IMAGE_SCALING_SUPPORT" };

    public static boolean[] isObjPref = { false, false, false, false, false, true, true, true, true, true, false, false };

    private static final Object[] prefObjs = { null, null, null, null, null,
            HScreen.getDefaultHScreen().getDefaultHVideoDevice().getDefaultConfiguration(), new Dimension(16, 15),
            new Dimension(320, 240), new HScreenRectangle(0.0f, 0.0f, 0.5f, 0.5f), null, null, null };

    /**
     * Test getPreferenceObject.
     * <ul>
     * <li>Test the 3 common preference objects
     * <li>Ensure that what is set, is what is retrieved
     * </ul>
     */
    public void testPreferenceObject()
    {
        HScreenConfigTemplateTest.checkPreferenceObject(template, allFields, isObjPref, prefObjs);
    }

    /**
     * Test getPreferencePriority and setPreference(int preference, int
     * priority)
     * <ul>
     * <li>Test that the set priority is the retrieved priority.
     * </ul>
     */
    public void testPreferencePriority()
    {
        HScreenConfigTemplateTest.checkPreferencePriority(template, allFields, isObjPref);
    }

    /**
     * Test isDisplayConfigSupported.
     * <ul>
     * <li>
     * </ul>
     */
    public void testDisplayConfigSupported()
    {
        // Load the "supported" test data for this class...
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "supported");
        // Get the test cases...
        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);

        // Iterate through all test cases testing the isDisplayConfigSupported()
        // method on each "inputtemplate" template with the given
        // "compareconfig" configuration, and compare against the given
        // <supported> value.
        for (int i = 0; i < testCases.length; i++)
        {
            // Get the input templates
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            // Get a reference to the test template...
            HGraphicsConfigTemplate testTemplate = null;
            for (int j = 0; j < inputData.length; j++)
            {
                if (inputData[j].data instanceof HGraphicsConfigTemplate)
                {
                    testTemplate = (HGraphicsConfigTemplate) inputData[j].data;
                    break;
                }
            }

            assertTrue("\"inputtemplate\" must be of type org.havi.ui.HGraphicsConfigTemplate " + "for test case " + i,
                    testTemplate != null);

            // Get the configuration template to test against...
            TestData[] configData = XMLTestConfig.subsetTestData(inputData, "compareconfig");
            assertTrue("One and only one \"compareconfig\" template expected for test case " + i,
                    configData.length == 1);
            assertTrue("\"compareconfig\" must be of type org.havi.ui.HGraphicsConfigTemplate " + "for test case " + i,
                    configData[0].data instanceof HGraphicsConfigTemplate);

            // Get a reference to the config template...
            HGraphicsConfigTemplate configTemplate = (HGraphicsConfigTemplate) configData[0].data;

            // Get the specified device to test against...
            HGraphicsDevice device = (HGraphicsDevice) XMLTestConfig.findNamedDevice("graphics", configData[0].misc);
            assertNotNull("the specified device is not found in test case <" + i + "> " + configData[0].misc, device);
            // Get the best configuration matching the template...
            HGraphicsConfiguration config = (HGraphicsConfiguration) XMLTestConfig.getBestConfig(device, configTemplate);

            // Get the result data...
            TestData[] result = XMLTestConfig.getTestData("Result", testCases[i]);
            assertTrue("One and only one \"Result\" object expected for test case " + i, result.length == 1);
            assertTrue("\"Result\" object must be of type java.lang.Boolean " + "for test case " + i,
                    result[0].data instanceof Boolean);

            // Get the boolean result...
            Boolean compatible = (Boolean) result[0].data;

            // test against the default configuration on the given device
            assertEquals("the isDisplayConfigSupported method failed for testcase <" + i + ">",
                    compatible.booleanValue(), testTemplate.isConfigSupported(config));

        }
    }
}
