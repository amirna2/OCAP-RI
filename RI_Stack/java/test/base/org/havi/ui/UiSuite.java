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

import junit.framework.TestSuite;

public class UiSuite
{
    public static TestSuite suite() throws Exception
    {

        TestSuite suite;

        suite = new TestSuite("org.havi.ui");

        /*
         * Commented-out entries need to be revisited: - Add a suite() method. -
         * Remove if "abstract" test. - Make sure works.
         */

        // suite.addTest(HAnimateLookTest.suite());
        // suite.addTest(HAnimationTest.suite());
        // suite.addTest(HBackgroundConfigTemplateTest.suite());
        suite.addTest(HBackgroundConfigurationTest.suite());
        suite.addTest(HBackgroundDeviceTest.suite());
        // suite.addTest(HBackgroundImageTest.suite());
        // suite.addTest(HChangeDataTest.suite());
        // suite.addTest(HComponentTest.suite());
        // suite.addTest(HConfigurationExceptionTest.suite());
        // suite.addTest(HContainerTest.suite());
        // suite.addTest(HDefaultTextLayoutManagerTest.suite());
        // suite.addTest(HEmulatedGraphicsConfigurationTest.suite());
        // suite.addTest(HEmulatedGraphicsDeviceTest.suite());
        // suite.addTest(HEventMulticasterTest.suite());
        // suite.addTest(HFlatEffectMatteTest.suite());
        // suite.addTest(HFlatMatteTest.suite());
        // suite.addTest(HFontCapabilitiesTest.suite());
        // suite.addTest(HGraphicButtonTest.suite());
        // suite.addTest(HGraphicLookTest.suite());
        // suite.addTest(HGraphicsConfigTemplateTest.suite());
        suite.addTest(HGraphicsConfigurationTest.suite());
        suite.addTest(HGraphicsDeviceTest.suite());
        // suite.addTest(HIconTest.suite());
        // suite.addTest(HImageEffectMatteTest.suite());
        // suite.addTest(HImageHintsTest.suite());
        // suite.addTest(HImageMatteTest.suite());
        // suite.addTest(HInvalidLookExceptionTest.suite());
        // suite.addTest(HItemValueTest.suite());
        // suite.addTest(HListElementTest.suite());
        // suite.addTest(HListGroupLookTest.suite());
        // suite.addTest(HListGroupTest.suite());
        // suite.addTest(HMatteExceptionTest.suite());
        // suite.addTest(HMultilineEntryLookTest.suite());
        // suite.addTest(HMultilineEntryTest.suite());
        // suite.addTest(HPermissionDeniedExceptionTest.suite());
        // suite.addTest(HRangeLookTest.suite());
        // suite.addTest(HRangeTest.suite());
        // suite.addTest(HRangeValueTest.suite());
        // suite.addTest(HSceneFactoryTest.suite());
        // suite.addTest(HSceneTemplateTest.suite());
        // suite.addTest(HSceneTest.suite());
        // suite.addTest(HScreenConfigTemplateTest.suite());
        suite.addTest(HScreenConfigurationTest.suite());
        suite.addTest(HScreenDeviceTest.suite());
        // suite.addTest(HScreenDimensionTest.suite());
        // suite.addTest(HScreenPointTest.suite());
        // suite.addTest(HScreenRectangleTest.suite());
        suite.addTest(HScreenTest.suite());
        // suite.addTest(HSinglelineEntryLookTest.suite());
        // suite.addTest(HSinglelineEntryTest.suite());
        // suite.addTest(HSoundTest.suite());
        // suite.addTest(HStaticAnimationTest.suite());
        // suite.addTest(HStaticIconTest.suite());
        // suite.addTest(HStaticRangeTest.suite());
        // suite.addTest(HStaticTextTest.suite());
        // suite.addTest(HStillImageBackgroundConfigurationTest.suite());
        // suite.addTest(HTextButtonTest.suite());
        // suite.addTest(HTextLookTest.suite());
        // suite.addTest(HTextTest.suite());
        // suite.addTest(HToggleButtonTest.suite());
        // suite.addTest(HToggleGroupTest.suite());
        // suite.addTest(HUIExceptionTest.suite());
        // suite.addTest(HVersionTest.suite());
        // suite.addTest(HVideoComponentTest.suite());
        // suite.addTest(HVideoConfigTemplateTest.suite());
        suite.addTest(HVideoConfigurationTest.suite());
        suite.addTest(HVideoDeviceTest.suite());
        // suite.addTest(HVisibleTest.suite());

        return suite;
    }

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
}
