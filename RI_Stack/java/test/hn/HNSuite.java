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

public class HNSuite extends TestSuite
{
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite("ocap.hn");
        
        // Each unit test should start with a "set up" that gets the control point and
        // controlled device in a known state. Look at UPnPServiceTest, UPnPDeviceTest,
        // and UPnPControlPointTest for examples. (That code should be factored out.)

        // Let's do the upnp-diag tests first ...
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientServiceTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientDeviceTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientStateVariableTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionTest.class);
//        suite.addTestSuite(org.ocap.hn.upnp.client.UPnPControlPointTest.class);
//        suite.addTestSuite(org.ocap.hn.upnp.common.CommonTest.class);
        //suite.addTestSuite(org.ocap.hn.upnp.server.UPnPDeviceManagerTest.class);
        //suite.addTestSuite(org.ocap.hn.upnp.server.UPnPManagedDeviceTest.class);
        //suite.addTestSuite(org.ocap.hn.upnp.server.UPnPManagedStateVariableTest.class);

        // ... then the non-upnp-diag tests.
//        suite.addTestSuite(org.cablelabs.impl.media.streaming.ContentRequestTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.NetModuleTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.ChannelContentItemTest.class);        
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.ContentContainerTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.HiddenContentItemTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.ContentEntryComparatorTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.ContentItemTest.class);
        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.MetadataNodeTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.content.TrackingChangesOptionTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.upnp.cm.CMSProtocolInfoTest.class);
//        suite.addTestSuite(org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParserTest.class);

        return suite;
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
}
