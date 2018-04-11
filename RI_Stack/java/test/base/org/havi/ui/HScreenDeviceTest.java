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

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.cablelabs.impl.manager.resource.NotifySetWarningPeriod;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.davic.resources.ResourceServer;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.havi.ui.event.HScreenConfigurationEvent;
import org.havi.ui.event.HScreenConfigurationListener;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.w3c.dom.Element;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.ResourceManager.Client;
import org.cablelabs.impl.ocap.resource.ApplicationResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.test.TestUtils;

/**
 * Tests {@link #HScreenDevice}.
 * 
 * @todo Add tests to test for deadlock avoidance. E.g., what happens if
 *       ResourceClient.requestRelease()/release() turn around and call
 *       reserveDevice() or releaseDevice()?
 * @todo Add test to verify correctness when more than one thread/app is trying
 *       to reserve the device at the same time.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @author Alex Resh
 * @author Aaron Kamienski
 * 
 * @version $Revision: 1.9 $, $Date: 2002/06/03 21:32:19 $
 */
public class HScreenDeviceTest extends TestCase
{
    private static final boolean DEBUG = false;

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>implements ResourceServer
     * <li>implements ResourceProxy
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testImplements(HScreenDevice.class, ResourceServer.class);
        TestUtils.testImplements(HScreenDevice.class, ResourceProxy.class);
    }

    /**
     * Test the single constructor of HScreenDevice.
     * <ul>
     * HScreenDevice()
     * </ul>
     */
    public void testConstructors()
    {
        // HScreenDevice is unable to be explicitly instantiated.
        TestUtils.testNoPublicConstructors(HScreenDevice.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HScreenDevice.class);
    }

    /**
     * Interface test case. Used to test instances of HScreenDevice. (Expected
     * to be extended by subclass tests.)
     */
    public static class InstanceTest extends InterfaceTestCase
    {
        /**
         * Tests getConfigurations().
         * <ul>
         * <li>Should return expected number of configurations for a device.
         * <li>Should return expected configurations for a device.
         * </ul>
         */
        public void testConfigurations() throws Exception
        {
            Element[] xconfigs = test.getConfigurations(xdevice);
            HScreenConfiguration[] configs = getConfigs(device);

            assertEquals("Unexpected number of configurations", xconfigs.length, configs.length);

            // Look for each expected configuration (using a template)
            for (int t = 0; t < xconfigs.length; ++t)
            {
                HScreenConfigTemplate template = test.toTemplate(xconfigs[t]);
                boolean found = false;
                // Look for this config in set of configs
                for (int c = 0; c < configs.length; ++c)
                {
                    if (configs[c] != null && HScreenTest.isConfigSupported(template, configs[c]))
                    {
                        // Mark config as found
                        configs[c] = null;
                        found = true;
                        break;
                    }
                }
                assertTrue("Could not find config " + xconfigs[t].getAttribute("id"), found);
            }

            // Make sure our modifications didn't affect original
            HScreenConfiguration[] configs2 = getConfigs(device);
            assertEquals("Unexpected number of configurations", configs.length, configs2.length);
            for (int i = 0; i < configs2.length; ++i)
                assertNotNull("Looks like we overwrote a shared array of configs", configs2[i]);
        }

        /**
         * Tests getCurrentConfiguration().
         * <ul>
         * <li>Configuration used for the device.
         * </ul>
         */
        public void testCurrentConfiguration() throws Exception
        {
            device.reserveDevice(new TestResourceClient());
            try
            // reserve/release
            {
                HScreenConfiguration[] configs = getConfigs(device);
                HScreenConfiguration current = getCurrConfig(device);

                assertNotNull("Initial current config should be non-null", current);

                for (int i = 0; i < configs.length; i++)
                {
                    setConfig(device, configs[i]);

                    // ensure that the current config is the set config.
                    current = getCurrConfig(device);
                    assertSame("retrieved configuration should equal set configuration", configs[i], current);
                    assertSame("current configuration should return same", current, getCurrConfig(device));

                }
            }
            finally
            {
                device.releaseDevice();
            }
        }

        /**
         * Test getDefaultConfiguration().
         */
        public void testDefaultConfiguration() throws Exception
        {
            HScreenConfiguration defConfig = getDefConfig(device);

            assertNotNull("Default configuration should not be null", defConfig);
            assertSame("Default configuration should not change between calls", defConfig, getDefConfig(device));

            // Ensure that default is found in configs
            HScreenConfiguration configs[] = getConfigs(device);
            OUT: do
            {
                for (int i = 0; i < configs.length; ++i)
                {
                    if (defConfig == configs[i]) break OUT;
                }
                fail("Default configuration not a valid config");
            }
            while (false);

            // Ensure that it doesn't change as config changes
            device.reserveDevice(new TestResourceClient());
            try
            {
                for (int i = 0; i < configs.length; ++i)
                {
                    if (defConfig == configs[i]) continue;

                    setConfig(device, configs[i]);

                    assertSame("Default configuration should be constant", defConfig, getDefConfig(device));
                }
            }
            finally
            {
                device.releaseDevice();
            }

            // If there is a fixed default configuration, check it
            Element xconfig = test.getDefaultConfiguration(xdevice);
            if (xconfig != null)
            {
                assertTrue("Configuration doesn't match expected default config", HScreenTest.isConfigSupported(
                        test.toTemplate(xconfig), defConfig));
            }
        }

        /**
         * Test the setGraphicsConfiguration method
         * <ul>
         * <li>Check for proper exception throwing (SecurityException,
         * HPermissionDeniedException, and HConfigurationException)
         * <li>Check that any registered HScreenConfigurationListeners are
         * notified when the configuration changes.
         * </ul>
         */
        public void testSetConfiguration() throws Exception
        {
            TestResourceClient clientListener = new TestResourceClient();
            assertTrue("Should've been able to reserve the device", device.reserveDevice(clientListener));
            try
            // reserver/release
            {
                try
                {
                    setConfig(device, null);
                    fail("HGraphicsDevice should throw a HConfigurationException "
                            + "when trying to set a null configuration on device <" + device.getIDstring() + ">");
                }
                catch (HConfigurationException e)
                {
                }
                catch (HPermissionDeniedException e)
                {
                    fail("Incorrect exception thrown");
                }

                HScreenConfiguration configs[] = getConfigs(device);

                // Reset the configuration to the first in the list.
                setConfig(device, configs[0]);

                for (int y = configs.length - 1; y >= 0; y--)
                {
                    try
                    {
                        // Only check it if the setConfiguration actually
                        // worked.
                        if (setConfig(device, configs[y]))
                        {
                            assertSame("The set configuration should equal the retrieved configuration", configs[y],
                                    getCurrConfig(device));
                        }
                    }
                    catch (Exception e)
                    {
                        fail("Error occurred when testing the setting of a Screen " + "Configuration on device <"
                                + device.getIDstring() + ">" + " and Configuration <" + y + ">");
                    }
                }

                final boolean okay[] = new boolean[1];

                // Reset the configuration to the first in the list.
                setConfig(device, configs[0]);

                // Add the configuration change listener.
                device.addScreenConfigurationListener(clientListener);

                // Interate backwards over the configuration array.
                for (int y = configs.length - 1; y >= 0; y--)
                {
                    clientListener.reset();

                    synchronized (clientListener)
                    {
                        // Only check it if the setConfiguration actually
                        // worked.
                        if (setConfig(device, configs[y]))
                        {
                            clientListener.wait(5000L);
                            // Check for proper call of event handler
                            assertTrue("HScreenConfigurationListener should've been called", clientListener.reportFired);
                        }
                    }
                }

                // Reset the configuration to the first in the list.
                setConfig(device, configs[0]);

                // Remove the configuration change listener.
                device.removeScreenConfigurationListener(clientListener);

                // Interate backwards over the configuration array.
                for (int y = configs.length - 1; y >= 0; y--)
                {
                    clientListener.reset();

                    // Only check it if the setConfiguration actually worked.
                    synchronized (clientListener)
                    {
                        if (setConfig(device, configs[y]))
                        {
                            clientListener.wait(500L);
                            // Check for proper non-call of event handler
                            assertFalse("HScreenConfigurationListener should NOT have been called",
                                    clientListener.reportFired);
                        }
                    }
                }
            }
            finally
            {
                device.removeScreenConfigurationListener(clientListener);
                device.releaseDevice();
            }
        }

        /**
         * Tests getBestConfiguration(). Using templates created to select a
         * given configuration, check that the desired configuration is
         * returned.
         * <ul>
         * <li>template returned from getConfigTemplate().
         * <li>template returned from getConfigTemplate(), modified to be
         * <i>preferred</i> instead of <i>required</i>
         * <li>the set of minimal templates required to select the configuration
         * using <i>required</i>
         * <li>the set of minimal templates required to select the configuration
         * using <i>preferred</i>
         * </ul>
         */
        public void testBestConfiguration_expected()
        {
            HScreenConfiguration[] configs = getConfigs(device);

            for (int i = 0; i < configs.length; ++i)
            {
                Vector templates = findTemplates(configs[i], configs, allPrefs);

                assertTrue("Expected at least three config template for config[" + i + "]", templates.size() >= 3);

                // Check each template
                for (Enumeration e = templates.elements(); e.hasMoreElements();)
                {
                    HScreenConfigTemplate t = (HScreenConfigTemplate) e.nextElement();
                    HScreenConfiguration best = getBestConfig(device, t);

                    assertNotNull("Expected a configuration be returned", best);
                    assertSame("Expected configuration for expected config template to match", configs[i], best);

                    // !!!!!! Should also do...
                    // Now modify the template...
                    // Make all DONT_CAREs -> PREFERRED/PREFERRED_NOT
                    // Look up best agains
                }
            }
        }

        /**
         * Tests getBestConfiguration() (array). Similar to
         * {@link #testBestConfiguration_expected}.
         */
        public void testBestConfigurationArray_expected()
        {
            HScreenConfiguration[] configs = getConfigs(device);

            for (int i = 0; i < configs.length; ++i)
            {
                Vector templates = findTemplates(configs[i], configs, allPrefs);
                // Templates that don't select ANY config...
                Vector badTemplates = findBadTemplates(configs, allPrefs);

                assertTrue("Expected at least three config template for config[" + i + "]", templates.size() >= 3);

                // Check each template
                for (Enumeration e = templates.elements(); e.hasMoreElements();)
                {
                    HScreenConfigTemplate t = (HScreenConfigTemplate) e.nextElement();
                    HScreenConfigTemplate array[] = createTemplateArray(configs[i], badTemplates.size() + 1);
                    badTemplates.copyInto(array);
                    array[array.length - 1] = t;
                    HScreenConfiguration best = getBestConfig(device, array);

                    assertNotNull("Expected a configuration be returned", best);
                    assertSame("Expected configuration for expected config template to match", configs[i], best);

                    // !!!!!! Should also do...
                    // Now modify the template...
                    // Make all DONT_CAREs -> PREFERRED/PREFERRED_NOT
                    // Look up best agains
                }

                HScreenConfigTemplate array[] = createTemplateArray(configs[i], templates.size());
                templates.copyInto(array);
                HScreenConfiguration best = getBestConfig(device, array);

                assertNotNull("Expected a configuration be returned", best);
                assertSame("Expected configuration for expected config templates to match", configs[i], best);
            }
        }

        /**
         * Tests getBestConfiguration() (both kinds). Verifies that bad
         * templates don't select anybody.
         */
        public void testBestConfiguration_nobody()
        {
            HScreenConfiguration[] configs = getConfigs(device);

            Vector templates = findBadTemplates(configs, allPrefs);

            // Skip if couldn't come up with invalid templates
            if (templates.size() >= 1)
            {
                // Check each template
                for (Enumeration e = templates.elements(); e.hasMoreElements();)
                {
                    HScreenConfigTemplate t = (HScreenConfigTemplate) e.nextElement();

                    assertNull("Expected no configuration be returned", getBestConfig(device, t));

                }

                // Check all templates
                HScreenConfigTemplate array[] = createTemplateArray(configs[0], templates.size());
                templates.copyInto(array);

                assertNull("Expected no configuration be returned", getBestConfig(device, array));
            }
        }

        /**
         * Tests getBestConfiguration() . For each configuration, determine the
         * set of other configurations that it is incompatible with (by looking
         * at the expected coherent configurations). Verify that setting the
         * preference ZERO_*_IMPACT (as REQUIRED or PREFERRED) gets the expected
         * results.
         */
        public void testBestConfiguration_zeroImpact() throws Exception
        {
            HScreenConfiguration[] configs = getConfigs(device);

            for (int i = 0; i < configs.length; ++i)
            {
                HScreenConfiguration config = configs[i];
                HScreenConfigTemplate t = getTemplate(config);

                // Should be able to get it if asked for explicitly
                HScreenConfiguration best;
                best = getBestConfig(device, t);
                assertNotNull("Should be able to get if asked for explicitly", best);
                assertSame("Unexpected configuration returned", config, best);

                // Find compat/incompat configs
                Vector compat = new Vector();
                Vector incompat = new Vector();
                findCompat(config, compat, incompat);

                // Try incompat configs
                for (Enumeration e = incompat.elements(); e.hasMoreElements();)
                {
                    HScreenConfiguration other = (HScreenConfiguration) e.nextElement();

                    // Create template with ZERO_*_IMPACT
                    int pref = 0;
                    if (other instanceof HGraphicsConfiguration)
                    {
                        pref = HScreenConfigTemplate.ZERO_GRAPHICS_IMPACT;
                    }
                    else if (other instanceof HVideoConfiguration)
                    {
                        pref = HScreenConfigTemplate.ZERO_VIDEO_IMPACT;
                    }
                    else if (other instanceof HBackgroundConfiguration)
                    {
                        pref = HScreenConfigTemplate.ZERO_BACKGROUND_IMPACT;
                    }
                    else
                        fail("Internal error - unknown config type: " + other);

                    // Select other as current configuration
                    HScreenDevice otherDevice = getDevice(other);
                    assertFalse("Internal error? should not be same device!", device == otherDevice);
                    otherDevice.reserveDevice(new TestResourceClient());
                    try
                    // reserver/release
                    {
                        setConfig(otherDevice, other);

                        // Should not get with REQUIRED
                        t.setPreference(pref, HScreenConfigTemplate.REQUIRED);
                        best = getBestConfig(device, t);
                        assertSame("Expected no configuration returned for ZERO_*_IMPACT:REQUIRED", null, best);

                        /*
                         * From HBackgroundConfigTemplate: The ZERO_VIDEO_IMPACT
                         * property may be used in instances of this class to
                         * discover whether displaying background stills will
                         * have any impact on already running video.
                         * Implementations supporting the STILL_IMAGE preference
                         * shall return an HStillImageBackgroundConfiguration
                         * when requested except as described below. * If
                         * displaying an STILL_IMAGE interrupts video
                         * transiently while the image is decoded then a
                         * configuration shall not be returned if the
                         * ZERO_VIDEO_IMPACT property is present with the
                         * priority REQUIRED. * If displaying an STILL_IMAGE
                         * interrupts video while the image is decoded and for
                         * the entire period while the image is displayed then a
                         * configuration shall not be returned if the
                         * ZERO_VIDEO_IMPACT property is present with either the
                         * priorities REQUIRED or PREFERRED.
                         */
                        /*
                         * // Should not get with PREFERRED either
                         * t.setPreference(pref,
                         * HScreenConfigTemplate.PREFERRED); best =
                         * getBestConfig(device, t);assertSame(
                         * "Expected no configuration returned for ZERO_*_IMPACT:PREFERRED"
                         * , null, best);
                         */
                    }
                    finally
                    {
                        otherDevice.releaseDevice();
                    }
                }
            }
        }

        /**
         * Fills in the given <i>compat</i> and <i>incompat</i>
         * <code>Vector</code>s with the <code>HScreenConfiguration</code>s that
         * are compatible or incompatible (respectively) with the given
         * <i>config</i>.
         * 
         * <p>
         * This is accomplished by getting the set of all coherent
         * configurations and iterating over them. Foreach coherent
         * configuration, the given <i>config</i> is searched for. If it is
         * found, then all other configurations in the set are potentials for
         * <i>compatible</i> status. If <i>config</i> is not found, then all
         * other configurations are potentials for <i>incompatible</i> status.
         * <p>
         * Finally, the compatible/incompatible sets are generated from the
         * potential sets by removing the <i>intersection</i> of the potential
         * sets. This means that the configurations in <i>compat</i> are
         * <em>always</em> compatible; the configurations in <i>incompat</i> are
         * <em>never</em> compatible.
         * <p>
         * Note that <i>compat</i> and <i>incompat</i> can be reduced to size
         * zero if there is sufficient overlap.
         */
        protected void findCompat(HScreenConfiguration config, Vector compat, Vector incompat)
        {
            // Get coherent configs....
            Element[] xcoherent = test.getCoherentConfigs((Element) xdevice.getParentNode());

            Hashtable compatMap = new Hashtable();
            Hashtable incompatMap = new Hashtable();

            for (int i = 0; i < xcoherent.length; ++i)
            {
                Element[] xset = test.getCoherentElements(xcoherent[i], false);
                HScreenConfigTemplate[] set = new HScreenConfigTemplate[xset.length];
                for (int j = 0; j < set.length; ++j)
                    set[j] = test.toTemplate(xset[j]);
                HScreenConfiguration[] configs = getHScreen(device).getCoherentScreenConfigurations(set);

                // Assume correct (let HScreenTest cover correctness)
                assertNotNull("Could not get coherent set of configs", configs);

                // Is config in this list?
                boolean isCompat = false;
                for (int j = 0; !isCompat && j < configs.length; ++j)
                    isCompat = isCompat || configs[j] == config;
                Hashtable h = isCompat ? compatMap : incompatMap;
                for (int j = 0; j < configs.length; ++j)
                {
                    if (getDevice(config) != getDevice(configs[j]))
                    {
                        h.put(configs[j], configs[j]);
                    }
                }
            }

            // Generate compat Vector
            // Removing any configs that are in intersection of compat/incompat
            for (Enumeration e = compatMap.keys(); e.hasMoreElements();)
            {
                Object c = e.nextElement();
                if (incompatMap.get(c) == null)
                    compat.addElement(c);
                else
                {
                    compat.removeElement(c);
                    incompatMap.remove(c);
                }
            }

            // Generate incompat Vector
            for (Enumeration e = incompatMap.keys(); e.hasMoreElements();)
            {
                incompat.addElement(e.nextElement());
            }
        }

        /**
         * Test getIDstring.
         * <ul>
         * <li>test that an ID is always returned
         * <li>validate against expected id
         * </ul>
         */
        public void testIDstring()
        {
            String expected = xdevice.getAttribute("id");
            String id = device.getIDstring();

            assertNotNull("Expected non-null device id", device.getIDstring());
            assertEquals("Expected same device id string for two calls", id, device.getIDstring());
            assertEquals("Unexpected value for id string", expected, id);
        }

        /**
         * Test getScreenAspectRatio.
         * <ul>
         * <li>test that an aspect ratio is always returned
         * </ul>
         */
        public void testScreenAspectRatio() throws Exception
        {
            // Should reflect pixel resolution of current config
            // Try each config in turn and verify
            HScreenConfiguration[] configs = getConfigs(device);
            for (int i = 0; i < configs.length; ++i)
            {
                device.reserveDevice(new TestResourceClient());
                try
                {
                    assertTrue("Should've been able to reserve the device - " + i, setConfig(device, configs[i]));
                }
                finally
                {
                    device.releaseDevice();
                }

                Dimension sar = device.getScreenAspectRatio();
                assertNotNull("Expected non-null screen aspect ratio - " + i, sar);
                assertEquals("Expected same screen aspect ratio for multiple calls - " + i, sar,
                        device.getScreenAspectRatio());

                Dimension par = configs[i].getPixelAspectRatio();
                HScreenRectangle norm = configs[i].getScreenArea();
                Dimension user = configs[i].getPixelResolution();

                /*
                 * Skip non-contributing configurations (e.g., a non-contrib
                 * video configuration that is used with a still image
                 * background config.
                 */
                if (par.width == 0 && par.height == 0 && user.width == 0 && user.height == 0 && norm.width == 0.0F
                        && norm.height == 0.0F)
                {
                    if (DEBUG) System.out.println("testSAR: Skipping non-contrib config: " + getName() + " - " + i);
                    continue;
                }

                /*
                 * Deal with weirdities with 16:9 480[ip] configurations.
                 * Problem is that in order to have a 16:9 480[ip] configuration
                 * we need an odd width. 480*16/9 = 853.33333333 That 1/3 pixel
                 * gets rounded off. But we want to consider it for SAR/PAR!
                 */
                if (user.width == 853 && user.height == 480)
                {
                    assertEquals("Unexpected screen aspect ratio - " + i, new Dimension(16, 9), sar);
                    continue;
                }

                /* Validate SAR against PAR... */
                /* First try this method: which might fail due to rounding. */
                try
                {
                    /*
                     * Figure screen aspect ratio (SAR) in terms of: PAR = pixel
                     * aspect ratio Wn/Hn = normalized dimensions Wu/Hu = user
                     * dimensions (pixel resolution)
                     * 
                     * PAR = SAR * (Wn/Hn) * (Hu/Wu)
                     * 
                     * SAR = PAR * (Wu/Hu) * (Hn/Wn)
                     */
                    int sarx = (int) (par.width * user.width * norm.height);
                    int sary = (int) (par.height * user.height * norm.width);
                    int gcd = gcd(sarx, sary);

                    if (DEBUG)
                    {
                        System.out.println("SAR: " + getName() + " - " + i);
                        System.out.println(" sarx = " + sarx + " = " + par.width + " * " + user.width + " * "
                                + norm.height);
                        System.out.println(" sary = " + sary + " = " + par.height + " * " + user.height + " * "
                                + norm.width);
                        System.out.println(" gcd  = " + gcd);
                        System.out.println(" SAR  = " + sar);
                        System.out.println("      = " + (new Dimension(sarx / gcd, sary / gcd)));
                    }

                    assertEquals("Unexpected screen aspect ratio - " + i, new Dimension(sarx / gcd, sary / gcd), sar);
                }
                /* Next fallback to this method. */
                catch (AssertionFailedError e)
                {
                    /*
                     * Ensure that our SAR can give us the correct PAR: PAR =
                     * pixel aspect ratio Wn/Hn = normalized dimensions Wu/Hu =
                     * user dimensions (pixel resolution)
                     * 
                     * PAR = SAR * (Wn/Hn) * (Hu/Wu)
                     */
                    int parx = (int) (sar.width * user.height * norm.width);
                    int pary = (int) (sar.height * user.width * norm.height);
                    int gcd = gcd(parx, pary);

                    if (DEBUG)
                    {
                        System.out.println("PAR: " + getName() + " - " + i);
                        System.out.println(" parx = " + parx + " = " + sar.width + " * " + user.height + " * "
                                + norm.width);
                        System.out.println(" pary = " + pary + " = " + sar.height + " * " + user.width + " * "
                                + norm.height);
                        System.out.println(" gcd  = " + gcd);
                        System.out.println(" PAR  = " + par);
                        System.out.println("      = " + (new Dimension(parx / gcd, pary / gcd)));
                    }

                    assertEquals("Unexpected pixel aspect ratio from screen aspect ratio - " + i, new Dimension(parx
                            / gcd, pary / gcd), par);
                }
            }
        }

        private static int gcd(int m, int n)
        {
            int a = Math.max(n, m);
            int b = Math.min(n, m);
            int r = 1;
            while (r > 0)
            {
                r = a % b;
                a = b;
                b = r;
            }
            return a;
        }

        private static Dimension ratio(Dimension d)
        {
            int gcd = gcd(d.width, d.height);

            return new Dimension(d.width / gcd, d.height / gcd);
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(InstanceTest.class);
            suite.setName(HScreenDevice.class.getName());
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

        protected Element xdevice;

        protected HScreenDevice device;

        protected XMLDispConfig test;

        protected int[] allPrefs;

        protected void setUp() throws Exception
        {
            super.setUp();

            TestParam param = (TestParam) createImplObject();
            xdevice = param.xdevice;
            device = param.device;
            test = param.test;
            allPrefs = param.allPrefs;

            assertNotNull(xdevice);
            assertNotNull(device);
            assertNotNull(test);
            assertNotNull(allPrefs);
        }

        protected void tearDown() throws Exception
        {
            xdevice = null;
            device = null;
            test = null;
            allPrefs = null;

            super.tearDown();
        }

        /**
         * Type of object returned by ImplFactory.createImplObject().
         */
        public static class TestParam implements ImplFactory
        {
            public HScreenDevice device;

            public Element xdevice;

            public XMLDispConfig test;

            public int[] allPrefs;

            public TestParam(HScreenDevice device, Element xdevice, XMLDispConfig test, int[] allPrefs)
            {
                this.device = device;
                this.xdevice = xdevice;
                this.test = test;
                this.allPrefs = allPrefs;
            }

            public Object createImplObject()
            {
                return this;
            }

            public String toString()
            {
                String id = (xdevice != null) ? xdevice.getAttribute("id") : null;
                return (id == null) ? super.toString() : id;
            }
        }
    }

    /**
     * Test
     * addResourceStatusEventListener()/removeResourceStatusEventListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public void testResourceStatusEventListener() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        try
        {
            // install resource status listener
            device.addResourceStatusEventListener((ResourceStatusListener) rc);

            // Reserve device, which should succeed
            synchronized (rc)
            {
                // And result in resource status event
                assertTrue("Unable to reserve graphics device", device.reserveDevice(rc));
                rc.wait(5000);
                assertTrue("Listener was not fired for reservation event", rc.statusChangedFired);
            }

            // Release device, which should succeed
            // And result in resource status event
            rc.reset();
            synchronized (rc)
            {
                device.releaseDevice();
                rc.wait(5000);
                assertTrue("Listener was not fired for release event", rc.statusChangedFired);
            }

            // remove resource status listener
            device.removeResourceStatusEventListener(rc);

            // Listener shouldn't be called
            rc.reset();
            synchronized (rc)
            {
                assertTrue("Unable to reserve graphics device", device.reserveDevice(rc));
                rc.wait(500);
                assertFalse("Removed listener was called for reservation event", rc.statusChangedFired);
            }

            // listener shouldn't be called
            rc.reset();
            synchronized (rc)
            {
                device.releaseDevice();
                rc.wait(500);
                Thread.sleep(50);
                assertFalse("Removed listener was called for release event", rc.statusChangedFired);
            }
        }
        finally
        {
            // release device
            device.releaseDevice();
            // remove resource status listener
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Test
     * addScreenConfigurationListener()/removeScreenConfigurationListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public void testScreenConfigurationListener() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        try
        {
            // install screenConfiguration listener
            device.addScreenConfigurationListener((HScreenConfigurationListener) rc);

            // reserve graphics device
            assertTrue("Unable to reserve screen device", device.reserveDevice(rc));

            // change configuration
            // should get listener fired
            synchronized (rc)
            {
                changeConfiguration(device);
                rc.wait(5000);
                assertEquals("Expected listener to be fired if configuration changed",
                        getConfigurationCount(device) > 1, rc.reportFired);
            }

            // remove resource status listener
            // and retry
            // Listener should not be called
            device.removeScreenConfigurationListener((HScreenConfigurationListener) rc);
            rc.reset();
            synchronized (rc)
            {
                changeConfiguration(device);
                rc.wait(500);
                assertFalse("Removed listener was called for screen configuration event", rc.reportFired);
            }
        }
        finally
        {
            // release device
            device.releaseDevice();
            // remove resource status listener
            device.removeScreenConfigurationListener((HScreenConfigurationListener) rc);
        }
    }

    /**
     * Test getClient.
     * <ul>
     * <li>
     * </ul>
     */
    public void testClient()
    {
        ResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        try
        {
            // reserve device
            assertTrue("Unable to reserve graphics device", device.reserveDevice(rc));
            assertSame("getClient did not return client that holds reservation", rc, device.getClient());

            // reserve with another Client
            rc = new TestResourceClient();
            assertTrue("Unable to reserve graphics device", device.reserveDevice(rc));
            assertSame("getClient did not return client that holds reservation", rc, device.getClient());

            // release device
            device.releaseDevice();
            assertNull("getClient should return null if device isn't reserved", device.getClient());
        }
        finally
        {
            // release device
            device.releaseDevice();
        }
    }

    /**
     * Test releaseDevice.
     * <ul>
     * <li>
     * </ul>
     */
    public void testReleaseDevice() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        try
        {
            // install resource status listener
            device.addResourceStatusEventListener((ResourceStatusListener) rc);

            // reserve graphics device
            boolean reserved = device.reserveDevice(rc);
            assertTrue("Unable to reserve device", reserved);

            // release device
            rc.reset();
            synchronized (rc)
            {
                device.releaseDevice();
                rc.wait(5000);
                // make sure listener was fired
                assertTrue("Failed to release device", rc.statusChangedFired);
            }
            device.removeResourceStatusEventListener((ResourceStatusListener) rc);

            // release device - should have no effect
            rc.reset();
            device.releaseDevice();
            synchronized (rc)
            {
                rc.wait(500);
                assertFalse("No event should be generated for un-reserved device", rc.statusChangedFired);
            }
        }
        finally
        {
            device.releaseDevice();
            device.removeResourceStatusEventListener((ResourceStatusListener) rc);
        }
    }

    /**
     * Test reserveDevice being required to change the configuration.
     */
    public void testReserveDevice_required() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        try
        {
            assertTrue("Unable to reserve device", device.reserveDevice(rc));
            // Change the configuration should go through
            changeConfiguration(device);

            device.releaseDevice();
            try
            {
                changeConfiguration(device);
                fail("Expected HPermissionDeniedException");
            }
            catch (HPermissionDeniedException e)
            {
            }
        }
        finally
        {
            device.releaseDevice();
        }
    }

    private String getProxyType(HScreenDevice device)
    {
        if (device instanceof HGraphicsDevice)
            return HGraphicsDevice.class.getName();
        else if (device instanceof HVideoDevice)
            return HVideoDevice.class.getName();
        else if (device instanceof HBackgroundDevice)
            return HBackgroundDevice.class.getName();
        else
            fail("Unexpected device type");
        return null;
    }

    private void checkClient(String context, Client client, ResourceClient rc, ResourceProxy proxy)
    {
        assertNotNull("Expected a client to be passed to " + context, client);
        assertSame("Expected client to specify given client", rc, client.client);
        assertSame("Expected client to specify given proxy", proxy, client.proxy);
        assertNotNull("Expected client to specify a CallerContext", client.context);
    }

    /**
     * Tests reserveDevice(); makes sure that isReservationAllowed is called.
     */
    public void testReserveDevice_isAllowed() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        device.addResourceStatusEventListener(rc);
        RezMgr rezmgr = replaceRezMgr();
        try
        {

            for (int truefalse = 0; truefalse < 2; ++truefalse)
            {
                try
                {
                    boolean allowed = truefalse != 0;
                    rezmgr.reset(allowed, 0);

                    synchronized (rc)
                    {
                        // Reserve device
                        assertEquals("Expected reservation to be allowed/disallowed", allowed, device.reserveDevice(rc));
                        rc.wait(allowed ? 5000 : 500);

                        // Check results
                        assertTrue("Expected RezMgr.isReservationAllowed() to be called", rezmgr.allowedCalled);
                        checkClient("isReservationAllowed", rezmgr.allowedClient, rc, device);
                        assertEquals("Expected proxyType to be passed to isReservationAllowed", getProxyType(device),
                                rezmgr.allowedProxy);

                        // Was change listener called or not?
                        assertEquals("Expected status listener to be called if reservation allowed", allowed,
                                rc.statusChangedFired);
                    }
                }
                finally
                {
                    // Release before looping
                    device.releaseDevice();
                    Thread.sleep(50);
                }
            }
        }
        finally
        {
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Tests reserveDevice() given no current owner.
     */
    public void testReserveDevice_noOwner() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        device.addResourceStatusEventListener(rc);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            // nobody owns it
            assertNull("Did not expect somebody to own the device", device.getClient());
            synchronized (rc)
            {
                assertTrue("Unable to reserve unowned screen device", device.reserveDevice(rc));
                rc.wait(5000);
                assertTrue("Expected status listener to be called", rc.statusChangedFired);
            }

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            device.releaseDevice();
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Tests reserveDevice() given same owner.
     */
    public void testReserveDevice_alreadyOwned() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        RezMgr rezmgr = replaceRezMgr();
        try
        {
            // acquire it for ourself
            device.addResourceStatusEventListener(rc);
            synchronized (rc)
            {
                assertTrue("Unable to reserve unowned screen device", device.reserveDevice(rc));
                rc.wait(5000);
                assertTrue("Expected status listener to be invoked upon reservation", rc.statusChangedFired);
            }

            // We already own it
            synchronized (rc)
            {
                rc.reset();
                assertTrue("Unable to re-reserve device", device.reserveDevice(rc));
                rc.wait(5000);
                assertFalse("Expected no status listener to be called - since we own it!", rc.statusChangedFired);
            }

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            device.releaseDevice();
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Tests reserveDevice() given requestRelease=true.
     */
    public void testReserveDevice_requestRelease() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        device.addResourceStatusEventListener(rc);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            // acquire it for rc
            synchronized (rc)
            {
                assertTrue("Unable to reserve unowned screen device", device.reserveDevice(rc));
                rc.wait(5000);
                assertTrue("Expected status listener to be called", rc.statusChangedFired);
            }

            // Reservation by another client
            rc.reset();
            synchronized (rc)
            {
                assertTrue("Unable to reserve for another client", device.reserveDevice(rc2));
                rc.wait(5000);
                assertTrue("Expected status listener to be called", rc.statusChangedFired);
            }
            assertTrue("Expected requestRelease to have been called", rc.requestReleaseCalled);
            assertFalse("Did not expect notifyRelease to have been called", rc.notifyReleaseCalled);

            // Should not get so far as calling prioritizeContention
            assertFalse("Should not have contention", rezmgr.prioritizeCalled);
        }
        finally
        {
            device.releaseDevice();
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Tests reserveDevice() contention prioritization.
     */
    public void testReserveDevice_contention() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        device.addResourceStatusEventListener(rc);
        RezMgr rezmgr = replaceRezMgr();
        try
        {
            for (int i = 0; i <= RezMgr.PRIOR_MAX; ++i)
            {
                rezmgr.reset(true, i);

                // nobody owns it
                synchronized (rc)
                {
                    assertTrue("Unable to reserve unowned screen device", device.reserveDevice(rc));
                    rc.wait(5000);
                    assertTrue("Expected status listener to be called", rc.statusChangedFired);
                }

                // Contention
                rc2.reset();
                rc.reset(false);
                boolean reserved;
                synchronized (rc)
                {
                    reserved = device.reserveDevice(rc2);
                    rc.wait(5000);
                    assertTrue("Expected requestRelease to be called", rc.requestReleaseCalled);
                }
                assertFalse("Did not expect requestRelease to be called on requester", rc2.requestReleaseCalled);
                assertTrue("Expected contention - " + i, rezmgr.prioritizeCalled);

                // prioritizeContention params
                checkClient("prioritizeContention(req) - " + i, rezmgr.prioritizeReq, rc2, device);
                assertNotNull("Expected owners to be passed to prioritizeContention", rezmgr.prioritizeOwn);
                assertEquals("Expected 1 owner to be passed to prioritizeContention", 1, rezmgr.prioritizeOwn.length);
                checkClient("prioritizeContention(owner) - " + i, rezmgr.prioritizeOwn[0], rc, device);

                // Callbacks made?
                boolean release = rezmgr.prioritizeOkay || !rezmgr.prioritizeHasClient;
                assertFalse("release() should not be called on requester", rc2.releaseCalled);
                assertFalse("notifyRelease() should not be called on requester", rc2.notifyReleaseCalled);
                assertEquals("Expected release to have been called - " + i, release, rc.releaseCalled);
                assertEquals("Expected notifyRelease to have been called - " + i, release, rc.notifyReleaseCalled);
                assertEquals("Expected statusChanged to have been called - " + i, release, rc.statusChangedFired);

                // getClient
                if (!rezmgr.prioritizeHasClient)
                    assertTrue("getClient() should be null - " + i, device.getClient() == null);
                else
                    assertSame("Expected client to be set - " + i, rezmgr.prioritizeOkay ? rc2 : rc, device.getClient());

                // reserved?
                assertEquals("Expected contention to be resolved - " + i, rezmgr.prioritizeOkay, reserved);

                device.releaseDevice();
                Thread.sleep(50);
            }
        }
        finally
        {
            device.releaseDevice();
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Tests reserveDevice() and checks the requested type of ResourceUsage
     * passed to createResourceUsage.
     */
    public void testReserveDevice_resourceUsageType() throws Exception
    {
        TestResourceClient rc = new TestResourceClient();
        TestResourceClient rc2 = new TestResourceClient();
        HScreenDevice device = getScreenDevice();
        if (device == null) return;

        device.addResourceStatusEventListener(rc);
        RezMgr rezmgr = replaceRezMgr();
        rezmgr.reset(true, 4);
        try
        {
            // nobody owns it
            assertNull("Did not expect somebody to own the device", device.getClient());
            synchronized (rc)
            {
                assertTrue("Unable to reserve unowned screen device", device.reserveDevice(rc));
                rc.wait(5000);
                assertTrue("Expected status listener to be called", rc.statusChangedFired);
            }

            // Contention
            rc2.reset();
            rc.reset(false);
            synchronized (rc)
            {
                device.reserveDevice(rc2);
                rc.wait(5000);
                assertTrue("Expected requestRelease to be called", rc.requestReleaseCalled);
            }
            Thread.sleep(50);

            // type of the requested ResourceUsage passed to the resource
            // manager should
            // be ApplicationResourceUsage
            for (int i = 0; i < rezmgr.prioritizeOwn.length; i++)
            {
                assertTrue("Expected ResourceUsage type" + i + " to be ApplicationResourceUsage",
                        rezmgr.prioritizeOwn[i].resusage instanceof ApplicationResourceUsage);
            }
        }
        finally
        {
            device.releaseDevice();
            restoreRezMgr();
            device.removeResourceStatusEventListener(rc);
        }
    }

    /**
     * Retrieves a template describing the given configuration.
     */
    static HScreenConfigTemplate getTemplate(HScreenConfiguration config)
    {
        HScreenConfigTemplate t = null;
        if (config instanceof HGraphicsConfiguration)
            t = ((HGraphicsConfiguration) config).getConfigTemplate();
        else if (config instanceof HBackgroundConfiguration)
            t = ((HBackgroundConfiguration) config).getConfigTemplate();
        else if (config instanceof HVideoConfiguration)
            t = ((HVideoConfiguration) config).getConfigTemplate();
        else
            fail("Unknown configuration type " + config.getClass());
        return t;
    }

    /**
     * Returns the screen device given a screen configuration.
     */
    static HScreenDevice getDevice(HScreenConfiguration c)
    {
        HScreenDevice d = null;
        if (c instanceof HGraphicsConfiguration)
            d = ((HGraphicsConfiguration) c).getDevice();
        else if (c instanceof HVideoConfiguration)
            d = ((HVideoConfiguration) c).getDevice();
        else if (c instanceof HBackgroundConfiguration)
            d = ((HBackgroundConfiguration) c).getDevice();
        else
            fail("Unknown screen configuration type " + c.getClass());
        return d;
    }

    /**
     * Returns the current configuration given a screen device.
     */
    static HScreenConfiguration getCurrConfig(HScreenDevice d)
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).getCurrentConfiguration();
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).getCurrentConfiguration();
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).getCurrentConfiguration();
        else
            fail("Unknown screen configuration type " + d.getClass());
        return null;
    }

    /**
     * Returns the default configuration given a screen device.
     */
    static HScreenConfiguration getDefConfig(HScreenDevice d)
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).getDefaultConfiguration();
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).getDefaultConfiguration();
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).getDefaultConfiguration();
        else
            fail("Unknown screen configuration type " + d.getClass());
        return null;
    }

    /**
     * Calls <code>getBestConfiguration()</code> on the given device, passing
     * the given templates as arguments.
     */
    static HScreenConfiguration getBestConfig(HScreenDevice d, HScreenConfigTemplate[] t)
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).getBestConfiguration((HGraphicsConfigTemplate[]) t);
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).getBestConfiguration((HVideoConfigTemplate[]) t);
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).getBestConfiguration((HBackgroundConfigTemplate[]) t);
        else
            fail("Unknown screen configuration type " + d.getClass());
        return null;
    }

    /**
     * Calls <code>getBestConfiguration()</code> on the given device, passing
     * the given template as argument.
     */
    static HScreenConfiguration getBestConfig(HScreenDevice d, HScreenConfigTemplate t)
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).getBestConfiguration((HGraphicsConfigTemplate) t);
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).getBestConfiguration((HVideoConfigTemplate) t);
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).getBestConfiguration((HBackgroundConfigTemplate) t);
        else
            fail("Unknown screen configuration type " + d.getClass());
        return null;
    }

    /**
     * Returns the configurations supported for the given screen device.
     */
    static HScreenConfiguration[] getConfigs(HScreenDevice d)
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).getConfigurations();
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).getConfigurations();
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).getConfigurations();
        else
            fail("Unknown screen configuration type");
        return null;
    }

    /**
     * Sets the current configuration for the given device to the given
     * configuration.
     */
    static boolean setConfig(HScreenDevice d, HScreenConfiguration c) throws HConfigurationException,
            HPermissionDeniedException, SecurityException
    {
        if (d instanceof HGraphicsDevice)
            return ((HGraphicsDevice) d).setGraphicsConfiguration((HGraphicsConfiguration) c);
        else if (d instanceof HBackgroundDevice)
            return ((HBackgroundDevice) d).setBackgroundConfiguration((HBackgroundConfiguration) c);
        else if (d instanceof HVideoDevice)
            return ((HVideoDevice) d).setVideoConfiguration((HVideoConfiguration) c);
        else
            fail("Unknown device type");
        return false;
    }

    /**
     * Gets the HScreen for the given device.
     */
    static HScreen getHScreen(HScreenDevice device)
    {
        HScreen[] screens = HScreen.getHScreens();
        for (int i = 0; i < screens.length; ++i)
        {
            HScreenDevice[] devices = null;

            if (device instanceof HGraphicsDevice)
            {
                devices = screens[i].getHGraphicsDevices();
            }
            else if (device instanceof HBackgroundDevice)
            {
                devices = screens[i].getHBackgroundDevices();
            }
            else if (device instanceof HVideoDevice)
            {
                devices = screens[i].getHVideoDevices();
            }
            if (devices == null) continue;

            for (int j = 0; j < devices.length; ++j)
                if (device == devices[j]) return screens[i];
        }
        return null;
    }

    /**
     * Returns whether the two objects are equal. Main purpose is to compare
     * HScreenRectangle objects.
     */
    static boolean areEquals(Object obj1, Object obj2)
    {
        if ((obj1 instanceof HScreenRectangle) && (obj2 instanceof HScreenRectangle))
        {
            HScreenRectangle rect1 = (HScreenRectangle) obj1;
            HScreenRectangle rect2 = (HScreenRectangle) obj2;

            return rect1.x == rect2.x && rect1.y == rect2.y && rect1.width == rect2.width
                    && rect1.height == rect2.height;
        }
        else
            return obj1.equals(obj2);
    }

    /**
     * Count the number of templates that match the given preference/priority/
     * object combination.
     */
    protected static int countTemplates(int pref, int prior, Object obj, HScreenConfigTemplate[] templates)
    {
        int count = 0;
        for (int i = 0; i < templates.length; ++i)
        {
            if (prior == templates[i].getPreferencePriority(pref)
                    && (obj == null || areEquals(obj, templates[i].getPreferenceObject(pref))))
            {
                ++count;
            }
        }
        return count;
    }

    /**
     * Create an array of templates describing the given array of
     * configurations.
     * 
     * @see #findMinimalTemplates
     */
    static HScreenConfigTemplate[] getTemplates(HScreenConfiguration configs[])
    {
        HScreenConfigTemplate[] templates = createTemplateArray(configs[0], configs.length);
        for (int i = 0; i < configs.length; ++i)
            templates[i] = getTemplate(configs[i]);
        return templates;
    }

    /**
     * Creates a config template array of a type that matches the given screen
     * configuration type.
     */
    static HScreenConfigTemplate[] createTemplateArray(HScreenConfiguration c, int n)
    {
        if (c instanceof HGraphicsConfiguration)
            return new HGraphicsConfigTemplate[n];
        else if (c instanceof HBackgroundConfiguration)
            return new HBackgroundConfigTemplate[n];
        else if (c instanceof HVideoConfiguration)
            return new HVideoConfigTemplate[n];
        else
            fail("Unknown config type");
        return null;
    }

    /**
     * Create a default template of a type that matches the given configuration.
     * 
     * @see #findMinimalTemplates
     */
    protected static HScreenConfigTemplate createEmptyTemplate(HScreenConfigTemplate orig)
    {
        HScreenConfigTemplate t = null;
        if (orig instanceof HGraphicsConfigTemplate)
            t = new HGraphicsConfigTemplate();
        else if (orig instanceof HBackgroundConfigTemplate)
            t = new HBackgroundConfigTemplate();
        else if (orig instanceof HVideoConfigTemplate)
            t = new HVideoConfigTemplate();
        else
            fail("Unknown configuration type");
        return t;
    }

    /**
     * Creates a new template, similar to the original one. Only the preferences
     * listed in prefs are copied to the new one. If preferred is set, then
     * REQUIRED/REQUIRED_NOT priorities are changed to PREFERRED/PREFERRED_NOT.
     */
    protected static HScreenConfigTemplate createTemplate(HScreenConfigTemplate orig, int prefs[], boolean preferred)
    {
        HScreenConfigTemplate t = createEmptyTemplate(orig);

        for (int i = 0; i < prefs.length; ++i)
        {
            int pref = prefs[i];
            int prior = orig.getPreferencePriority(pref);
            if (prior == HScreenConfigTemplate.DONT_CARE) continue;
            Object obj = null;
            try
            {
                obj = orig.getPreferenceObject(pref);
            }
            catch (Exception e)
            {
            }

            // Modify priority if desired (REQUIRED->PREFERRED)
            if (preferred)
            {
                switch (prior)
                {
                    case HScreenConfigTemplate.REQUIRED:
                        prior = HScreenConfigTemplate.PREFERRED;
                        break;
                    case HScreenConfigTemplate.REQUIRED_NOT:
                        prior = HScreenConfigTemplate.PREFERRED_NOT;
                        break;
                    default:
                        continue; // skip...
                }
            }

            if (obj != null)
                t.setPreference(pref, obj, prior);
            else
                t.setPreference(pref, prior);
        }

        return t;
    }

    /**
     * Finds the set of minimal config templates necessary to describe the given
     * configuration using the given preferences.
     * 
     * @param config
     *            the configuration being searched for
     * @param configs
     *            all configurations
     * @param prefs
     *            all preferences
     */
    protected static Vector findTemplates(HScreenConfiguration config, HScreenConfiguration[] configs, int prefs[])
    {
        Vector found = new Vector();
        int combo[] = new int[prefs.length];
        int comboi = 0;
        HScreenConfigTemplate template = getTemplate(config);
        HScreenConfigTemplate[] templates = getTemplates(configs);

        // Add the exact match templates
        found.addElement(createTemplate(template, prefs, false));
        found.addElement(createTemplate(template, prefs, true));

        /*
         * Foreach preference... - If same value for all templates then ignore
         * it (DONT_CARE). - If doesn't match other templates, then create a new
         * minimal template. - If found on some, then it's part of a combo
         * preference
         */
        for (int i = 0; i < prefs.length; ++i)
        {
            int pref = prefs[i];
            int prior = template.getPreferencePriority(pref);

            if (HScreenConfigTemplate.DONT_CARE == prior) continue;

            Object obj = null;
            try
            {
                obj = template.getPreferenceObject(pref);
            }
            catch (Exception e)
            {
            }
            int n = countTemplates(pref, prior, obj, templates);

            if (n == 1)
            {
                /* Single preference is sufficient to find this config. */
                int[] single = { pref };
                found.addElement(createTemplate(template, single, false));
                found.addElement(createTemplate(template, single, true));
            }
            else if (n > 1 && n != templates.length)
            {
                /* Part of combination of preferences. */
                combo[comboi++] = pref;
            }
            else if (n > 0)
            {
                // Common preference, skip it
            }
            else
            // if n == 0
            {
                fail("Should never happen! Should match target template!");
            }
        }

        // Go back and generate combo template
        // If combo doesn't adequately describe config, then there's something
        // wrong!
        // But, definitely need more than one combo preference!!!
        if (comboi > 1)
        {
            int shrink[] = new int[comboi];
            System.arraycopy(combo, 0, shrink, 0, comboi);
            found.addElement(createTemplate(template, shrink, false));
            found.addElement(createTemplate(template, shrink, true));
        }

        return found;
    }

    /**
     * Creates a set of templates that <i>should not</i> be compatible with any
     * of the given configurations. Creates templates with single non-object
     * preferences that don't match the given configurations.
     * <p>
     * Should be extended to create templates that consist of object preferences
     * as well a combination of preferences.
     * 
     * @param configs
     *            all configurations
     * @param prefs
     *            all preferences
     * @return a <code>Vector</code> containing the templates
     */
    protected static Vector findBadTemplates(HScreenConfiguration configs[], int prefs[])
    {
        Vector bad = new Vector();
        int priors[] = { HScreenConfigTemplate.REQUIRED, HScreenConfigTemplate.REQUIRED_NOT };
        HScreenConfigTemplate[] templates = getTemplates(configs);

        PREFS: for (int i = 0; i < prefs.length; ++i)
        {
            for (int j = 0; j < priors.length; ++j)
            {
                int pref = prefs[i];
                int prior = priors[j];

                for (int h = 0; h < configs.length; ++h)
                {
                    HScreenConfigTemplate template = templates[h];

                    if (HScreenConfigTemplate.DONT_CARE == template.getPreferencePriority(pref)) continue;

                    Object obj = null;
                    try
                    {
                        obj = template.getPreferenceObject(pref);
                    }
                    catch (Exception e)
                    {
                    }

                    // For now, skip object preferences
                    if (obj != null) continue PREFS;

                    int n = countTemplates(pref, prior, null, templates);

                    if (n == 0)
                    {
                        // Here is one that won't match
                        HScreenConfigTemplate t = createEmptyTemplate(template);
                        if (obj != null)
                        {
                            // REQUIRED_NOT w/ obj... will likely get a
                            // config!!!
                            // Want REQUIRED with unfound obj...
                            t.setPreference(pref, obj, prior);
                        }
                        else
                            t.setPreference(pref, prior);

                        bad.addElement(t);
                    }
                    // if n > 1 then might be useful in combination...
                    // if n == 1, useless to us
                }
            }
        }

        // Could find min/max object prefs and use ones beyond range...

        return bad;
    }

    private ResourceManager save;

    private RezMgr replaceRezMgr()
    {
        RezMgr rezmgr = new RezMgr();
        save = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        ManagerManagerTest.updateManager(ResourceManager.class, RezMgr.class, false, rezmgr);
        return rezmgr;
    }

    private void restoreRezMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(ResourceManager.class, save.getClass(), true, save);
        save = null;
    }

    /**
     * Returns the screen device to be tested. Should be overridden by subclass
     * tests.
     */
    protected HScreenDevice getScreenDevice()
    {
        return HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();
    }

    /**
     * Returns the number of configurations supported (for this test). The
     * default HScreenDeviceTest only tests the default graphics config. Should
     * be overridden by subclass tests.
     */
    protected int getConfigurationCount(HScreenDevice device)
    {
        HGraphicsConfiguration[] configs = ((HGraphicsDevice) device).getConfigurations();
        return configs.length;
    }

    /**
     * Changes the current configuration. Should be overridden by subclass
     * tests.
     * 
     * @return <code>true</code> if the configuration was modified
     */
    protected boolean changeConfiguration(HScreenDevice device) throws Exception
    {
        // Let's cycle through the configurations
        HScreenConfiguration curr = getCurrConfig(device);
        HScreenConfiguration[] configs = getConfigs(device);

        // Find which-ever one we are...
        int i;
        for (i = 0; i < configs.length; ++i)
            if (configs[i].equals(curr)) break;
        assertFalse("Current configuration not found", i >= configs.length);

        // Select the next configuration, wrapping if necessary
        i = (i + 1) % configs.length;
        return setConfig(device, configs[i]);
    }

    /**
     * ResourceClient, ResourceStatusListener, and HScreenConfigurationListener
     * for testing.
     */
    static class TestResourceClient implements ResourceClient, ResourceStatusListener, HScreenConfigurationListener
    {
        public boolean notifyReleaseCalled = false;

        public boolean statusChangedFired = false;

        public boolean releaseCalled = false;

        public boolean reportFired = false;

        public boolean requestReleaseCalled = false;

        public boolean REQUEST = true;

        public void reset(boolean REQUEST)
        {
            notifyReleaseCalled = false;
            statusChangedFired = false;
            releaseCalled = false;
            reportFired = false;
            requestReleaseCalled = false;
            this.REQUEST = REQUEST;
        }

        public void reset()
        {
            reset(REQUEST);
        }

        public TestResourceClient()
        {
        }

        /**
         * A call to this operation notifies the ResourceClient that proxy has
         * lost access to a resource. This can happen for two reasons: either
         * the resource is unavailable for some reason beyond the control of the
         * environment (e.g. hardware failure) or because the client has been
         * too long in dealing with a ResourceClient.release() call.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         */
        public void notifyRelease(ResourceProxy p)
        {
            notifyReleaseCalled = true;
        }

        /**
         * A call to this operation informs the ResourceClient that proxy is
         * about to lose access to a resource. The ResourceClient shall complete
         * any clean-up that is needed before the resource is lost before it
         * returns from this operation. This operation is not guaranteed to be
         * allowed to complete before notifyRelease() is called.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         */
        public void release(ResourceProxy p)
        {
            releaseCalled = true;
        }

        /**
         * A call to this operation informs the ResourceClient that another
         * application has requested the resource accessed via the proxy
         * parameter. If the ResourceClient decides to give up the resource as a
         * result of this, it should terminate its usage of proxy and return
         * True, otherwise False. requestData may be used to pass more data to
         * the ResourceClient so that it can decide whether or not to give up
         * the resource, using semantics specified outside this framework; for
         * conformance to this framework, requestData can be ignored by the
         * ResourceClient.
         * 
         * @param proxy
         *            - the ResourceProxy representing the scarce resource to
         *            the application
         * @param requestData
         *            - application specific data
         * @return boolean If the ResourceClient decides to give up the resource
         *         following this call, it should terminate its usage of proxy
         *         and return True, otherwise False.
         */
        public boolean requestRelease(ResourceProxy p, Object requestData)
        {
            requestReleaseCalled = true;
            return REQUEST;
        }

        public synchronized void report(HScreenConfigurationEvent e)
        {
            reportFired = true;
            notifyAll();
        }

        public synchronized void statusChanged(ResourceStatusEvent e)
        {
            statusChangedFired = true;
            notifyAll();
        }
    } // class TestResourceClient

    /**
     * Replacement ResourceManager for catching calls to the ResourceManager.
     */
    static class RezMgr implements ResourceManager
    {
        public boolean ALLOWED = true;

        public int PRIOR = 0;

        // isReservationAllowed
        public boolean allowedCalled = false;

        public Client allowedClient = null;

        public String allowedProxy = null;

        // prioritizeContention
        public boolean prioritizeCalled = false;

        public Client prioritizeReq = null;

        public Client[] prioritizeOwn = null;

        public boolean prioritizeOkay;

        public boolean prioritizeHasClient;

        public Class resourceUsageType = null;

        public void reset(boolean ALLOWED, int PRIOR)
        {
            this.ALLOWED = ALLOWED;
            this.PRIOR = PRIOR;
            allowedCalled = false;
            allowedClient = null;
            allowedProxy = null;
            prioritizeCalled = false;
            prioritizeReq = null;
            prioritizeOwn = null;
            resourceUsageType = null;
        }

        public boolean isReservationAllowed(Client client, String proxyType)
        {
            allowedCalled = true;
            allowedClient = client;
            allowedProxy = proxyType;
            return ALLOWED;
        }

        public static final int PRIOR_MAX = 4;

        public Client[] prioritizeContention(Client requester, Client[] owners)
        {
            prioritizeCalled = true;
            prioritizeReq = requester;
            prioritizeOwn = owners;
            switch (PRIOR)
            {
                case 0:
                    prioritizeOkay = false;
                    prioritizeHasClient = false;
                    return new Client[0];
                case 1:
                    prioritizeOkay = true;
                    prioritizeHasClient = true;
                    return new Client[] { requester };
                case 2:
                    prioritizeOkay = false;
                    prioritizeHasClient = true;
                    return owners;
                case 3:
                    prioritizeOkay = true;
                    prioritizeHasClient = true;
                    return new Client[] { requester, owners[0] };
                case 4:
                    prioritizeOkay = false;
                    prioritizeHasClient = true;
                    return new Client[] { owners[0], requester };
                default:
                    fail("Unexpected PRIOR setting");
                    return null;
            }
        }

        public ResourceUsage[] prioritizeContention2(ResourceUsageImpl requester, ResourceUsageImpl[] owners)
        {
            // does nothing
            return null;
        }

        public void destroy()
        {
            // does nothing
        }

        public ResourceContentionManager getContentionManager()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public Client negotiateRelease(Client owners[], Object data)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        // ITSB INTEGRATION CHANGE.
        // public ExtendedResourceUsage createResourceUsage(CallerContext
        // context)
        // {
        // return new ApplicationResourceContext(context);
        // }
        // public ExtendedResourceUsage createResourceUsage(CallerContext
        // context, Class type, Object data)
        // {
        // resourceUsageType = type;
        // return createResourceUsage(context);
        // }
        public int getWarningPeriod()
        {
            // does nothing
            return 0;
        }

        public void registerWarningPeriodListener(NotifySetWarningPeriod nsp)
        {
            // does nothing
        }

        public void deliverContentionWarningMessage(ResourceUsage requestedResourceUsage,
                ResourceUsage[] currentReservations)
        {
        }

        public boolean isContentionHandlerValid()
        {
            return true;
        }

    }

    // Boilerplate

    protected void tearDown() throws Exception
    {
        // Make sure it's no longer reserved
        HScreenDevice dev = getScreenDevice();
        if (dev != null) dev.releaseDevice();
        Thread.sleep(50);
        restoreRezMgr();

        super.tearDown();
    }

    /**
     * Used to implement suite() for subclass tests.
     */
    protected static Test suite(Class testClass, int[] allPrefs, String type) throws Exception
    {
        return suite(InstanceTest.isuite(), testClass, allPrefs, type);
    }

    /**
     * Used to implement suite() for subclass tests.
     */
    protected static Test suite(InterfaceTestSuite isuite, Class testClass, int[] allPrefs, String type)
            throws Exception
    {
        TestSuite suite = new TestSuite(testClass);

        /* HScreenDeviceTest.InstanceTest foreach instance of HGraphicsDevice */
        HScreen screens[] = HScreen.getHScreens();
        if (screens != null)
        {
            XMLDispConfig test = new XMLDispConfig();
            Element xscreens[] = test.getScreens();
            // foreach screen
            boolean any = false;
            for (int i = 0; i < screens.length; ++i)
            {
                if (screens[i] != null)
                {
                    HScreenDevice[] devices = null;
                    if ("graphics".equals(type))
                        devices = screens[i].getHGraphicsDevices();
                    else if ("background".equals(type))
                        devices = screens[i].getHBackgroundDevices();
                    else if ("video".equals(type))
                        devices = screens[i].getHVideoDevices();
                    else
                        fail("Unknown type " + type);

                    // foreach device
                    for (int j = 0; j < devices.length; ++j)
                    {
                        String id = devices[j].getIDstring();
                        Element xdevice = test.findDevice(xscreens[i], id, type);
                        assertNotNull("Did not expect device of this name " + id, xdevice);
                        isuite.addFactory(new InstanceTest.TestParam(devices[j], xdevice, test, allPrefs));
                        any = true;
                    }
                }
            }
            if (any) suite.addTest(isuite);
        }

        return suite;
    }

    public static Test suite() throws Exception
    {
        return new TestSuite(HScreenDeviceTest.class);
    }

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
        System.exit(0);
    }

    public HScreenDeviceTest(String str)
    {
        super(str);
    }
}
