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

import org.cablelabs.impl.manager.EventManager;
import org.cablelabs.impl.manager.ManagerManager;

public class ManagerSuite
{
    public static TestSuite suite()
    {

        /**
         *@todo This is a workaround for making sure the ed queue is
         *       initialized see 4598
         */
        ManagerManager.getInstance(EventManager.class);
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException exc)
        {

        }
        TestSuite suite;
        suite = new TestSuite("Manager");

        // manager
        suite.addTest(org.cablelabs.impl.manager.CallerContextMulticasterTest.suite());
        // suite.addTestSuite(org.cablelabs.impl.manager.ManagerManagerTest.class);
        suite.addTest(org.cablelabs.impl.manager.ResourceManagerClientTest.suite());
        suite.addTestSuite(org.cablelabs.impl.manager.ServiceManagerCannedTest.class);

        // manager/application
        suite.addTest(org.cablelabs.impl.manager.application.AppClassLoaderTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.AppDomainImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.AppExecQueueTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.AppIconImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.ApiRegistrarTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.AppManagerProxyImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.AppThreadGroupTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.CCMgrTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.CompositeAppsDBTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.DemandExecQueueTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.ExecQueueTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.SystemContextTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.XletAppTest.suite());
        suite.addTest(org.cablelabs.impl.manager.application.XletAppContextTest.suite());

        // manager/download
        suite.addTest(org.cablelabs.impl.manager.download.BaseAppDownloadMgrTest.suite());

        // manager/eas
        // suite.addTest(org.cablelabs.impl.manager.eas.EASParserTest.suite());
        // suite.addTest(org.cablelabs.impl.manager.eas.TextScrollTest.suite());

        // manager/event
        suite.addTest(org.cablelabs.impl.manager.event.EventMgrTest.suite());

        // manager/filesys
        suite.addTest(org.cablelabs.impl.manager.filesys.AuthFileSysTest.suite());
        suite.addTest(org.cablelabs.impl.manager.filesys.AuthOpenFileTest.suite());
        suite.addTest(org.cablelabs.impl.manager.filesys.CachedFileSysTest.suite());
        suite.addTest(org.cablelabs.impl.manager.filesys.CachedOpenFileTest.suite());
        suite.addTest(org.cablelabs.impl.manager.filesys.FileManagerImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.filesys.NonAuthOpenFileTest.suite());

        // manager/reclaimation
        suite.addTestSuite(org.cablelabs.impl.manager.reclaim.NonRecursiveWrapperTest.class);

        // manager/resource
        suite.addTest(org.cablelabs.impl.manager.resource.RezMgrTest.suite());

        // manager/service
        suite.addTest(org.cablelabs.impl.manager.service.AbstractServiceImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.CannedSIDatabaseTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.CannedServiceMgrTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.ServiceMgrImplTest.suite());
        suite.addTest(org.cablelabs.impl.manager.service.ServicesDatabaseImplTest.suite());

        // manager/signalling
        suite.addTest(org.cablelabs.impl.manager.signalling.AitParserTest.suite());
        suite.addTest(org.cablelabs.impl.manager.signalling.XaitParserTest.suite());
        suite.addTest(org.cablelabs.impl.manager.signalling.SignallingMgrTest.suite());

        // manager/system
        suite.addTest(org.cablelabs.impl.manager.system.SystemModuleTest.suite());

        // manager/timer
        suite.addTest(org.cablelabs.impl.manager.timer.TimerMgrJava2Test.suite());

        // manager/xml
        suite.addTest(org.cablelabs.impl.manager.xml.nano.XmlMgrImplTest.suite());

        // signal tests are done
        suite.addTest(org.cablelabs.test.SignalTestsDoneTest.suite());

        return suite;
    }

    public static void main(String[] args)
    {
        System.out.println("\n**********************************************************");
        System.out.println("* Starting Manager tests.");
        System.out.println("************************************************************");
        org.cablelabs.test.textui.TestRunner.run(suite());
        System.out.println("\n**********************************************************");
        System.out.println("* Finishing Manager tests.");
        System.out.println("************************************************************");
        System.exit(0);
    }
}
