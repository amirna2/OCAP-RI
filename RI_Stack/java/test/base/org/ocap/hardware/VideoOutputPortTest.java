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

package org.ocap.hardware;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;
import java.util.Enumeration;
import junit.framework.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests VideoOutputPort.
 * 
 * @author Aaron Kamienski
 */
public class VideoOutputPortTest extends TestCase
{
    public static String[] capabilityStrings = { "CAPABILITY_TYPE_DTCP", "CAPABILITY_TYPE_HDCP",
            "CAPABILITY_TYPE_RESOLUTION_RESTRICTION", };

    /**
     * Interface test case. Used to test instances of VideoOutputPort.
     */
    public static class InstanceTest extends InterfaceTestCase
    {
        /**
         * Tests enable(). Since enabling might not be possible, just ensure
         * that enabling produces same results.
         */
        public void testEnable()
        {
            vop.enable();
            boolean enabled = vop.status();

            vop.disable();

            vop.enable();
            assertEquals("Re-enabling should have same effect as initial enabling", enabled, vop.status());

            doTestPermission(new XTest()
            {
                public void doX(VideoOutputPort vop)
                {
                    vop.enable();
                }
            });
        }

        /**
         * Tests disable(). Since disabling might not be possible, just ensure
         * that disabling produces same results.
         */
        public void testDisable()
        {
            vop.disable();
            boolean enabled = vop.status();

            vop.enable();

            vop.disable();
            assertEquals("Re-disabling should have same effect as initial disabling", enabled, vop.status());

            doTestPermission(new XTest()
            {
                public void doX(VideoOutputPort vop)
                {
                    vop.disable();
                }
            });
        }

        public void doTestPermission(XTest test)
        {
            DummySecurityManager sm = new DummySecurityManager();
            ProxySecurityManager.install();
            ProxySecurityManager.push(sm);

            try
            {
                // Clear current permission (only catch the first)
                sm.p = null;

                // Perform operation
                test.doX(vop);

                assertNotNull("SecurityManager.checkPermission should be called", sm.p);
                assertTrue("SecurityManager.checkPermission should be called with MonitorAppPermission",
                        sm.p instanceof MonitorAppPermission);
                assertEquals("Expected MonitorAppPermission(\"\")", "setVideoPort", sm.p.getName());
            }
            finally
            {
                ProxySecurityManager.pop();
            }
        }

        /**
         * Tests security checks for the following methods:
         * <ul>
         * <li>enable
         * <li>disable
         * </ul>
         */
        public void testEnableDisableSecurity()
        {
        }

        /**
         * Tests status().
         */
        public void testStatus()
        {
            boolean status = vop.status();
            assertEquals("Two calls should produce same results", status, vop.status());

            vop.enable();
            status = vop.status();
            assertEquals("Two calls (following enable) should produce same results", status, vop.status());

            vop.disable();
            status = vop.status();
            assertEquals("Two calls (following disable) should produce same results", status, vop.status());

        }

        /**
         * Tests queryCapability().
         */
        public void testQueryCapability()
        {
            int[] capabilities = { VideoOutputPort.CAPABILITY_TYPE_DTCP, VideoOutputPort.CAPABILITY_TYPE_HDCP,
                    VideoOutputPort.CAPABILITY_TYPE_RESOLUTION_RESTRICTION, };
            Class[] classes = { Boolean.class, Boolean.class, Integer.class };

            for (int i = 0; i < capabilities.length; ++i)
            {
                Object cap = vop.queryCapability(capabilities[i]);

                assertNotNull("Expected non-null returned for capability " + capabilityStrings[i], cap);
                assertTrue("Invalid type returned for capability " + capabilityStrings[i],
                        classes[i].isAssignableFrom(cap.getClass()));

                assertEquals("Expected same value returned for capability " + capabilityStrings[i], cap,
                        vop.queryCapability(capabilities[i]));
            }

            try
            {
                vop.queryCapability(10000);
                fail("Expected IllegalArgumentException for bad capability");
            }
            catch (IllegalArgumentException e)
            {
            }
        }

        /**
         * Tests getIEEE1394Mode().
         */
        public void testGetIEEE1394Node()
        {
            try
            {
                IEEE1394Node[] nodes = vop.getIEEE1394Node();

                assertNotNull("Expected non-null array of IEEE1394Nodes", nodes);
                for (int i = 0; i < nodes.length; ++i)
                {
                    assertNotNull("Expected non-null IEEE1394Nodes", nodes[i]);
                }

                doTestPermission(new XTest()
                {
                    public void doX(VideoOutputPort vop)
                    {
                        vop.getIEEE1394Node();
                    }
                });

            }
            catch (IllegalStateException e)
            {
                if (vop.getType() == VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394)
                {
                    fail("Unexpected IllegalStateException for 1394 port");
                }
            }

            /* Shouldn't get this far if non-1394 type... */
            if (vop.getType() != VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394)
            {
                fail("Expected IllegalStateException for non-1394 port");
            }
        }

        private byte[] addeq(byte[] op1, byte[] op2)
        {
            long op264 = 0, op164 = 0;
            for (int i = 0; i < op2.length; ++i)
                op264 = (op264 << 8) | (op2[i] & 0xFF);
            for (int i = 0; i < op1.length; ++i)
                op164 = (op164 << 8) | (op1[i] & 0xFF);

            long dest64 = op164 + op264;

            byte[] dest = new byte[8];
            for (int i = dest.length; i-- != 0; dest64 >>>= 8)
                dest[i] = (byte) (dest64 & 0xFF);

            return dest;
        }

        /**
         * Tests selectIEEE1394Sink().
         */
        public void testSelectIEEE1394Sink()
        {
            try
            {
                IEEE1394Node[] nodes = {};
                try
                {
                    nodes = vop.getIEEE1394Node();
                }
                catch (IllegalStateException e)
                {
                }

                byte[] badEUI64 = { 0, 0, 0, 0, 0, 0, 0, 1 };
                for (int i = 0; i < nodes.length; ++i)
                {
                    badEUI64 = addeq(badEUI64, nodes[i].getEUI64());
                    vop.selectIEEE1394Sink(nodes[i].getEUI64(), nodes[i].getSubunitType()[0]);
                }

                try
                {
                    vop.selectIEEE1394Sink(badEUI64, (short) 0);
                    fail("Expected IllegalArgumentException for bad EUI64");
                }
                catch (IllegalArgumentException e)
                {
                }

                doTestPermission(new XTest()
                {
                    public void doX(VideoOutputPort vop)
                    {
                        vop.selectIEEE1394Sink(null, (short) 0);
                    }
                });

            }
            catch (IllegalStateException e)
            {
                if (vop.getType() == VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394)
                {
                    fail("Unexpected IllegalStateException for 1394 port");
                }
            }

            /* Shouldn't get this far if non-1394 type... */
            if (vop.getType() != VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394)
            {
                fail("Expected IllegalStateException for non-1394 port");
            }
        }

        /**
         * Test getType().
         */
        public void testGetType()
        {
            int type = vop.getType();

            assertEquals("Expected same type to returned for two calls", type, vop.getType());

            switch (type)
            {
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF:
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB:
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO:
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394:
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI:
                case VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO:
                    break;
                default:
                    fail("Unknown type (" + type + ") returned");
            }
        }

        // TODO: remove set/getRFChannel()
        /**
         * Test get/setRFChannel
         */
        /*
         * public void testRFChannel() { // save the current state boolean
         * currentState = vop.getRFBypass(); // first disable RF bypass
         * vop.setRFBypass(false); // verify that RF bypass is off
         * assertFalse("Expected RF bypass to be disabled", vop.getRFBypass());
         * 
         * // get the current channel int channel = vop.getRFChannel();
         * 
         * // set the current channel and check the result int newChannel =
         * (channel == 3) ? 4 : 3; vop.setRFChannel(newChannel);
         * assertEquals("Assumed that the RF bypass channel changed",
         * vop.getRFChannel(), newChannel);
         * assertEquals("Assumed that the RF bypass channel stayed set",
         * vop.getRFChannel(), newChannel);
         * 
         * // turn on RF bypass vop.setRFBypass(true); // verify that RF bypass
         * is on assertTrue("Expected RF bypass to be enabled",
         * vop.getRFBypass());
         * 
         * // check that an exception is thrown when bypass is on try {
         * vop.setRFChannel(channel);
         * fail("Expected an IllegalStateException to be thrown"); } catch
         * (IllegalStateException e) { // test passed }
         * 
         * try { vop.getRFChannel();
         * fail("Expected an IllegalStateException to be thrown"); } catch
         * (IllegalStateException e) { // test passed } vop.setRFBypass(false);
         * assertFalse("Expected RF bypass to be disabled", vop.getRFBypass());
         * 
         * // verify that the channel number didn't change
         * assertEquals("RF channel should not of changed", vop.getRFChannel(),
         * newChannel);
         * 
         * // restore state vop.setRFBypass(currentState); }
         */

        // TODO: remove get/setRFBypass()
        /**
         * Test get/setRFBypass
         */
        /*
         * public void testRFBypass() { // first get the current state boolean
         * enabled = vop.getRFBypass();
         * assertEquals("Expected same state to be returned on consecutive calls"
         * , enabled, vop.getRFBypass()); // change the state
         * vop.setRFBypass(!enabled); boolean state = vop.getRFBypass();
         * assertEquals("Expected the RF bypass state to change", !enabled,
         * state);
         * assertEquals("Expected same state to be returned on consecutive calls"
         * , state, vop.getRFBypass()); // restore to previous state
         * vop.setRFBypass(enabled); }
         */

        /* ...Boilerplate... */
        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(InstanceTest.class);
            suite.setName(VideoOutputPort.class.getName());
            return suite;
        }

        public InstanceTest(String name, ImplFactory f)
        {
            this(name, VideoOutputPort.class, f);
        }

        protected InstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }

        protected VideoOutputPort createVideoOutputPort()
        {
            return (VideoOutputPort) createImplObject();
        }

        protected VideoOutputPort vop;

        protected void setUp() throws Exception
        {
            super.setUp();
            vop = createVideoOutputPort();
        }

        protected void tearDown() throws Exception
        {
            vop = null;
        }

        /**
         * Used to test security checks.
         */
        private static interface XTest
        {
            public void doX(VideoOutputPort vop);
        }
    }

    /**
     * Verify that there are no public constructors.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(VideoOutputPort.class);
    }

    /**
     * Verify that appropriate fields are defined.
     */
    public void testFields()
    {
        TestUtils.testFieldValues(VideoOutputPort.class, new String[] { "AV_OUTPUT_PORT_TYPE_RF",
                "AV_OUTPUT_PORT_TYPE_BB", "AV_OUTPUT_PORT_TYPE_SVIDEO", "AV_OUTPUT_PORT_TYPE_1394",
                "AV_OUTPUT_PORT_TYPE_DVI", "AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO", }, new int[] { 0, 1, 2, 3, 4, 5 });
        TestUtils.testFieldValues(VideoOutputPort.class, capabilityStrings, new int[] { 0, 1, 2 });
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
        /* VideoOutputPortTest */
        TestSuite suite = new TestSuite(VideoOutputPortTest.class);

        /* VideoOutputPortTest.InstanceTest foreach instance. */
        InterfaceTestSuite isuite = InstanceTest.isuite();
        for (Enumeration e = Host.getInstance().getVideoOutputPorts(); e.hasMoreElements();)
        {
            final VideoOutputPort vop = (VideoOutputPort) e.nextElement();
            isuite.addFactory(new ImplFactory()
            {
                public Object createImplObject()
                {
                    return vop;
                }
            });
        }
        suite.addTest(isuite);

        return suite;
    }

    public VideoOutputPortTest(String name)
    {
        super(name);
    }
}
