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
//-  == Brian's secret code for 'Hey, this used to work.'
//$$ == Randy's secret code for "Runs to completion but takes a long time, so I'm commenting out while I debug other tests"
//x  == setup method throws exceptions when run in a suite

import junit.framework.TestSuite;

public class ImplSuite
{
    public static TestSuite suite()
    {
        TestSuite suite;
        suite = new TestSuite("Impl");

        // base
        suite.addTest(SIDatabaseImplTest.suite());

        // awt
        suite.addTest(org.cablelabs.impl.awt.MPEToolkitTest.suite()); // OK

        // davic
        suite.addTest(org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImplTest.suite()); // OK
        suite.addTest(org.cablelabs.impl.davic.net.tuning.NetworkInterfaceManagerImplTest.suite()); // OK

        // ocap
        suite.addTest(org.cablelabs.impl.ocap.OcapMainTest.suite());
        suite.addTest(org.cablelabs.impl.ocap.si.PATTableChangeManagerTest.suite());
        suite.addTest(org.cablelabs.impl.ocap.si.PMTTableChangeManagerTest.suite());
        suite.addTestSuite(org.cablelabs.impl.ocap.si.DescriptorImplTest.class);

        // security
        suite.addTest(org.cablelabs.impl.security.AppCodeSourceTest.suite());
        suite.addTest(org.cablelabs.impl.security.AppPermissionsTest.suite());
        suite.addTest(org.cablelabs.impl.security.PermissionCollectionTest.suite()); // takes
                                                                                     // a
                                                                                     // long
                                                                                     // time
                                                                                     // to
                                                                                     // finish
        suite.addTest(org.cablelabs.impl.security.PermissionInfoImplTest.suite());
        suite.addTest(org.cablelabs.impl.security.PersistentFileCredentialTest.suite()); // Tests
                                                                                         // run:
                                                                                         // 62,
                                                                                         // Failures:
                                                                                         // 1,
                                                                                         // Errors:
                                                                                         // 0,
                                                                                         // 28.1
                                                                                         // seconds
        suite.addTest(org.cablelabs.impl.security.PolicyImplTest.suite()); // Tests
                                                                           // run:
                                                                           // 78,
                                                                           // Failures:
                                                                           // 1,
                                                                           // Errors:
                                                                           // 0,
                                                                           // Time:
                                                                           // 1,020.113
        suite.addTest(org.cablelabs.impl.security.SecurityManagerImplTest.suite()); // gets
                                                                                    // here
                                                                                    // but
                                                                                    // hangs
                                                                                    // on
                                                                                    // this
                                                                                    // suite

        // signalling
        suite.addTest(org.cablelabs.impl.manager.signalling.AitPropsTest.suite());

        // sound
        suite.addTest(org.cablelabs.impl.sound.mpe.SoundTestSuite.suite());
        // util
        suite.addTest(org.cablelabs.impl.util.MPEEnvTest.suite());

        // new tests
        suite.addTest(org.cablelabs.impl.awt.LeakTest.suite());
        suite.addTest(org.cablelabs.impl.davic.mpeg.sections.BasicSectionTest.suite());
        // x
        // suite.addTest(org.cablelabs.impl.davic.mpeg.NotAuthorizedExceptionTest.suite());
        suite.addTest(org.cablelabs.impl.havi.HEventRepresentationDatabaseTest.suite());
        suite.addTest(org.cablelabs.impl.util.EventMulticasterTest.suite());

        suite.addTest(org.cablelabs.impl.io.DefaultFileSysTest.suite());
        suite.addTest(org.cablelabs.impl.io.DefaultOpenFileTest.suite());
        suite.addTest(org.cablelabs.impl.io.FileSysManagerTest.suite());
        suite.addTest(org.cablelabs.impl.io.http.HttpFileSysTest.suite());
        suite.addTest(org.cablelabs.impl.manager.signalling.XaitPropsTest.suite());
        suite.addTest(org.cablelabs.impl.util.string.MultiStringTest.suite());

        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite());

        return suite;
    }

    public static void main(String[] args)
    {
        System.out.println("\n**********************************************************");
        System.out.println("* Starting Impl tests.");
        System.out.println("************************************************************");
        org.cablelabs.test.textui.TestRunner.run(suite());
        System.out.println("\n**********************************************************");
        System.out.println("* Finishing Impl tests.");
        System.out.println("************************************************************");
        System.exit(0);
    }
}
