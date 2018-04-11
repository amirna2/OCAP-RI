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

import junit.framework.TestSuite;

public class OrgSuite
{
    public static TestSuite suite()
    {
        TestSuite suite;
        suite = new TestSuite("Org");

        // davic
        // -----
        // net
        suite.addTest(org.davic.net.LocatorTest.suite());
        suite.addTest(org.davic.net.tuning.NetworkInterfaceTest.suite());
        suite.addTest(org.davic.net.tuning.NetworkInterfaceManagerTest.suite());
        suite.addTest(org.davic.net.tuning.StreamTableTest.suite());

        // resources
        suite.addTest(org.davic.resources.ResourceStatusEventTest.suite());

        // dvb
        // ---
        suite.addTest(org.dvb.application.AppIDTest.suite());
        suite.addTest(org.dvb.application.AppsDatabaseTest.suite());
        suite.addTest(org.dvb.application.AppStateChangeEventTest.suite());
        suite.addTest(org.dvb.application.AppsControlPermissionTest.suite());
        suite.addTest(org.dvb.application.AppsDatabaseEventTest.suite());
        suite.addTest(org.dvb.application.CurrentServiceFilterTest.suite());
        suite.addTest(org.dvb.application.IllegalProfileParameterExceptionTest.suite());
        suite.addTest(org.dvb.application.RunningApplicationsFilterTest.suite());
        suite.addTest(org.dvb.net.tuning.TunerPermissionTest.suite());
        suite.addTest(org.dvb.net.rc.RCInterfaceManagerTest.suite());

        // dsmcc
        suite.addTest(org.dvb.dsmcc.DSMCCExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.DSMCCObjectTest.suite());
        suite.addTest(org.dvb.dsmcc.DSMCCStreamEventTest.suite());
        suite.addTest(org.dvb.dsmcc.IllegalObjectTypeExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.InsufficientResourcesExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.InvalidAddressExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.InvalidFormatEventTest.suite());
        suite.addTest(org.dvb.dsmcc.InvalidFormatExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.InvalidPathNameExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.InvalidPathnameEventTest.suite());
        suite.addTest(org.dvb.dsmcc.LoadingAbortedEventTest.suite());
        suite.addTest(org.dvb.dsmcc.MPEGDeliveryErrorEventTest.suite());
        suite.addTest(org.dvb.dsmcc.MPEGDeliveryExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.NPTRateTest.suite());
        suite.addTest(org.dvb.dsmcc.NotEntitledEventTest.suite());
        suite.addTest(org.dvb.dsmcc.NotEntitledExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.NotLoadedExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.NothingToAbortExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.ObjectChangeEventTest.suite());
        suite.addTest(org.dvb.dsmcc.ServerDeliveryErrorEventTest.suite());
        suite.addTest(org.dvb.dsmcc.ServerDeliveryExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.ServiceDomainTest.suite());
        suite.addTest(org.dvb.dsmcc.ServiceXFRErrorEventTest.suite());
        suite.addTest(org.dvb.dsmcc.ServiceXFRExceptionTest.suite());
        suite.addTest(org.dvb.dsmcc.ServiceXFRReferenceTest.suite());
        suite.addTest(org.dvb.dsmcc.StreamEventTest.suite());
        suite.addTest(org.dvb.dsmcc.SuccessEventTest.suite());
        suite.addTest(org.dvb.dsmcc.UnknownEventExceptionTest.suite());

        // event
        suite.addTest(org.dvb.event.EventManagerTest.suite());
        suite.addTest(org.dvb.event.OverallRepositoryTest.suite());
        suite.addTest(org.dvb.event.UserEventAvailableEventTest.suite());
        suite.addTest(org.dvb.event.UserEventRepositoryTest.suite());
        suite.addTest(org.dvb.event.UserEventTest.suite());
        suite.addTest(org.dvb.event.UserEventUnavailableEventTest.suite());

        // io
        suite.addTest(org.dvb.io.persistent.FileAccessPermissionsTest.suite());
        suite.addTest(org.dvb.io.persistent.FileAttributesTest.suite());

        // net
        suite.addTest(org.dvb.net.DatagramSocketBufferControlTest.suite());

        // test
        suite.addTest(org.dvb.test.DVBTestTest.suite());

        // ui
        suite.addTest(org.dvb.ui.DVBAlphaCompositeTest.suite());
        suite.addTest(org.dvb.ui.DVBBufferedImageTest.suite());
        suite.addTest(org.dvb.ui.DVBColorTest.suite());
        suite.addTest(org.dvb.ui.DVBGraphicsTest.suite());
        suite.addTest(org.dvb.ui.DVBRasterFormatExceptionTest.suite());
        suite.addTest(org.dvb.ui.DVBTextLayoutManagerTest.suite());
        suite.addTest(org.dvb.ui.FontFactoryTest.suite());
        suite.addTest(org.dvb.ui.FontFormatExceptionTest.suite());
        suite.addTest(org.dvb.ui.FontNotAvailableExceptionTest.suite());
        suite.addTest(org.dvb.ui.UnsupportedDrawingOperationExceptionTest.suite());

        // user
        suite.addTest(org.dvb.user.GeneralPreferenceTest.suite());
        suite.addTest(org.dvb.user.UserPreferenceManagerTest.suite());

        // ocap
        // ----
        suite.addTestSuite(org.ocap.media.AlternativeMediaPresentationReasonTest.class);
        suite.addTestSuite(org.ocap.media.MediaTimerTest.class);
        suite.addTest(org.ocap.resource.ResourceContentionManagerTest.suite());
        suite.addTest(org.ocap.OcapSystemTest.suite());
        suite.addTestSuite(org.ocap.hardware.PowerModeChangeListenerTest.class);
        suite.addTest(org.ocap.hardware.pod.PODTest.suite());
        suite.addTestSuite(org.ocap.storage.AvailableStorageListenerTest.class);
        suite.addTestSuite(org.ocap.storage.StorageManagerTest.class);
        suite.addTestSuite(org.ocap.storage.StorageProxyTest.class);
        suite.addTestSuite(org.ocap.storage.DetachableStorageOptionTest.class);
        suite.addTestSuite(org.ocap.storage.RemovableStorageOptionTest.class);
        suite.addTest(org.ocap.ui.HSceneManagerTest.suite());

        // application
        suite.addTest(org.ocap.application.AppFilterTest.suite());
        suite.addTest(org.ocap.application.AppManagerProxyTest.suite());
        suite.addTest(org.ocap.application.AppPatternTest.suite());

        // event
        suite.addTest(org.ocap.event.UserEventActionTest.suite());
        suite.addTest(org.ocap.event.UserEventTest.suite());
        suite.addTest(org.ocap.event.EventManagerTest.suite());

        // hardware
        suite.addTestSuite(org.ocap.hardware.HostTest.class);

        // media
        suite.addTestSuite(org.ocap.media.ClosedCaptioningAttributeTest.class);
        suite.addTestSuite(org.ocap.media.MediaTimerListenerTest.class);
        suite.addTestSuite(org.ocap.media.VBIFilterListenerTest.class);

        // mpeg
        suite.addTest(org.ocap.mpeg.PODExtendedChannelTest.suite());

        // net
        suite.addTest(org.ocap.net.OcapLocatorTest.suite());
        suite.addTest(org.ocap.net.OCRCInterfaceTest.suite());

        // system events
        suite.addTest(org.ocap.system.event.EventSuite.suite());

        // test
        suite.addTest(org.ocap.test.OCAPTestTest.suite());

        // ui/event
        suite.addTest(org.ocap.ui.event.OCRcEventTest.suite());

        // section filter
        suite.addTest(org.davic.mpeg.sections.SectionsSuite.suite());

        // signal tests are done
        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite());
        return suite;
    }

    public static void main(String[] args)
    {
        System.out.println("\n************************************************************");
        System.out.println("* Starting Org tests.");
        System.out.println("************************************************************");
        org.cablelabs.test.textui.TestRunner.run(suite());
        System.out.println("\n************************************************************");
        System.out.println("* Finishing Org tests.");
        System.out.println("************************************************************");
        System.exit(0);
    }
}
