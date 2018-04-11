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

/**
 * Tests {@link #HFlatMatte}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.14 $, $Date: 2002/06/03 21:32:13 $
 */
public class HFlatMatteTest extends AbstractMatteTest
{
    /** True if mattes are supported on this platform */
    boolean matteSupported;

    /**
     * Standard constructor.
     */
    public HFlatMatteTest(String s)
    {
        super(s);
        matteSupported = TestSupport.getProperty("snap2.havi.test.flatMatteSupported", true);
    }

    /**
     * Parameterized test constructor.
     */
    public HFlatMatteTest(String s, Object params)
    {
        super(s, params);
        matteSupported = TestSupport.getProperty("snap2.havi.test.flatMatteSupported", true);
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite(HFlatMatteTest.class));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hmatte = new HFlatMatte();
    }

    /**
     * Test the 2 constructors of HFlatMatte.
     * <ul>
     * <li>HFlatMatte()
     * <li>HFlatMatte(float data)
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HFlatMatte()", (HFlatMatte) hmatte, 1.0f);
        checkConstructor("HFlatMatte(0.5f)", new HFlatMatte(0.5f), 0.5f);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HFlatMatte matte, float data)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", matte);
        assertEquals(msg + " matte data not initialized correctly", data, matte.getMatteData(), 0);
    }

    /**
     * Tests getMatteData/setMatteData
     * <ul>
     * <li>The set matte should be the retreived matte
     * </ul>
     */
    public void testMatteData()
    {
        HFlatMatte matte = (HFlatMatte) hmatte;

        matte.setMatteData(0.0f);
        assertEquals("Set matte data should be retrieved data", 0.0f, matte.getMatteData(), 0);
        matte.setMatteData(0.25f);
        assertEquals("Set matte data should be retrieved data", 0.25f, matte.getMatteData(), 0);
        matte.setMatteData(0.5f);
        assertEquals("Set matte data should be retrieved data", 0.5f, matte.getMatteData(), 0);
        matte.setMatteData(1.0f);
        assertEquals("Set matte data should be retrieved data", 1.0f, matte.getMatteData(), 0);
    }

    private static final float testData[] = { 0.0f, 0.3f, 1.0f };

    /**
     * Tests proper operation of matte with an HContainer.
     * <ul>
     * <li>GROUPED means that the HContainer's background (if there is one) as
     * well as the sub-components are affected by the matte.
     * <li>UNGROUPED means that only HContainer's background (there isn't one by
     * default) is affected by the matte.
     * </ul>
     */
    public void testContainer() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        // No addl. alpha for components
        // Ungrouped
        doTestContainer(180, 180, 180, 180, // main background image color
                180, 0, 180, 180, // container bg image color
                180, 180, 0, 180, // component bg image color
                180, 180, 180, 0, // component bg image color
                0.3f, 1.0f, 1.0f, // container/component/component matte values
                false); // ungrouped
        // No addl. alpha for components
        // Grouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 0.3f, 1.0f, 1.0f,
                true);
        // Addl. alpha for a component
        // Ungrouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 0.3f, 0.3f, 1.0f,
                false);
        // Addl. alpha for a component
        // Grouped
        doTestContainer(180, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 180, 180, 180, 180, 0, 0.3f, 0.3f, 1.0f,
                true);
    }

    /**
     * Tests proper operation of matte with an HComponent.
     */
    public void testComponent() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!matteSupported) return;

        doTestComponent(180, 255, 255, 0, // main background image color
                180, 255, 0, 255, // component bg image color
                1.0f); // component matte value
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, 0.3f);
        doTestComponent(180, 255, 255, 0, 180, 255, 0, 255, 0.0f);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, 1.0f);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, 0.3f);
        doTestComponent(180, 255, 255, 0, 255, 255, 0, 255, 0.0f);
    }

    /**
     * Internal mapping to the more generic doTestContainer method. Implicitly
     * generates the HFlatMatte and a FloatFraction parameters.
     * 
     * @param mainA
     *            main background image color alpha component
     * @param mainR
     *            main background image color red component
     * @param mainG
     *            main background image color blue component
     * @param mainB
     *            main background image color green component
     * @param bgA
     *            container background image color alpha component
     * @param bgR
     *            container background image color red component
     * @param bgG
     *            container background image color blue component
     * @param bgB
     *            container background image color green component
     * @param fg1A
     *            component #1 background image color alpha component
     * @param fg1R
     *            component #1 background image color red component
     * @param fg1G
     *            component #1 background image color blue component
     * @param fg1B
     *            component #1 background image color green component
     * @param fg2A
     *            component #2 background image color alpha component
     * @param fg2R
     *            component #2 background image color red component
     * @param fg2G
     *            component #2 background image color blue component
     * @param fg2B
     *            component #2 background image color green component
     * @param xA
     *            extra alpha component for container matte
     * @param x1A
     *            extra alpha component for component #1 matte
     * @param x2A
     *            extra alpha component for component #2 matte
     * @param grouped
     *            whether the container is grouped or not
     */
    private void doTestContainer(int mainA, int mainR, int mainG, int mainB, int bgA, int bgR, int bgG, int bgB,
            int fg1A, int fg1R, int fg1G, int fg1B, int fg2A, int fg2R, int fg2G, int fg2B, float xA, float x1A,
            float x2A, boolean grouped) throws Exception
    {
        doTestContainer(mainA, mainR, mainG, mainB, bgA, bgR, bgG, bgB, fg1A, fg1R, fg1G, fg1B, fg2A, fg2R, fg2G, fg2B,
                new FloatFraction(xA), new FloatFraction(x1A), new FloatFraction(x2A), new HFlatMatte(xA),
                (x1A == 1.0) ? null : new HFlatMatte(x1A), (x2A == 1.0) ? null : new HFlatMatte(x2A), grouped, SRC_OVER);
    }

    /**
     * Internal mapping to the more generic doTestComponent method. Implicitly
     * generates the HFlatMatte and a FloatFraction parameters.
     * 
     * @param dA
     *            background image alpha
     * @param dR
     *            background image red
     * @param dG
     *            background image green
     * @param dB
     *            background blue
     * @param sA
     *            component bg image alpha
     * @param sR
     *            component bg image red
     * @param sG
     *            component bg image green
     * @param sB
     *            component bg image blue
     * @param xA
     *            extra alpha component
     */
    private void doTestComponent(int dA, int dR, int dG, int dB, int sA, int sR, int sG, int sB, float xA)
            throws Exception
    {
        doTestComponent(dA, dR, dG, dB, sA, sR, sG, sB, new FloatFraction(xA), new HFlatMatte(xA), SRC_OVER);
    }
}
