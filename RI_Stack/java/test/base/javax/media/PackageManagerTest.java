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
package javax.media;

import java.util.Vector;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.sound.mpe.CannedCallerContext;

public class PackageManagerTest extends TestCase
{
    private CallerContextManager save;

    private CCMgr ccmgr;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(PackageManagerTest.class);
        System.exit(0);
    }

    public static TestSuite suite()
    {
        return new TestSuite(PackageManagerTest.class);
    }

    public void setUp()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));
    }

    public void tearDown()
    {
        if (save != null)
        {
            ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
        }
    }

    /**
     * Tests that the results of calls to setContentProtocolPrefix are stored in
     * a context specific manner
     */
    public void testSetGetContentPrefixContextIsolated() throws Exception
    {
        CannedCallerContext cc1 = new CannedCallerContext();
        CannedCallerContext cc2 = new CannedCallerContext();
        final String testContentString = "xyz.xyz.xyz";
        //
        // add to the content prefix list in one caller context
        //
        cc1.runInContextSync(new Runnable()
        {
            public void run()
            {
                Vector contentPrefix = PackageManager.getContentPrefixList();
                contentPrefix.add(testContentString);
                PackageManager.setContentPrefixList(contentPrefix);
            }
        });
        //
        // verify that it doesn't exist in the second context
        //
        cc2.runInContextSync(new Runnable()
        {
            public void run()
            {
                Vector contentPrefix = PackageManager.getContentPrefixList();
                for (int i = 0; i < contentPrefix.size(); i++)
                {
                    assertTrue("ContentPrefix not isolated across contexts", !contentPrefix.get(i).equals(
                            testContentString));
                }
            }
        });

        //
        // double check that it actually was set in the first context
        //
        cc1.runInContextSync(new Runnable()
        {
            public void run()
            {
                boolean found = false;
                Vector contentPrefix = PackageManager.getContentPrefixList();
                for (int i = 0; i < contentPrefix.size(); i++)
                {
                    if (contentPrefix.get(i).equals(testContentString))
                    {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }
        });
    }

    /**
     * Tests that the results of calls to setPackageProtocolPrefix are stored in
     * a context specific manner
     */
    public void testSetGetProtocolPrefixContextIsolated() throws Exception
    {
        CannedCallerContext cc1 = new CannedCallerContext();
        CannedCallerContext cc2 = new CannedCallerContext();
        final String testContentString = "abc.abc.abc";
        //
        // add to the content prefix list in one caller context
        //
        cc1.runInContextSync(new Runnable()
        {
            public void run()
            {
                Vector protocolPrefix = PackageManager.getProtocolPrefixList();
                protocolPrefix.add(testContentString);
                PackageManager.setProtocolPrefixList(protocolPrefix);
            }
        });
        //
        // verify that it doesn't exist in the second context
        //
        cc2.runInContextSync(new Runnable()
        {
            public void run()
            {
                Vector protocolPrefix = PackageManager.getProtocolPrefixList();
                for (int i = 0; i < protocolPrefix.size(); i++)
                {
                    assertTrue("ContentPrefix not isolated across contexts", !protocolPrefix.get(i).equals(
                            testContentString));
                }
            }
        });

        //
        // double check that it actually was set in the first context
        //
        cc1.runInContextSync(new Runnable()
        {
            public void run()
            {
                boolean found = false;
                Vector protocolPrefix = PackageManager.getProtocolPrefixList();
                for (int i = 0; i < protocolPrefix.size(); i++)
                {
                    if (protocolPrefix.get(i).equals(testContentString))
                    {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }
        });
    }
}
