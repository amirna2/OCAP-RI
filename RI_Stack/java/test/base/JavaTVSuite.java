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

import junit.framework.TestSuite;

public class JavaTVSuite
{
    public static TestSuite suite()
    {

        TestSuite suite;

        suite = new TestSuite("JavaTV");

        suite.addTest(org.cablelabs.impl.manager.service.AbstractServiceImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.CannedServiceMgrTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.CannedSIDatabaseTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.ServiceMgrImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.SISnapshotManagerCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.NetworkLocatorTest.suite());
        suite.addTest(org.cablelabs.impl.service.PackageExceptionTest.suite());
        suite.addTest(org.cablelabs.impl.service.ServiceContextFactoryExtCannedTest.isuite());
        suite.addTest(org.cablelabs.impl.service.ServiceContextExtCannedTest.isuite());
        suite.addTest(org.cablelabs.impl.service.SICacheCannedTest.isuite());
        suite.addTest(org.cablelabs.impl.service.SIDatabaseCannedTest.isuite());
        suite.addTest(org.cablelabs.impl.service.javatv.navigation.ServiceComponentImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.navigation.ServiceDescriptionImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.selection.ServiceContextImplTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.service.RatingDimensionImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.service.ServiceImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.transport.NetworkImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.transport.TransportImplCannedTest.suite());
        suite.addTest(org.cablelabs.impl.service.javatv.transport.TransportStreamImplCannedTest.suite());
        suite.addTest(javax.tv.service.PackageTypesCannedTest.suite());
        suite.addTest(javax.tv.service.SIManagerCannedTest.isuite());
        suite.addTest(javax.tv.service.navigation.LocatorFilterCannedTest.suite());
        suite.addTest(javax.tv.service.navigation.PackageTypesAndExceptionsCannedTest.suite());
        suite.addTest(javax.tv.service.navigation.PreferenceFilterCannedTest.suite());
        suite.addTest(javax.tv.service.navigation.ServiceIteratorCannedTest.isuite());
        suite.addTest(javax.tv.service.navigation.ServiceListCannedTest.isuite());
        suite.addTest(javax.tv.service.navigation.ServiceTypeFilterCannedTest.suite());
        suite.addTest(javax.tv.service.navigation.SIElementFilterCannedTest.suite());
        suite.addTest(javax.tv.service.selection.PackageEventsAndExceptionsCannedTest.suite());
        suite.addTest(javax.tv.service.selection.ServiceContextFactoryTest.suite());
        suite.addTest(javax.tv.service.selection.ServiceContextTest.suite());
        suite.addTest(javax.tv.service.transport.PackageEventsCannedTest.suite());
        suite.addTest(org.ocap.service.ServiceTypePermissionTest.suite());
        suite.addTest(org.dvb.spi.ProviderRegistryTest.suite());
        suite.addTest(org.dvb.spi.selection.SelectionProviderContextTest.suite());
        suite.addTest(org.cablelabs.impl.spi.SelectionProviderInstanceTest.suite());

        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite()); // signal
                                                                       // tests
                                                                       // are
                                                                       // done

        return suite;
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
}
