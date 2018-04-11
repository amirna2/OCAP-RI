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
import org.w3c.dom.*;
import org.cablelabs.gear.test.XMLTestConfig;
import org.cablelabs.gear.test.XMLTestConfig.TestData;

/**
 * Tests {@link #HSceneFactory}.
 * 
 * @author Jay Tracy
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/11/07 21:14:08 $
 */
public class HSceneFactoryTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HSceneFactoryTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HSceneFactoryTest.class);
    }

    /** Common access HSceneFactory */
    private HSceneFactory factory;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        factory = HSceneFactory.getInstance();
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
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HSceneFactory.class, Object.class);
    }

    /**
     * Ensure that there are no accessible constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HSceneFactory.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HSceneFactory.class);
    }

    /**
     * Tests getInstance().
     * <ul>
     * <li>returns a non-null HSceneFactory instance
     * <li>repeat callings should return the same object
     * </ul>
     */
    public void testInstance()
    {
        HSceneFactory f[] = new HSceneFactory[3];

        for (int i = 0; i < f.length; ++i)
        {
            f[i] = HSceneFactory.getInstance();
            assertNotNull("getInstance should return a non-null HSceneFactory", f);
            assertSame("getInstance should return the same object each time", factory, f[i]);
        }
    }

    /**
     * Tests getBestSceneTemplate.
     * 
     * Loop through all available test cases described for the "besttemplate"
     * test in the xml configuration file. For each test case, create a template
     * described by <template> and pass that into getBestSceneTemplate().
     * Compare the returned HSceneTemplate to the template described by
     * <besttemplate>.
     * 
     * Test data should be set up such that it tests the following:
     * <ul>
     * <li>returns the best scene possible (or null) for the given scene
     * template
     * <li>test that REQUIRED preferences are fulfilled, or no scene is returned
     * <li>test that LARGEST_DIMENSION does its job
     * </ul>
     */
    public void testBestSceneTemplate()
    {
        // load the "besttemplate" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "besttemplate");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);
        assertTrue("No test cases found for \"besttemplate\" test", (testCases.length > 0));

        // iterate through all test cases testing
        // getBestSceneTemplate for each template
        for (int i = 0; i < testCases.length; i++)
        {
            // get and execute setup data
            TestData[] setupData = XMLTestConfig.getTestData("Setup", testCases[i]);

            // optional setup step to set the current configuration
            if (setupData.length > 0)
            {
                assertTrue("setup object should be an HGraphicsConfigTemplate for " + "test case " + i,
                        setupData[0].data instanceof HGraphicsConfigTemplate);

                // find the device to set currentConfig on.
                // it should have been specified on the template element
                // and consequently set in the misc field of setupData.
                HScreenDevice device = XMLTestConfig.findNamedDevice(setupData[0].data.getClass().getName(),
                        setupData[0].misc);

                HScreenConfiguration config = XMLTestConfig.getBestConfig(device,
                        (HScreenConfigTemplate) setupData[0].data);

                XMLTestConfig.setCurrentConfig(device, config);
            }

            // Get the input template to test with (1 max)
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            assertTrue("must have one and only one input template for test case " + i, (inputData.length == 1));
            assertTrue("input object must be an HSceneTemplate for test case " + i,
                    (inputData[0].data instanceof HSceneTemplate));

            HSceneTemplate inputTemplate = (HSceneTemplate) inputData[0].data;

            // get best template from factory
            HSceneTemplate bestTemplate = factory.getBestSceneTemplate(inputTemplate);

            // get result template to compare against
            TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);
            assertTrue("One and only one result element expected for test case " + i, resultData.length == 1);

            if (resultData[0].data instanceof HSceneTemplate)
            {
                // make sure the best template matches the expected template
                // (specified in the xml file).
                TestSupport.assertEqual("incorrect best template returned for testCase " + i,
                        (HSceneTemplate) resultData[0].data, bestTemplate, HSceneTemplateTest.prefNames);
            }
            else if (resultData[0].data.equals("null"))
            {
                assertNull("best template should be null for testCase " + i, bestTemplate);
            }
            else
                fail("Invalid result data - must contain an HSceneTemplate or null");
        }
    }

    /**
     * Tests getBestScene().
     * 
     * Loop through all available test cases described for the "bestscene" test
     * in the xml configuration file. For each test case, create a template
     * described by <template> and pass that into getBestScene(). Then compare
     * the returned HScene to the template described by the <besttemplate>
     * element for the current test case.
     * 
     * Test data should be set up such that it tests the following:
     * <ul>
     * <li>returns the best scene possible (or null) for the given scene
     * template
     * <li>test that REQUIRED preferences are fulfilled, or no scene is returned
     * <li>test that LARGEST_DIMENSION does its job
     * </ul>
     */
    public void testBestScene()
    {
        // load the "bestscene" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "bestscene");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);
        assertTrue("No test cases found for \"besttemplate\" test", (testCases.length > 0));

        // iterate through all test cases testing
        // getBestScene for each template
        for (int i = 0; i < testCases.length; i++)
        {
            // get and execute setup data
            TestData[] setupData = XMLTestConfig.getTestData("Setup", testCases[i]);

            // optional setup step to set the current configuration
            if (setupData.length > 0)
            {
                assertTrue("setup object should be an HGraphicsConfigTemplate for " + "test case " + i,
                        setupData[0].data instanceof HGraphicsConfigTemplate);

                // find the device to set currentConfig on.
                // it should have been specified on the template element
                // and consequently set in the misc field of setupData.
                HScreenDevice device = XMLTestConfig.findNamedDevice(setupData[0].data.getClass().getName(),
                        setupData[0].misc);

                HScreenConfiguration config = XMLTestConfig.getBestConfig(device,
                        (HScreenConfigTemplate) setupData[0].data);

                XMLTestConfig.setCurrentConfig(device, config);
            }

            // Get the input template to test with (1 max)
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            assertTrue("must have one and only one input template for test case " + i, (inputData.length == 1));
            assertTrue("input object must be an HSceneTemplate for test case " + i,
                    (inputData[0].data instanceof HSceneTemplate));

            HSceneTemplate inputTemplate = (HSceneTemplate) inputData[0].data;

            // get best HScene from factory
            HScene bestScene = null;
            try
            {
                bestScene = factory.getBestScene(inputTemplate);

                // get result template to compare against
                TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);
                assertTrue("One and only one result element expected for test case " + i, resultData.length == 1);

                if (resultData[0].data instanceof HSceneTemplate)
                {
                    // make sure the template from the best scene
                    // matches the expected template (specified in the xml
                    // file).
                    TestSupport.assertEqual("incorrect best scene returned for testCase " + i,
                            (HSceneTemplate) resultData[0].data, (bestScene != null) ? bestScene.getSceneTemplate()
                                    : null, HSceneTemplateTest.prefNames);
                }
                else if (resultData[0].data.equals("null"))
                {
                    assertNull("best scene should be null for testCase " + i, bestScene);
                }
                else
                    fail("Invalid result data - must contain an HSceneTemplate or null");
            }
            finally
            {
                if (bestScene != null)
                {
                    bestScene.dispose();
                }
            }
        }
    }

    /**
     * Tests getDefaultScene().
     * 
     * Compares the template for the scene returned from
     * HSceneFactory.getInstance().getDefaultHScene() to the expected template
     * described in the xml test file.
     */
    public void testDefaultScene()
    {
        // load the "defaultscene" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "defaultscene");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);
        assertTrue("No test cases found for \"besttemplate\" test", (testCases.length > 0));

        // iterate through all test cases testing getDefaultHScene()
        for (int i = 0; i < testCases.length; i++)
        {
            // get and execute setup data
            TestData[] setupData = XMLTestConfig.getTestData("Setup", testCases[i]);

            // optional setup step to set the current configuration
            if (setupData.length > 0)
            {
                assertTrue("setup object should be an HGraphicsConfigTemplate for " + "test case " + i,
                        setupData[0].data instanceof HGraphicsConfigTemplate);

                // find the device to set current config on.
                // it should have been specified on the template element
                // and consequently set in the "misc" field of setupData.
                HScreenDevice device = XMLTestConfig.findNamedDevice(setupData[0].data.getClass().getName(),
                        setupData[0].misc);

                HScreenConfiguration config = XMLTestConfig.getBestConfig(device,
                        (HScreenConfigTemplate) setupData[0].data);

                XMLTestConfig.setCurrentConfig(device, config);
            }

            // No Input elements expected.

            // get default scene from factory
            HScene defaultScene = null;
            try
            {
                defaultScene = factory.getDefaultHScene();

                // Get result template to compare against
                TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);
                assertTrue("One and only one result element expected for test case " + i, resultData.length == 1);

                if (resultData[0].data instanceof HSceneTemplate)
                {
                    // make sure the default template matches the expected
                    // template
                    // (specified in the xml file).
                    TestSupport.assertEqual("incorrect default scene returned for testCase " + i,
                            (HSceneTemplate) resultData[0].data,
                            (defaultScene != null) ? defaultScene.getSceneTemplate() : null,
                            HSceneTemplateTest.prefNames);
                }
                else
                    fail("Invalid result data - must contain an HSceneTemplate");
            }
            finally
            {
                if (defaultScene != null)
                {
                    defaultScene.dispose();
                }
            }
        }
    }

    /**
     * Tests getDefaultScene(HScreen).
     * 
     * Compares the template for the scene returned from
     * HSceneFactory.getInstance().getDefaultHScene(HScreen) to the expected
     * template described in the xml test file.
     */
    public void testDefaultSceneForScreen()
    {
        // load the "defaultsceneforscreen" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "defaultsceneforscreen");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);
        assertTrue("No test cases found for \"besttemplate\" test", (testCases.length > 0));

        // iterate through all test cases testing getDefaultHScene(HScreen)
        for (int i = 0; i < testCases.length; i++)
        {
            // get and execute setup data
            TestData[] setupData = XMLTestConfig.getTestData("Setup", testCases[i]);

            // optional setup step to set the current configuration
            if (setupData.length > 0)
            {
                assertTrue("setup object should be an HGraphicsConfigTemplate for " + "test case " + i,
                        setupData[0].data instanceof HGraphicsConfigTemplate);

                // find the device to set current config on.
                // it should have been specified on the template element
                // and consequently set in the "misc" field of setupData.
                HScreenDevice device = XMLTestConfig.findNamedDevice(setupData[0].data.getClass().getName(),
                        setupData[0].misc);

                HScreenConfiguration config = XMLTestConfig.getBestConfig(device,
                        (HScreenConfigTemplate) setupData[0].data);

                XMLTestConfig.setCurrentConfig(device, config);
            }

            // get input data specifying which HScreen to use.
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            assertTrue("There should be only one piece of input data for test case " + i, (inputData.length == 1));
            assertTrue("The input data for test case " + i + " should be a " + "java.lang.Integer object",
                    inputData[0].data instanceof Integer);

            HScreen[] screens = HScreen.getHScreens();
            int screenNum = ((Integer) inputData[0].data).intValue();

            // get default scene from factory
            HScene defaultScene = null;
            try
            {
                defaultScene = factory.getDefaultHScene(screens[screenNum]);

                // Get result template to compare against
                TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);
                assertTrue("One and only one result element expected for test case " + i, resultData.length == 1);

                if (resultData[0].data instanceof HSceneTemplate)
                {
                    // make sure the default template matches the expected
                    // template
                    // (specified in the xml file).
                    TestSupport.assertEqual("incorrect default scene returned for testCase " + i,
                            (HSceneTemplate) resultData[0].data,
                            (defaultScene != null) ? defaultScene.getSceneTemplate() : null,
                            HSceneTemplateTest.prefNames);
                }
                else
                    fail("Invalid result data - must contain an HSceneTemplate");
            }
            finally
            {
                if (defaultScene != null)
                {
                    defaultScene.dispose();
                }
            }
        }
    }

    /**
     * Tests resizeScene().
     * 
     * Test data should be set up such that it tests the following:
     * <ul>
     * <li>Templates which request a fully possible size
     * <li>Templates which request an unattainable size, but the unattainable
     * sizes are not REQUIRED
     * <li>Templates which request unattainable and REQUIRED size preferences.
     * <li>test that LARGEST_DIMENSION does its job
     * <li>only size/location options should be considered
     * 
     */
    public void testResizeScene()
    {
        // load the "resize" test data
        String xmlFilePath = TestSupport.getProperty("XMLTestData");
        Element test = XMLTestConfig.getTestElement(xmlFilePath, getClass(), "resize");

        Element[] testCases = XMLTestConfig.getTestCases(test, null, null);
        assertTrue("No test cases found for \"besttemplate\" test", (testCases.length > 0));

        // iterate through all test cases testing resizeScene()
        for (int i = 0; i < testCases.length; i++)
        {
            // get and execute setup data
            TestData[] setupData = XMLTestConfig.getTestData("Setup", testCases[i]);

            // optional setup step to set the current configuration
            if (setupData.length > 0)
            {
                assertTrue("setup object should be an HGraphicsConfigTemplate for " + "test case " + i,
                        setupData[0].data instanceof HGraphicsConfigTemplate);

                // find the device to set currentConfig on.
                // it should have been specified on the template element
                // and consequently set in the misc field of setupData.
                HScreenDevice device = XMLTestConfig.findNamedDevice(setupData[0].data.getClass().getName(),
                        setupData[0].misc);

                HScreenConfiguration config = XMLTestConfig.getBestConfig(device,
                        (HScreenConfigTemplate) setupData[0].data);

                XMLTestConfig.setCurrentConfig(device, config);
            }

            // Get the input templates to test with (2 total)
            TestData[] inputData = XMLTestConfig.getTestData("Input", testCases[i]);

            // Get the input template...
            TestData[] srcTemp = XMLTestConfig.subsetTestData(inputData, "inputtemplate");
            assertTrue("One and only one \"inputtemplate\" template expected for test case " + i, srcTemp.length == 1);
            assertTrue("\"inputtemplate\" must be of type org.havi.ui.HSceneTemplate for test case " + i,
                    srcTemp[0].data instanceof HSceneTemplate);

            // Get the resize template...
            TestData[] resizeTemp = XMLTestConfig.subsetTestData(inputData, "resizetemplate");
            assertTrue("One and only one \"resizetemplate\" template expected for test case " + i,
                    resizeTemp.length == 1);
            assertTrue("\"resizetemplate\" must be of type org.havi.ui.HSceneTemplate for test case " + i,
                    resizeTemp[0].data instanceof HSceneTemplate);

            // extract the templates
            HSceneTemplate inputTemplate = (HSceneTemplate) inputData[0].data;
            HSceneTemplate resizeTemplate = (HSceneTemplate) inputData[1].data;

            // get best HScene from factory
            HScene bestScene = null;
            try
            {
                bestScene = factory.getBestScene(inputTemplate);

                // get the resized HScene from the factory
                HSceneTemplate resizedTemplate = factory.resizeScene(bestScene, resizeTemplate);

                // get result template to compare against
                TestData[] resultData = XMLTestConfig.getTestData("Result", testCases[i]);
                assertTrue("One and only one result element expected for test case " + i, resultData.length == 1);

                if (resultData[0].data instanceof HSceneTemplate)
                {
                    // make sure the template returned from resizeScene()
                    // matches the expected template (specified in the xml
                    // file).
                    TestSupport.assertEqual("incorrect resized scene returned for testCase " + i,
                            (HSceneTemplate) resultData[0].data, resizedTemplate, HSceneTemplateTest.prefNames);
                }
                else
                    fail("Invalid result data - must contain an HSceneTemplate");
            }
            finally
            {
                if (bestScene != null)
                {
                    bestScene.dispose();
                }
            }
        }
    }

    /**
     * Tests getFullScreenScene().
     * <ul>
     * <li>Test with all HGraphicsConfigurations on all HGraphicsDevices on all
     * HScreens.
     * <li>A full-screen scene should be created on HGraphicsConfigurations that
     * are full screen themselves. ( null otherwise )
     * </ul>
     */
    public void testFullScreenScene()
    {
        HScreen[] screens = HScreen.getHScreens();
        for (int scrNum = 0; scrNum < screens.length; scrNum++)
        {
            HGraphicsDevice[] devices = screens[scrNum].getHGraphicsDevices();
            if (devices == null) continue;

            for (int devNum = 0; devNum < devices.length; devNum++)
            {
                // try to get a full screen scene
                HScene scene = null;
                try
                {
                    scene = factory.getFullScreenScene(devices[devNum]);
                    HScreenRectangle screenRect = null;
                    if (scene != null)
                    {
                        HSceneTemplate template = scene.getSceneTemplate();

                        // build up HScreenRectangle
                        HScreenDimension screenDim = (HScreenDimension) template.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_DIMENSION);
                        HScreenPoint screenLoc = (HScreenPoint) template.getPreferenceObject(HSceneTemplate.SCENE_SCREEN_LOCATION);
                        screenRect = new HScreenRectangle(screenLoc.x, screenLoc.y, screenDim.width, screenDim.height);
                    }

                    // see if that device has any full screen configurations
                    boolean hasFullScreenConfig = false;
                    HGraphicsConfiguration[] configs = devices[devNum].getConfigurations();
                    for (int confNum = 0; confNum < configs.length; confNum++)
                    {
                        if (isFullScreen(configs[confNum].getScreenArea()))
                        {
                            hasFullScreenConfig = true;
                            break;
                        }
                    }

                    if (hasFullScreenConfig)
                    {
                        assertTrue("Should be able to allocate a full screen scene " + "on device <"
                                + devices[devNum].getIDstring() + ">", isFullScreen(screenRect));
                    }
                    else
                    {
                        assertNull("Should NOT be able to allocate a full screen scene " + "on device <"
                                + devices[devNum].getIDstring() + ">", scene);
                    }
                }
                finally
                {
                    scene.dispose();
                }
            }
        }
    }

    /**
     * Tests dispose().
     * <ul>
     * <li>Test that the HScene throws java.lang.IllegalStateException after
     * being disposed of
     * </ul>
     */
    public void testDispose()
    {
        HScene hs = factory.getDefaultHScene();
        factory.dispose(hs);
        try
        {
            factory.dispose(hs);
        }
        catch (Exception ignored)
        {
            fail("Calling HSceneFactory.dispose() more than once " + "should have no effect");
        }
        try
        {
            factory.resizeScene(hs, new HSceneTemplate());
            fail("Exception should be thrown given usage of HScene following disposal");
        }
        catch (IllegalStateException ignored)
        {
        }
        try
        {
            hs.setVisible(true);
            fail("Exception should be thrown given usage of HScene following" + "disposal");
        }
        catch (Exception ignored)
        {
        }
    }

    private boolean isFullScreen(HScreenRectangle rect)
    {
        if (rect == null)
            return false;
        else
            return (rect.x == 0.0f && rect.y == 0.0f && rect.width == 1.0f && rect.height == 1.0f);
    }

}
