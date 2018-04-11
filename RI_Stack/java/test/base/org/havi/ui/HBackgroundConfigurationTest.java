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
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests {@link #HBackgroundConfiguration}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:11 $
 */
public class HBackgroundConfigurationTest extends HScreenConfigurationTest
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HScreenConfiguration
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HBackgroundConfiguration.class, HScreenConfiguration.class);
    }

    /**
     * Test the constructor of HBackgroundConfiguration.
     * <ul>
     * <li>HBackgroundConfiguration()
     * </ul>
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HBackgroundConfiguration.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HBackgroundConfiguration.class);
    }

    /**
     * Tests instances of HBackgroundConfiguration.
     */
    public static class BgInstanceTest extends InstanceTest
    {
        /**
         * Tests getColor/setColor.
         * <ul>
         * <li>The set color should be the retreived color
         * </ul>
         * 
         * Doesn't test whether the color is <i>actually</i> displayed.
         */
        public void testColor() throws Exception
        {
            device.reserveDevice(new HScreenDeviceTest.TestResourceClient());
            try
            {
                HScreenDeviceTest.setConfig(device, config);

                HBackgroundConfiguration bg = (HBackgroundConfiguration) config;
                bg.setColor(Color.black);
                assertEquals("Expected setColor to be retrieved", Color.black, bg.getColor());
            }
            finally
            {
                device.releaseDevice();
            }
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(BgInstanceTest.class);
            suite.setName(HBackgroundConfiguration.class.getName());
            return suite;
        }

        public BgInstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected BgInstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }
    }

    /**
     * Standard constructor.
     */
    public HBackgroundConfigurationTest(String str)
    {
        super(str);
    }

    public static Test suite() throws Exception
    {
        return suite(BgInstanceTest.isuite(), HBackgroundConfigurationTest.class, "background");
    }

    /**
     * Standalone runner.
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
        System.exit(0);
    }

} // End of HBackgroundConfigurationTest
