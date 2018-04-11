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
import net.sourceforge.groboutils.junit.v1.iftc.*;

import java.awt.Dimension;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import org.havi.ui.HScreenDeviceTest.TestResourceClient;

/**
 * Tests {@link #HScreenTest}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski (rewritten to using InstanceTest)
 * @version $Revision: 1.2 $, $Date: 2002/06/03 21:32:20 $
 */
public class HScreenTest extends TestCase
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HScreen.class, Object.class);
    }

    /**
     * Test for not public constructors.
     */
    public void testConstructors()
    {
        // HScreen is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(HScreen.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HScreen.class);
    }

    /**
     * Test getHScreens.
     */
    public void testHScreens()
    {
        HScreen[] screens = HScreen.getHScreens();

        assertNotNull("Should return non-null", screens);
        assertTrue("Should return array of at least 1 HScreen", screens.length >= 1);

        // Verify that entries are non-null and distinct
        Hashtable map = new Hashtable();
        for (int i = 0; i < screens.length; ++i)
        {
            assertNotNull("Screen[" + i + "] was null", screens[i]);
            assertTrue("Screen[" + i + "] was repeated in getHScreens", map.get(screens[i]) == null);
            map.put(screens[i], screens[i]);
        }

        // Verify that Number of defined screens matches number of expected
        // screens
        XMLDispConfig test = new XMLDispConfig();
        Element[] xscreens = test.getScreens();

        assertEquals("Unexpected number of screens", xscreens.length, screens.length);

        // Verify that overwriting array doesn't affect original
        for (int i = 0; i < screens.length; ++i)
            screens[i] = null;
        HScreen[] screens2 = HScreen.getHScreens();
        for (int i = 0; i < screens2.length; ++i)
        {
            assertNotNull("Array doesn't appear to be a copy", screens2[i]);
        }
    }

    /**
     * Test getDefaultHScreen.
     */
    public void testDefaultHScreen()
    {
        HScreen[] screens = HScreen.getHScreens();
        HScreen defScreen = HScreen.getDefaultHScreen();

        assertNotNull("Default screen should not be null", defScreen);

        // Ensure that default is valid
        boolean found = false;
        for (int i = 0; i < screens.length; ++i)
        {
            if (screens[i] == defScreen)
            {
                found = true;
                break;
            }
        }
        assertTrue("Default screen isn't one of the valid screens", found);

        // How do we verify the default screen is the correct one?
        // Screens don't have any distinguishing characteristics, other than
        // the supported devices and configurations (and coherent configs).
        // For now we'll skip it.
        if (false)
        {
            XMLDispConfig test = new XMLDispConfig();
            Element[] xscreens = test.getScreens();
        }
    }

    static boolean isConfigSupported(HScreenConfigTemplate t, HScreenConfiguration c)
    {
        if (t instanceof HGraphicsConfigTemplate && c instanceof HGraphicsConfiguration)
            return ((HGraphicsConfigTemplate) t).isConfigSupported((HGraphicsConfiguration) c);
        else if (t instanceof HVideoConfigTemplate && c instanceof HVideoConfiguration)
            return ((HVideoConfigTemplate) t).isConfigSupported((HVideoConfiguration) c);
        else if (t instanceof HBackgroundConfigTemplate && c instanceof HBackgroundConfiguration)
            return ((HBackgroundConfigTemplate) t).isConfigSupported((HBackgroundConfiguration) c);
        else
            return false;
    }

    /**
     * Interface test case. Used to test instances of HScreen.
     */
    public static class InstanceTest extends InterfaceTestCase
    {
        /**
         * Test getDefaultHBackgroundDevice.
         */
        public void testDefaultHBackgroundDevice()
        {
            doTestDefaultDevice(screen.getDefaultHBackgroundDevice(), screen.getHBackgroundDevices(), "background");
        }

        /**
         * Test getDefaultHGraphicsDevice.
         */
        public void testDefaultHGraphicsDevice()
        {
            doTestDefaultDevice(screen.getDefaultHGraphicsDevice(), screen.getHGraphicsDevices(), "graphics");
        }

        /**
         * Test getDefaultHVideoDevice.
         */
        public void testDefaultHVideoDevice()
        {
            doTestDefaultDevice(screen.getDefaultHVideoDevice(), screen.getHVideoDevices(), "video");
        }

        /**
         * Test getHBackgroundDevices.
         */
        public void testHBackgroundDevices()
        {
            doTestDevices(screen.getHBackgroundDevices(), "background");
        }

        /**
         * Test getHGraphicsDevices.
         */
        public void testHGraphicsDevices()
        {
            doTestDevices(screen.getHGraphicsDevices(), "graphics");
        }

        /**
         * Test getHVideoDevices.
         */
        public void testHVideoDevices()
        {
            doTestDevices(screen.getHVideoDevices(), "video");
        }

        private HScreenConfigTemplate[] getCoherentTemplates(Element xcoherent)
        {
            Element[] xset = test.getCoherentElements(xcoherent, false);
            assertTrue("Internal error - need coherent config of more than one device", xset.length > 1);

            HScreenConfigTemplate[] set = new HScreenConfigTemplate[xset.length];
            for (int i = 0; i < set.length; ++i)
            {
                set[i] = test.toTemplate(xset[i]);
            }

            return set;
        }

        /**
         * Test getCoherentScreenConfigurations
         * 
         * Go over the expected set of coherent configurations. Create templates
         * for each and ensure that they return what is expected.
         */
        public void testGetCoherentScreenConfigurations_expected() throws Exception
        {
            Element[] xcoherent = test.getCoherentConfigs(xscreen);

            for (int i = 0; i < xcoherent.length; ++i)
            {
                // Get templates for coherent configuration
                HScreenConfigTemplate[] set = getCoherentTemplates(xcoherent[i]);
                // Lookup coherent configurations
                HScreenConfiguration[] coherent = screen.getCoherentScreenConfigurations(set);

                // Verify coherent configurations
                assertNotNull("Expected a non-null coherent configuration to be returned", coherent);
                assertTrue("Expected a coherent configuration to be returned", coherent.length >= set.length);

                // Verify non-null elements
                for (int j = 0; j < coherent.length; ++j)
                    assertNotNull("Expected non-null elements in array", coherent[j]);

                // Validate expected configs
                for (int j = 0; j < set.length; ++j)
                {
                    boolean found = false;
                    for (int h = 0; h < coherent.length; ++h)
                    {
                        if (coherent[h] != null && HScreenTest.isConfigSupported(set[j], coherent[h]))
                        {
                            found = true;
                            coherent[h] = null;
                            break;
                        }
                    }
                    assertTrue("Did not find a match for the desired template " + j, found);
                }

                // !!! Check that second call returns same
                // !!! Check that second call isn't affected by writing to array
            }

            // !!!! TODO

            // Find coherent configs using a single template

            // Find coherent configs using minimal templates (as can be found by
            // HScreenDeviceTest)...
            // Validate them just the same.

            // Find coherent configs that CANNOT be matched
            // And make sure that they aren't!
            // Based on 1) single configs that cannot be matched
            // and 2) combos that cannot be matched (tougher)
        }

        /**
         * Tests setCoherentConfigurations.
         * 
         * Go over the expected set of coherent configurations and ensure that
         * they can be set. Ensure that they are set correctly.
         */
        public void testSetCoherentScreenConfigurations_expected() throws Exception
        {
            Element[] xcoherent = test.getCoherentConfigs(xscreen);

            for (int i = 0; i < xcoherent.length; ++i)
            {
                // Get templates for coherent configuration
                HScreenConfigTemplate[] set = getCoherentTemplates(xcoherent[i]);
                // Lookup coherent configurations
                HScreenConfiguration[] coherent = screen.getCoherentScreenConfigurations(set);

                assertNotNull("Expected coherent configurations - " + i, coherent);

                // Try to set coherent config
                // Requires us to reserve each device
                try
                {
                    // !!!!! TODO !!!!!
                    // Might be a bug.
                    // Hmmmm.... coherent configs doesn't return configs for all
                    // if one is NOT_CONTRIBUTING...
                    // So we won't reserver the video device here.
                    // Question is: should we reserve it (how do we know) or
                    // should it be implicitly reserved?
                    reserveDevices(coherent);

                    // Set coherent config
                    assertTrue("Should've been able to set coherent config",
                            screen.setCoherentScreenConfigurations(coherent));

                    // Check that current configs changed
                    for (int j = 0; j < coherent.length; ++j)
                    {
                        HScreenDevice dev = HScreenDeviceTest.getDevice(coherent[j]);
                        HScreenConfiguration curr = HScreenDeviceTest.getCurrConfig(dev);

                        assertSame("Current configuration expected to be set by coherent configs", coherent[j], curr);
                    }
                }
                finally
                {
                    releaseDevices(coherent);
                }
            }
        }

        /**
         * Test that getCoherentScreenConfigurations() fails correctly when a
         * template isn't compatible with other templates.
         */
        public void testGetCoherentScreenConfigurations_incompat() throws Exception
        {
            // Get all combos of configurations
            // If a combo is expected, skip it
            // Else, ensure that null is returned

            Vector combos = combineConfigs(getDevices(), 0);
            Element[] xcoherent = test.getCoherentConfigs(xscreen);

            // Foreach combination
            for (int i = 0; i < combos.size(); ++i)
            {
                Vector v = (Vector) combos.elementAt(i);
                HScreenConfiguration[] configs = new HScreenConfiguration[v.size()];
                v.copyInto(configs);

                boolean found = false;
                for (int j = 0; j < xcoherent.length; ++j)
                {
                    // Get templates for coherent configuration
                    HScreenConfigTemplate[] set = getCoherentTemplates(xcoherent[j]);

                    if (isConfigSupported(set, configs))
                    {
                        found = true;
                        break;
                    }
                }
                // Known to be coherent config, skip it
                if (found)
                {
                    continue;
                }

                // Make sure getCoherentConfigurations() doesn't return
                // something
                // Get templates
                HScreenConfigTemplate[] templates = new HScreenConfigTemplate[configs.length];
                for (int j = 0; j < templates.length; ++j)
                    templates[j] = HScreenDeviceTest.getTemplate(configs[j]);

                assertNull("Invalid coherent templates should return null for configurations",
                        screen.getCoherentScreenConfigurations(templates));
            }
        }

        /**
         * Test the reservation checks of setCoherentScreenConfigurations.
         */
        public void testSetCoherentScreenConfigurations_reservation() throws Exception
        {
            // If a device isn't reserved...
            // And we call setCoherentScreenConfigurations... should get
            // a PermissionDeniedException

            Element[] xcoherent = test.getCoherentConfigs(xscreen);
            if (xcoherent.length > 0)
            {
                HScreenConfigTemplate[] set = getCoherentTemplates(xcoherent[0]);
                HScreenConfiguration[] configs = screen.getCoherentScreenConfigurations(set);
                assertNotNull("Should return coherent screen configurations", configs);

                // Reserve all devices but one
                // and try setCoherentScreenConfigurations
                for (int i = 0; i < set.length; ++i)
                {
                    try
                    {
                        // reserve devices
                        reserveDevices(configs);

                        // release one device
                        HScreenDeviceTest.getDevice(configs[i]).releaseDevice();

                        // set coherent config
                        // should fail!
                        try
                        {
                            screen.setCoherentScreenConfigurations(configs);
                            fail("Expected HPermissionDeniedException");
                        }
                        catch (HPermissionDeniedException e)
                        {
                        }
                    }
                    finally
                    {
                        releaseDevices(configs);
                    }
                }
            }
        }

        /**
         * Test that setCoherentScreenConfigurations() fails correctly when a
         * configuration isn't compatible with other configurations.
         */
        public void testSetCoherentScreenConfigurations_incompat() throws Exception
        {
            // Generate vector of vectors
            Vector combos = combineConfigs(getDevices(), 0);

            // Foreach combination
            for (Enumeration e = combos.elements(); e.hasMoreElements();)
            {
                // Generate templates array
                Vector v = (Vector) e.nextElement();
                HScreenConfiguration[] configs = new HScreenConfiguration[v.size()];
                v.copyInto(configs);
                HScreenConfigTemplate[] templates = new HScreenConfigTemplate[configs.length];

                for (int i = 0; i < templates.length; ++i)
                    templates[i] = HScreenDeviceTest.getTemplate(configs[i]);

                // Call getCoherentScreenConfigurations
                HScreenConfiguration[] coherent = screen.getCoherentScreenConfigurations(templates);
                if (coherent == null)
                {
                    // Doesn't describe a coherent config
                    // Make sure setCoherentScreenConfigurations knows this
                    try
                    {
                        reserveDevices(configs);

                        assertFalse("Expected setCoherentScreenConfiguration to return false",
                                screen.setCoherentScreenConfigurations(configs));
                    }
                    catch (HConfigurationException ex)
                    {
                        // These are valid configurations!
                        fail("HConfigurationException not expected for valid configs");
                    }
                    finally
                    {
                        releaseDevices(configs);
                    }
                }
            }
        }

        /**
         * Tests getCoherentConfiguration() never returns
         * HVideoDevice.NOT_CONTRIBUTING. Should return null if NOT_CONTRIBUTING
         * is involved.
         */
        public void testGetCoherentConfigurations_NOTCONTRIB()
        {
            if (HVideoDevice.NOT_CONTRIBUTING == null) return;

            Element[] xcoherent = test.getCoherentConfigs(xscreen);

            for (int i = 0; i < xcoherent.length; ++i)
            {
                // Get templates for coherent configuration
                HScreenConfigTemplate[] set = getCoherentTemplates(xcoherent[i]);

                // Replace video templates with that for
                // HVideoDevice.NOT_CONTRIB
                boolean replaced = false;
                for (int j = 0; j < set.length; ++j)
                    if (set[j] instanceof HVideoConfigTemplate)
                    {
                        replaced = true;
                        set[j] = HVideoDevice.NOT_CONTRIBUTING.getConfigTemplate();
                    }
                // No HVideoDevice, so skip this one...
                if (!replaced) continue;
                // Lookup coherent configurations
                HScreenConfiguration[] coherent = screen.getCoherentScreenConfigurations(set);

                // Should always return null
                assertSame("Expected null array returned for NOT_CONTRIBUTING template", null, coherent);
            }
        }

        /**
         * Determines if the given set of configurations is supported by the
         * given set of templates.
         */
        private boolean isConfigSupported(HScreenConfigTemplate[] templates, HScreenConfiguration[] configs)
        {
            for (int i = 0; i < configs.length; ++i)
            {
                boolean found = false;
                for (int j = 0; j < templates.length; ++j)
                {
                    if (HScreenTest.isConfigSupported(templates[j], configs[i]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }

        /**
         * Generates a combination of all devices configurations. Given n
         * devices with m configurations, expect a Vector of m^n vectors. This
         * is a recursive call that stops when idx == devs.length-1.
         * 
         * @param devs
         *            the array of devices
         * @param idx
         *            the index of the current device
         * @return a Vector of Vectors containing configurations
         */
        private Vector combineConfigs(HScreenDevice[] devs, int idx) throws Exception
        {
            int next = idx + 1;
            HScreenConfiguration[] mine = HScreenDeviceTest.getConfigs(devs[idx]);
            Vector v = new Vector();

            if (next >= devs.length)
            {
                // Simply return my configurations
                // In an array big enough for all devices
                for (int i = 0; i < mine.length; ++i)
                {
                    Vector array = new Vector();
                    array.addElement(mine[i]);
                    v.addElement(array);
                }
            }
            else
            {
                // Recurse to other devices
                Vector sub = combineConfigs(devs, next);
                for (int j = 0; j < sub.size(); ++j)
                {
                    Vector array = (Vector) sub.elementAt(j);
                    for (int i = 0; i < mine.length; ++i)
                    {
                        // mix mine[i] with elements in sub
                        Vector copy = (Vector) array.clone();
                        copy.addElement(mine[i]);
                        v.addElement(copy);
                    }
                }
            }
            return v;
        }

        private void reserveDevices(HScreenConfiguration[] configs)
        {
            TestResourceClient rc = new TestResourceClient();
            for (int j = 0; j < configs.length; ++j)
            {
                assertTrue("Should've been able to reserve device", HScreenDeviceTest.getDevice(configs[j])
                        .reserveDevice(rc));
            }
        }

        private void releaseDevices(HScreenConfiguration[] configs)
        {
            TestResourceClient rc = new TestResourceClient();
            for (int j = 0; j < configs.length; ++j)
            {
                HScreenDeviceTest.getDevice(configs[j]).releaseDevice();
            }
        }

        /**
         * Returns all of the screen devices contained within this screen.
         */
        private HScreenDevice[] getDevices()
        {
            HScreenDevice[] gfx = screen.getHGraphicsDevices();
            HScreenDevice[] bg = screen.getHBackgroundDevices();
            HScreenDevice[] vid = screen.getHVideoDevices();

            HScreenDevice[] all = new HScreenDevice[gfx.length + bg.length + vid.length];
            System.arraycopy(gfx, 0, all, 0, gfx.length);
            System.arraycopy(vid, 0, all, gfx.length, vid.length);
            System.arraycopy(bg, 0, all, gfx.length + vid.length, bg.length);

            return all;
        }

        /**
         * Checks the valid devices.
         */
        private void doTestDevices(HScreenDevice devices[], String type)
        {
            Element[] xdevices = test.getDevices(xscreen, type);

            assertNotNull("Should return non-null array", devices);
            assertEquals("Unexpected number of devices", xdevices.length, devices.length);

            for (int i = 0; i < xdevices.length; ++i)
            {
                String id = xdevices[i].getAttribute("id");
                boolean found = false;
                for (int j = 0; j < devices.length; ++j)
                {
                    if (devices[j] != null && id.equals(devices[j].getIDstring()))
                    {
                        devices[j] = null;
                        found = true;
                        break;
                    }
                }
                assertTrue("Did not find expected device id=" + id, found);
            }
        }

        /**
         * Checks the default device.
         */
        private void doTestDefaultDevice(HScreenDevice defDev, HScreenDevice devices[], String type)
        {
            Element[] xdefault = test.getDefaultDevices(xscreen, type);
            assertFalse("Internal error - too many default devices specified", xdefault.length > 1);
            if (devices.length == 0)
            {
                assertNull("Given no devices, don't expect a default", defDev);
                assertTrue("Default device was expected", xdefault.length == 0);
            }
            else
            {
                assertTrue("Default device was unexpected", xdefault.length != 0);

                // Find default device amongst devices
                boolean found = false;
                for (int i = 0; i < devices.length; ++i)
                {
                    if (defDev == devices[i])
                    {
                        found = true;
                        break;
                    }
                }
                assertTrue("Default device isn't amongst valid devices", found);

                // Check device name
                assertEquals("Unexpected IDstring for default device", xdefault[0].getAttribute("id"),
                        defDev.getIDstring());
            }
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(InstanceTest.class);
            suite.setName(HScreen.class.getName());
            return suite;
        }

        public InstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected InstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }

        protected Element xscreen;

        protected HScreen screen;

        protected XMLDispConfig test;

        protected void setUp() throws Exception
        {
            super.setUp();

            TestParam param = (TestParam) createImplObject();
            xscreen = param.xscreen;
            screen = param.screen;
            test = param.test;
        }

        protected void tearDown() throws Exception
        {
            xscreen = null;
            screen = null;
            test = null;

            super.tearDown();
        }

        /**
         * Type of object returned by ImplFactory.createImplObject().
         */
        public static class TestParam implements ImplFactory
        {
            public HScreen screen;

            public Element xscreen;

            public XMLDispConfig test;

            public TestParam(HScreen screen, Element xscreen, XMLDispConfig test)
            {
                this.screen = screen;
                this.xscreen = xscreen;
                this.test = test;
            }

            public Object createImplObject()
            {
                return this;
            }

            public String toString()
            {
                String id = (xscreen != null) ? xscreen.getAttribute("id") : null;
                return (id == null) ? super.toString() : id;
            }
        }
    }

    /* Boilerplate */

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite() throws Exception
    {
        /* HScreenTest */
        TestSuite suite = new TestSuite(HScreenTest.class);

        /* HScreenTest.InstanceTest foreach instance. */
        InterfaceTestSuite isuite = InstanceTest.isuite();
        HScreen screens[] = HScreen.getHScreens();
        if (screens != null)
        {
            XMLDispConfig test = new XMLDispConfig();
            Element xscreens[] = test.getScreens();
            for (int i = 0; i < screens.length; ++i)
            {
                if (screens[i] != null)
                {
                    isuite.addFactory(new InstanceTest.TestParam(screens[i], xscreens[i], test));
                }
            }
        }
        suite.addTest(isuite);

        return suite;
    }

    public HScreenTest(String name)
    {
        super(name);
    }
}
