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
import org.w3c.dom.*;
import org.cablelabs.gear.test.XMLTestConfig;
import org.cablelabs.gear.test.XMLTestConfig.TestData;

/**
 * Tests {@link #HVideoConfigTemplate}.
 * 
 * @author Tom Henriksen
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:23 $
 */
public class HVideoConfigTemplateTest extends GUITest
{
    HVideoConfigTemplate template;

    HGraphicsConfiguration gfxCongif;

    /**
     * Standard constructor.
     */
    public HVideoConfigTemplateTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HVideoConfigTemplateTest.class);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        template = new HVideoConfigTemplate();

        // Get the default graphics configuration from default screen and set it
        // in preference objects array for VIDEO_GRAPHICS_PIXEL_ALIGNED and
        // GRAPHICS_MIXING
        prefObjs[5] = prefObjs[9] = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice().getDefaultConfiguration();
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
        TestUtils.testExtends(HVideoConfigTemplate.class, HScreenConfigTemplate.class);
    }

    /**
     * Test the single constructor of HVideoConfigTemplate.
     * <ul>
     * <li>HVideoConfigTemplate()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HVideoConfigTemplate()", new HVideoConfigTemplate());
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HVideoConfigTemplate vct)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", vct);

        // Check variables exposed in constructors
        HScreenConfigTemplateTest.checkInitialPriorities(vct, allFields, isObjPref);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HVideoConfigTemplate.class);
    }

    /**
     * Tests for no unexpected fields and that expected ones are unique.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HVideoConfigTemplate.class, fields);
        TestUtils.testUniqueFields(HVideoConfigTemplate.class, allFields, false);
    }

    private static final String fields[] = { "GRAPHICS_MIXING" };

    private static String allFields[] = { "ZERO_BACKGROUND_IMPACT", "ZERO_GRAPHICS_IMPACT", "ZERO_VIDEO_IMPACT",
            "INTERLACED_DISPLAY", "FLICKER_FILTERING", "VIDEO_GRAPHICS_PIXEL_ALIGNED", "PIXEL_ASPECT_RATIO",
            "PIXEL_RESOLUTION", "SCREEN_RECTANGLE", "GRAPHICS_MIXING" };

    final static boolean[] isObjPref = { false, false, false, false, false, true, true, true, true, true };

    private static Object[] prefObjs = { null, null, null, null, null, null, new Dimension(16, 15),
            new Dimension(320, 240), new HScreenRectangle(0.0f, 0.0f, 0.5f, 0.5f), null };

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
        // "compareconfig"
        // <config> configurations, and compare against the given "Result"
        // value.
        for (int i = 0; i < testCases.length; i++)
        {
            // Get the input templates
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            // Get the configuration template test...
            TestData[] templateData = XMLTestConfig.subsetTestData(inputData, "inputtemplate");
            assertTrue("One and only one \"inputtemplate\" expected for test case " + i, templateData.length == 1);
            assertTrue("\"inputtemplate\" must be of type org.havi.ui.HVideoConfigTemplate " + "for test case " + i,
                    templateData[0].data instanceof HVideoConfigTemplate);

            // Get the configuration template to test against...
            TestData[] configData = XMLTestConfig.subsetTestData(inputData, "compareconfig");
            assertTrue("One and only one \"compareconfig\" template expected for test case " + i,
                    configData.length == 1);
            assertTrue("\"compareconfig\" must be of type org.havi.ui.HVideoConfigTemplate " + "for test case " + i,
                    configData[0].data instanceof HVideoConfigTemplate);

            // Get a reference to the test template...
            HVideoConfigTemplate testTemplate = (HVideoConfigTemplate) inputData[0].data;

            // Get a reference to the config template...
            HVideoConfigTemplate configTemplate = (HVideoConfigTemplate) configData[0].data;

            // Get the specified device to test against...
            HVideoDevice device = (HVideoDevice) XMLTestConfig.findNamedDevice("video", configData[0].misc);
            assertNotNull("the specified device is not found in test case <" + i + "> " + configData[0].misc, device);
            // Get the best configuration matching the template...
            HVideoConfiguration config = (HVideoConfiguration) XMLTestConfig.getBestConfig(device, configTemplate);
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
