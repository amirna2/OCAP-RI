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

package org.cablelabs.impl.manager.recording;

import java.security.Permission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.shared.dvr.RecordingPermission;

import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

/**
 * Tests org.cablelabs.impl.manager.recording.RecordedServiceImpl
 * 
 * @author Arlis Dodson
 */
public class RecordedServiceImplTest extends TestCase
{
    private DummySecurityManager m_sm;

    /**
     * Tests that delete() invokes SecurityManager.checkPermission() with
     * RecordingPermission("delete","own").
     */
    public void testDeleteRecPerm()
    {
        RecordedServiceImplMock svc = new RecordedServiceImplMock();
        assertNotNull("Failed to retrieve RecordedServiceImplMock Object ref", svc);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("delete", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordedServiceImpl.delete() ...");
            svc.delete();
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by delete()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
    }

    /**
     * Tests that setMediaTime() invokes SecurityManager.checkPermission() with
     * RecordingPermission("modify","own").
     */
    public void testSetMediaTimeRecPerm()
    {
        RecordedServiceImplMock svc = new RecordedServiceImplMock();
        assertNotNull("Failed to retrieve RecordedServiceImplMock Object ref", svc);
        try
        {
            m_sm.setCheckFlag(true);
            m_sm.setPermToCheck(new RecordingPermission("modify", "own"));
            m_sm.setInvokedFlag(false);
            System.out.println("Invoking RecordedServiceImpl.setMediaTime() ...");
            javax.media.Time mediaTime = null; // okay for this test
            svc.setMediaTime(mediaTime);
        }
        catch (Exception e)
        {
        }
        finally
        {
            assertTrue("SecurityManager.checkPermission() not invoked by setMediaTime()", m_sm.getInvokedFlag());
            m_sm.setCheckFlag(false);
            m_sm.setInvokedFlag(false);
        }
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(RecordedServiceImplTest.class);
        return suite;
    }

    public RecordedServiceImplTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        m_sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(m_sm);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        ProxySecurityManager.pop();
    }

    /*
     * Not sure this approach is kosher, but it seemed to meet my immediate
     * need, which was simply to confirm that (a)
     * SecurityManager.checkPermission() is invoked (b) the correct Permission
     * is passed to checkPermission()
     */
    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        private Permission m_permToCheck;

        private boolean m_checkIt = false;

        private boolean m_invoked = false;

        public void checkPermission(Permission p)
        {
            m_invoked = false;
            if (m_checkIt)
            {
                // Using a kludge to handle sequential calls to
                // checkPermission() ...
                m_invoked = true;
                System.out.println("RecordedServiceImplTest.DummySecurityManager.checkPermission: " + p.toString());
                System.out.println("\tCompare to: " + m_permToCheck.toString());
                assertTrue("Permission to be checked does not match reference Permission", p.equals(m_permToCheck)
                        || p.implies(m_permToCheck));
            }
        }

        public void setPermToCheck(Permission p)
        {
            m_permToCheck = p;
        }

        public void setCheckFlag(boolean flag)
        {
            m_checkIt = flag;
        }

        public boolean getCheckFlag()
        {
            return m_checkIt;
        }

        public boolean getInvokedFlag()
        {
            return m_invoked;
        }

        public void setInvokedFlag(boolean flag)
        {
            m_invoked = flag;
        }
    }

    public class RecordedServiceImplMock extends org.cablelabs.impl.manager.recording.RecordedServiceImpl
    {

        public RecordedServiceImplMock()
        {
            super(new CannedRecordingImpl(), new Object());
        }

    }

    static class CannedRecordingImpl extends RecordingImpl
    {

        public int getId()
        {
            return 0;
        }

    }
}
