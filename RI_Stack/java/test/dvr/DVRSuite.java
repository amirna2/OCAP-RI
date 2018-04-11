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

import org.cablelabs.impl.manager.recording.PlaybackNotificationTests;
import org.cablelabs.impl.media.mpe.DVRAPITest;
import org.cablelabs.impl.media.player.RecordingPlayerTest;
import org.cablelabs.impl.media.player.SegmentedRecordingPlayerTest;
import org.cablelabs.impl.media.player.TSBPlayerTest;
import org.cablelabs.impl.media.source.RecordingDataSourceImplTest;
import org.cablelabs.impl.media.source.RecordingDataSourceTest;
import org.cablelabs.impl.media.source.TSBDataSourceImplTest;
import org.cablelabs.impl.media.source.TSBDataSourceTest;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 *
 */
public class DVRSuite extends TestCase
{
    public static TestSuite suite()
    {
        TestSuite suite;

        suite = new TestSuite("DVRSuite");

        suite.addTest(org.cablelabs.impl.manager.recording.db.SerializationMgrTest.suite()); // rg-runs-33-22-0
        suite.addTestSuite(org.cablelabs.impl.manager.recording.NavigationManagerTestCase.class); // rg-runs-7-0-5
        suite.addTest(org.cablelabs.impl.manager.recording.RecordedServiceImplTest.suite()); // rg-cannot
                                                                                             // resolve
                                                                                             // symbol
                                                                                             // will
                                                                                             // not
                                                                                             // compile;
                                                                                             // 2
                                                                                             // 2
                                                                                             // 0
        suite.addTest(org.cablelabs.impl.manager.recording.RecordingImplTest.suite()); // rg-runs-5-5-0
                                                                                       // 5
                                                                                       // 4
                                                                                       // 1
        suite.addTestSuite(org.cablelabs.impl.manager.recording.RecordingListTestCase.class); // rg-runs-1-1-0
                                                                                              // 4
        suite.addTest(org.cablelabs.impl.manager.recording.RecordingManagerImplTest.suite()); // rg-runs-10-2-0
                                                                                              // 10
                                                                                              // 2
                                                                                              // 0
        suite.addTestSuite(org.cablelabs.impl.manager.recording.SchedulerTestCase.class); // rg-runs-10-0-0
        suite.addTestSuite(org.cablelabs.impl.manager.RecordingDBManagerTest.class); // 1
                                                                                     // 1
                                                                                     // 0
        suite.addTest(org.cablelabs.impl.recording.AppDataContainerTest.suite()); // rg-runs-9
                                                                                  // 0
                                                                                  // 0
                                                                                  // 9
                                                                                  // 9
                                                                                  // 0
        suite.addTest(org.cablelabs.impl.recording.RecordingInfoTest.suite()); // rg-runs-6-0-0
                                                                               // 6
                                                                               // 0
                                                                               // 1
        suite.addTest(org.cablelabs.impl.service.javatv.selection.DVRServiceContextImplTest.suite()); // rg-4-1-0
                                                                                                      // 4
                                                                                                      // 1
                                                                                                      // 3
        suite.addTest(org.cablelabs.impl.storage.MediaStorageVolumeImplTest.suite()); // rg-runs
                                                                                      // 1
        suite.addTest(org.cablelabs.impl.storage.TimeShiftBufferOptionImplTest.suite()); // rg-runs
                                                                                         // 2
        suite.addTest(org.ocap.dvr.OcapRecordingPropertiesTest.suite()); // rg-runs
                                                                         // 2
        suite.addTestSuite(org.cablelabs.impl.util.TimeTableTest.class);
        suite.addTest(DVRAPITest.suite());
        suite.addTest(RecordingDataSourceImplTest.suite());
        suite.addTestSuite(RecordingDataSourceTest.class);
        suite.addTest(TSBDataSourceImplTest.suite());
        suite.addTestSuite(TSBDataSourceTest.class);
        suite.addTest(TSBPlayerTest.suite());
        suite.addTest(RecordingPlayerTest.suite());
        suite.addTest(SegmentedRecordingPlayerTest.suite());
        suite.addTest(PlaybackNotificationTests.suite());

        // signal tests are done
        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite());

        return suite;
    }

    public static void main(String[] args)
    {
        System.out.println("\n**********************************************************");
        System.out.println("* Starting DVR Junit tests.");
        System.out.println("************************************************************");
        org.cablelabs.test.textui.TestRunner.run(suite());
        System.out.println("\n**********************************************************");
        System.out.println("* Finishing DVR Junit tests.");
        System.out.println("************************************************************");
        System.exit(0);
    }
}
