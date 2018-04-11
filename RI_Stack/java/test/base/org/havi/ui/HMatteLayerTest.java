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
import org.cablelabs.test.*;

/**
 * Test framework required for HMatteLayer tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HMatteLayerTest extends Assert
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HMatteLayer
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HMatteLayer.class);
    }

    /**
     * Tests getMatte/setMatte
     * <ul>
     * <li>The set matte should be the retreived matte
     * <li>Each of the 4 standard mattes should be allowed.
     * <li>A null matte should be allowed
     * <li>Check for exception when setting matte and container already has a
     * running effect matte.
     * </ul>
     */
    public static void testMatte(HMatteLayer hc) throws HMatteException
    {
        assertNull("Matte should be unassigned", hc.getMatte());

        // Array of matte types
        HMatte mattes[] = new HMatte[] { new HFlatMatte(), new HImageMatte(), new HFlatEffectMatte(),
                new HImageEffectMatte(), };

        // Array of booleans that indicate whether the matte type is supported
        // on this platform.
        boolean[] matteTypeSupported = new boolean[] {
                TestSupport.getProperty("snap2.havi.test.flatMatteSupported", true),
                TestSupport.getProperty("snap2.havi.test.imageMatteSupported", true),
                TestSupport.getProperty("snap2.havi.test.flatEffectMatteSupported", true),
                TestSupport.getProperty("snap2.havi.test.imageEffectMatteSupported", true) };
        boolean performMatteTests = TestSupport.getProperty("snap2.havi.test.matteLayerTest", true);
        if (performMatteTests)
        {
            // Test each kind of matte
            for (int i = 0; i < mattes.length; ++i)
            {
                if (matteTypeSupported[i])
                {
                    // The matte type is supported. Make sure we can set and get
                    // the matte.
                    hc.setMatte(mattes[i]);
                    assertSame("Set matte should be retreived matte", mattes[i], hc.getMatte());
                }
                else
                {
                    // The matte type is not supported. Make sure the exception
                    // is
                    // thrown.
                    boolean exceptionThrown = false;
                    try
                    {
                        hc.setMatte(mattes[i]);
                    }
                    catch (HMatteException e)
                    {
                        exceptionThrown = true;
                    }
                    assertTrue("Set matte should throw HMatteException", exceptionThrown);
                }
            }
        }
        // Test clearing of matte
        hc.setMatte(null);
        assertNull("Matte should be cleared", hc.getMatte());

        if (performMatteTests)
        {
            // Test with an already running effect matte
            if (matteTypeSupported[2])
            {
                HFlatEffectMatte matte = new HFlatEffectMatte(new float[] { 0.0f, 0.1f, 0.2f, 0.5f, 1.0f });
                hc.setMatte(matte);
                try
                {
                    matte.start();

                    for (int i = 0; i < mattes.length; ++i)
                    {
                        try
                        {
                            hc.setMatte(mattes[i]);
                            fail("Exception should be thrown when already "
                                    + "associated with a running effect matte [" + i + "]");
                        }
                        catch (HMatteException expected)
                        {
                            // Expected
                        }
                    }

                }
                finally
                {
                    matte.stop();
                    hc.setMatte(null);
                }
            }
            // Test with an unknown matte type
            try
            {
                hc.setMatte(new HMatte()
                {
                });
                fail("Should not work with an unknown matte type");
            }
            catch (HMatteException expected)
            {
            }
        }
    }
}
