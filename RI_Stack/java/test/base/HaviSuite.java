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

//   == has failures/errors
//// == crashes the box or hangs
//g  == problems with graphics memory, see 5193

import junit.framework.TestSuite;

public class HaviSuite
{
    public static TestSuite suite()
    {
        TestSuite suite;
        suite = new TestSuite("Havi");

        // 03-02-2007 53 tests 38 failures
        // x suite.addTestSuite(org.havi.ui.HAnimateLookTest.class);
        // // suite.addTestSuite(org.havi.ui.HAnimationTest.class);
        suite.addTestSuite(org.havi.ui.HBackgroundConfigTemplateTest.class);
        suite.addTestSuite(org.havi.ui.HBackgroundConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HBackgroundDeviceTest.class);
        suite.addTestSuite(org.havi.ui.HBackgroundImageTest.class);
        suite.addTestSuite(org.havi.ui.HChangeDataTest.class);
        suite.addTestSuite(org.havi.ui.HComponentTest.class);
        suite.addTestSuite(org.havi.ui.HConfigurationExceptionTest.class);
        suite.addTestSuite(org.havi.ui.HContainerTest.class);
        suite.addTestSuite(org.havi.ui.HDefaultTextLayoutManagerTest.class);
        suite.addTestSuite(org.havi.ui.HEmulatedGraphicsConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HEmulatedGraphicsDeviceTest.class);
        suite.addTestSuite(org.havi.ui.HEventMulticasterTest.class);
        // 03-02-2007 17 tests 5 failures 2 errors
        // suite.addTestSuite(org.havi.ui.HFlatEffectMatteTest.class);
        suite.addTestSuite(org.havi.ui.HFlatMatteTest.class);
        suite.addTestSuite(org.havi.ui.HFontCapabilitiesTest.class);
        suite.addTestSuite(org.havi.ui.HGraphicButtonTest.class);
        // 03-02-2007 52 tests 33 failures
        // x suite.addTestSuite(org.havi.ui.HGraphicLookTest.class);
        suite.addTestSuite(org.havi.ui.HGraphicsConfigTemplateTest.class);
        suite.addTestSuite(org.havi.ui.HGraphicsConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HGraphicsDeviceTest.class);
        suite.addTestSuite(org.havi.ui.HIconTest.class);
        suite.addTestSuite(org.havi.ui.HImageHintsTest.class);
        suite.addTestSuite(org.havi.ui.HImageMatteTest.class);
        // 03-02-2007 18 tests 5 failures 2 errors
        // suite.addTestSuite(org.havi.ui.HImageEffectMatteTest.class);
        suite.addTestSuite(org.havi.ui.HInvalidLookExceptionTest.class);
        suite.addTestSuite(org.havi.ui.HListElementTest.class);
        // 03-02-2007 57 tests 39 failures
        // g suite.addTestSuite(org.havi.ui.HListGroupLookTest.class);
        suite.addTestSuite(org.havi.ui.HListGroupTest.class);
        suite.addTestSuite(org.havi.ui.HMatteExceptionTest.class);
        suite.addTestSuite(org.havi.ui.HMultilineEntryLookTest.class);
        suite.addTestSuite(org.havi.ui.HMultilineEntryTest.class);
        suite.addTestSuite(org.havi.ui.HPermissionDeniedExceptionTest.class);
        suite.addTestSuite(org.havi.ui.HRangeLookTest.class);
        suite.addTestSuite(org.havi.ui.HRangeTest.class);
        suite.addTestSuite(org.havi.ui.HRangeValueTest.class);
        suite.addTestSuite(org.havi.ui.HSceneFactoryTest.class);
        suite.addTestSuite(org.havi.ui.HSceneTemplateTest.class);
        suite.addTestSuite(org.havi.ui.HSceneTest.class);
        suite.addTestSuite(org.havi.ui.HScreenConfigTemplateTest.class);
        suite.addTestSuite(org.havi.ui.HScreenConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HScreenDeviceTest.class);
        suite.addTestSuite(org.havi.ui.HScreenDimensionTest.class);
        suite.addTestSuite(org.havi.ui.HScreenPointTest.class);
        suite.addTestSuite(org.havi.ui.HScreenRectangleTest.class);
        suite.addTestSuite(org.havi.ui.HScreenTest.class);
        suite.addTestSuite(org.havi.ui.HSinglelineEntryLookTest.class);
        suite.addTestSuite(org.havi.ui.HSinglelineEntryTest.class);
        suite.addTestSuite(org.havi.ui.HSoundTest.class);
        // // suite.addTestSuite(org.havi.ui.HStaticAnimationTest.class);
        suite.addTestSuite(org.havi.ui.HStaticIconTest.class);
        suite.addTestSuite(org.havi.ui.HStaticRangeTest.class);
        suite.addTestSuite(org.havi.ui.HStaticTextTest.class);
        suite.addTestSuite(org.havi.ui.HStillImageBackgroundConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HTextButtonTest.class);
        suite.addTestSuite(org.havi.ui.HTextTest.class);
        suite.addTestSuite(org.havi.ui.HToggleButtonTest.class);
        suite.addTestSuite(org.havi.ui.HToggleGroupTest.class);
        suite.addTestSuite(org.havi.ui.HUIExceptionTest.class);
        suite.addTestSuite(org.havi.ui.HVideoComponentTest.class);
        suite.addTestSuite(org.havi.ui.HVideoConfigTemplateTest.class);
        suite.addTestSuite(org.havi.ui.HVideoConfigurationTest.class);
        suite.addTestSuite(org.havi.ui.HVideoDeviceTest.class);
        suite.addTestSuite(org.havi.ui.HVisibleTest.class);

        // ui/event
        suite.addTestSuite(org.havi.ui.event.HActionEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HAdjustmentEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HBackgroundImageEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HEventGroupTest.class);
        suite.addTestSuite(org.havi.ui.event.HEventRepresentationTest.class);
        suite.addTestSuite(org.havi.ui.event.HFocusEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HItemEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HKeyCapabilitiesTest.class);
        suite.addTestSuite(org.havi.ui.event.HKeyEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HMouseCapabilitiesTest.class);
        suite.addTestSuite(org.havi.ui.event.HRcCapabilitiesTest.class);
        suite.addTestSuite(org.havi.ui.event.HRcEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HScreenConfigurationEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HScreenDeviceReleasedEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HScreenDeviceReservedEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HScreenLocationModifiedEventTest.class);
        suite.addTestSuite(org.havi.ui.event.HTextEventTest.class);
        suite.addTestSuite(org.havi.ui.HTextLookTest.class);
        suite.addTestSuite(org.havi.ui.HVersionTest.class);

        // signal tests are done
        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite());

        return suite;
    }

    public static void main(String[] args)
    {
        System.out.println("\n************************************************************");
        System.out.println("* Starting Havi tests.");
        System.out.println("************************************************************");
        org.cablelabs.test.textui.TestRunner.run(suite());
        System.out.println("\n************************************************************");
        System.out.println("* Finishing Havi tests.");
        System.out.println("************************************************************");
        System.exit(0);
    }
}
