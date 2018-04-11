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

/**
 * Tests {@link #HSceneTemplate}.
 * 
 * @author Aaron Kamienski
 * @author Jay Tracy (1.01b updates)
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:18 $
 */
public class HSceneTemplateTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HSceneTemplateTest(String str)
    {
        super(str);
    }

    private HSceneTemplate hst;

    private HScene scene;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hst = new HSceneTemplate();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
        if (scene != null) HSceneFactory.getInstance().dispose(scene);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HSceneTemplateTest.class);
    }

    /**
     * Test the 1 constructors of HVisible.
     * <ul>
     * <li>HSceneTemplate()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HSceneTemplate()", hst);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HSceneTemplate hst)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", hst);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HSceneTemplate.class);
    }

    /**
     * Tests for no unexpected fields and that expected ones are unique.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HSceneTemplate.class, fields);
        TestUtils.testUniqueFields(HSceneTemplate.class, fields, false, 0, 5);
        TestUtils.testUniqueFields(HSceneTemplate.class, fields, false, 5, 3);
    }

    private static final String[] fields = { "GRAPHICS_CONFIGURATION", "SCENE_PIXEL_DIMENSION", "SCENE_PIXEL_LOCATION",
            "SCENE_SCREEN_DIMENSION", "SCENE_SCREEN_LOCATION", "REQUIRED", "PREFERRED", "UNNECESSARY",
            "LARGEST_PIXEL_DIMENSION" };

    private final int preferences[] = { HSceneTemplate.GRAPHICS_CONFIGURATION, HSceneTemplate.SCENE_PIXEL_DIMENSION,
            HSceneTemplate.SCENE_PIXEL_LOCATION, HSceneTemplate.SCENE_SCREEN_DIMENSION,
            HSceneTemplate.SCENE_SCREEN_LOCATION };

    public static final String prefNames[] = { "GRAPHICS_CONFIGURATION", "SCENE_PIXEL_DIMENSION",
            "SCENE_PIXEL_LOCATION", "SCENE_SCREEN_DIMENSION", "SCENE_SCREEN_LOCATION" };

    private final Object dummyPref[] = {
            HScreen.getDefaultHScreen().getDefaultHGraphicsDevice().getDefaultConfiguration(), new Dimension(200, 150),
            new Point(50, 50), new HScreenDimension(0.4f, 0.4f), new HScreenPoint(0.25f, 0.5f), };

    private final int priorities[] = { HSceneTemplate.UNNECESSARY, HSceneTemplate.REQUIRED, HSceneTemplate.PREFERRED, };

    /**
     * Tests setPreference()/getPreferenceObject()/getPreferencePriority().
     * <ul>
     * <li>Test set value/priority is retrieved value/priority.
     * <li>Test default preference priority (UNNECESSARY).
     * <li>Test default preference object (null) (this is NOT specified in the
     * 1.0 spec, but is in the 1.01, so assume that).
     * </ul>
     */
    public void testPreference()
    {
        // Check default priority/value
        for (int i = 0; i < preferences.length; ++i)
        {
            assertNull("default " + prefNames[i] + " object should be null", hst.getPreferenceObject(preferences[i]));
            assertEquals("default " + prefNames[i] + " priority should be " + "UNNECESSARY", hst.UNNECESSARY,
                    hst.getPreferencePriority(preferences[i]));
        }

        // Check set/retrieved priority/value
        for (int i = 0; i < preferences.length; ++i)
        {
            for (int j = 0; j < priorities.length; ++j)
            {
                hst.setPreference(preferences[i], dummyPref[i], priorities[j]);
                assertSame("Set preference object should be retrieved " + "(" + prefNames[i] + "," + j + ")",
                        dummyPref[i], hst.getPreferenceObject(preferences[i]));
                assertEquals("Set preference priority should be retrieved " + "(" + prefNames[i] + "," + j + ")",
                        priorities[j], hst.getPreferencePriority(preferences[i]));

                // Should allow clearing
                hst.setPreference(preferences[i], null, priorities[j]);
                assertNull("Should allow clearing of preference " + "(" + prefNames[i] + "," + j + ")",
                        hst.getPreferenceObject(preferences[i]));
            }
        }
    }

    /**
     * Tests LARGEST_DIMENSION field. Note, this object is not necessarily the
     * same or equal to a Dimension object specifying the full-screen.
     */
    public void testLargestDimension()
    {
        assertTrue("LARGEST_DIMENSION should be a Dimension object",
                HSceneTemplate.LARGEST_PIXEL_DIMENSION instanceof Dimension);
    }

    /**
     * Tests GRAPHICS_CONFIGURATION preference.
     * <ul>
     * <li>Check proper object type
     * </ul>
     */
    public void testGraphicsConfig()
    {
        fillTemplate();
        checkPreference(hst, hst.GRAPHICS_CONFIGURATION, HGraphicsConfiguration.class);
    }

    /**
     * Tests SCENE_PIXEL_DIMENSION preference.
     * <ul>
     * <li>Check proper object type
     * </ul>
     */
    public void testPixelDimension()
    {
        fillTemplate();
        checkPreference(hst, hst.SCENE_PIXEL_DIMENSION, Dimension.class);
    }

    /**
     * Tests SCENE_PIXEL_LOCATION preference.
     * <ul>
     * <li>Check proper object type
     * </ul>
     */
    public void testPixelLocation()
    {
        fillTemplate();
        checkPreference(hst, hst.SCENE_PIXEL_LOCATION, Point.class);
    }

    /**
     * Tests SCENE_SCREEN_DIMENSION preference.
     * <ul>
     * <li>Check proper object type
     * </ul>
     */
    public void testScreenDimension()
    {
        fillTemplate();
        checkPreference(hst, hst.SCENE_SCREEN_DIMENSION, HScreenDimension.class);
    }

    /**
     * Tests SCENE_SCREEN_LOCATION preference.
     * <ul>
     * <li>Check proper object type
     * </ul>
     */
    public void testScreenLocation()
    {
        fillTemplate();
        checkPreference(hst, hst.SCENE_SCREEN_LOCATION, HScreenPoint.class);
    }

    /**
     * Check for proper preference Object type.
     */
    private void checkPreference(HSceneTemplate hst, int preference, Class cl)
    {
        Object pref = hst.getPreferenceObject(preference);

        assertNotNull("Preference object should be set", pref);
        assertTrue("Preference object should be instanceof " + cl.getName(), cl.isInstance(pref));
    }

    /**
     * Get a filled-in HSceneTemplate.
     */
    private void fillTemplate()
    {
        scene = HSceneFactory.getInstance().getDefaultHScene();
        hst = scene.getSceneTemplate();
    }
}
