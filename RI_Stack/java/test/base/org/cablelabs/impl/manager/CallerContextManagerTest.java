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

package org.cablelabs.impl.manager;

import junit.framework.*;
import org.dvb.application.*;
import org.ocap.application.*;
import javax.tv.xlet.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests the CallerContextManager interface.
 * 
 * @author Aaron Kamienski
 */
public class CallerContextManagerTest extends ManagerTest
{
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(CallerContextManagerTest.class);
        suite.setName(CallerContextManager.class.getName());
        return suite;
    }

    public CallerContextManagerTest(String name, ImplFactory f)
    {
        super(name, CallerContextManager.class, f);
    }

    protected CallerContextManager createCallerContextManager()
    {
        return (CallerContextManager) createManager();
    }

    private CallerContextManager ctxmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        ctxmgr = (CallerContextManager) mgr;
    }

    protected void tearDown() throws Exception
    {
        ctxmgr = null;
        super.tearDown();
    }

    /**
     * Tests getCurrentContext.
     */
    public void testGetCurrentContext() throws Exception
    {
        CallerContext ctx = ctxmgr.getCurrentContext();

        assertNotNull("The value returned by getCurrentContext should not be null", ctx);

        assertSame("The CallerContext returned on successive calls should be the same", ctx, ctxmgr.getCurrentContext());

        // Make sure when run on current context, that is current
        final CallerContext curr[] = new CallerContext[1];
        ctx.runInContextSync(new Runnable()
        {
            public void run()
            {
                curr[0] = ctxmgr.getCurrentContext();
            }
        });
        assertSame("The current context should be the system context", ctx, curr[0]);
    }

    /**
     * Tests getCurrentContext.
     */
    public void testGetSystemContext() throws Exception
    {
        CallerContext ctx = ctxmgr.getSystemContext();

        assertNotNull("The value returned by getSystemContext should not be null", ctx);

        assertSame("The CallerContext returned on successive calls should be the same", ctx, ctxmgr.getSystemContext());

        // Make sure when run on system context, that is current
        final CallerContext curr[] = new CallerContext[1];
        ctx.runInContextSync(new Runnable()
        {
            public void run()
            {
                curr[0] = ctxmgr.getCurrentContext();
            }
        });
        assertSame("The current context should be the system context", ctx, curr[0]);
    }

    /**
     * Tests destroy(). Overridden to make sure that other calls fail somehow.
     */
    /*
     * public void testDestroy() throws Exception { super.testDestroy();
     * 
     * // getCurrentContext try { CallerContext ctx =
     * ctxmgr.getCurrentContext(); if (ctx != null)
     * fail("getCurrentContext should fail if manager is destroyed"); else
     * assertNull("getCurrentContext should fail if manager is destroyed",
     * proxy); } catch(Exception e) {}
     * 
     * // getSystemContext try { CallerContext ctx = ctxmgr.getSystemContext();
     * if (ctx != null)
     * fail("getSystemContext should fail if manager is destroyed"); else
     * assertNull("getSystemContext should fail if manager is destroyed",
     * proxy); } catch(Exception e) {} }
     */
}
