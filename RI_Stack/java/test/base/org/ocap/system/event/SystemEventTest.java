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

package org.ocap.system.event;

import org.dvb.application.AppProxyTest.DummySecurityManager;

import junit.framework.*;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

/**
 * Tests SystemEvent.
 * 
 * @author Aaron Kamienski
 */
public class SystemEventTest extends TestCase
{
    private static final String[] fieldNames = { "BEGIN_SYS_INFO_EVENT_TYPES", "BEGIN_SYS_INFO_RESERVED_EVENT_TYPES",
            "END_SYS_INFO_EVENT_TYPES", "BEGIN_APP_INFO_EVENT_TYPES", "BEGIN_APP_INFO_RESERVED_EVENT_TYPES",
            "END_APP_INFO_EVENT_TYPES", "BEGIN_SYS_REC_ERROR_EVENT_TYPES", "BEGIN_SYS_REC_ERROR_RESERVED_EVENT_TYPES",
            "END_SYS_REC_ERROR_EVENT_TYPES", "BEGIN_APP_REC_ERROR_EVENT_TYPES",
            "BEGIN_APP_REC_ERROR_RESERVED_EVENT_TYPES", "END_APP_REC_ERROR_EVENT_TYPES",
            "BEGIN_SYS_CAT_ERROR_EVENT_TYPES", "BEGIN_SYS_CAT_ERROR_RESERVED_EVENT_TYPES",
            "END_SYS_CAT_ERROR_EVENT_TYPES", "BEGIN_APP_CAT_ERROR_EVENT_TYPES",
            "BEGIN_APP_CAT_ERROR_RESERVED_EVENT_TYPES", "END_APP_CAT_ERROR_EVENT_TYPES",
            "BEGIN_SYS_REBOOT_EVENT_TYPES", "BEGIN_SYS_REBOOT_RESERVED_EVENT_TYPES", "END_SYS_REBOOT_EVENT_TYPES",
            "BEGIN_SYS_RES_DEP_EVENT_TYPES", "BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES", "END_SYS_RES_DEP_EVENT_TYPES", };

    private static final int[] fieldValues = { 0x00000000, 0x04000000, 0x07FFFFFF, 0x08000000, 0x0C000000, 0x1FFFFFFF,
            0x20000000, 0x24000000, 0x27FFFFFF, 0x28000000, 0x2C000000, 0x2FFFFFFF, 0x30000000, 0x34000000, 0x37FFFFFF,
            0x38000000, 0x3C000000, 0x3FFFFFFF, 0x40000000, 0x44000000, 0x47FFFFFF, 0x50000000, 0x54000000, 0x57FFFFFF, };

    /**
     * Tests public fields.
     */
    public void testPublicFields()
    {
        TestUtils.testFieldValues(SystemEvent.class, fieldNames, fieldValues);
    }

    /**
     * Tests no public constructors.
     */
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(SystemEvent.class);
    }

    /**
     * Tests SystemEvent(int typeCode).
     * <ul>
     * <li>Verify specified typeCode is returned
     * <li>Verify default message.
     * <li>Verify date is within a range
     * <li>Verify AppID
     * </ul>
     */
    // public void testConstructor_typeCode()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests SystemEvent(int typeCode, String message).
     * <ul>
     * <li>Verify specified typeCode is returned
     * <li>Verify specified message is returned
     * <li>Verify date is within a range
     * <li>Verify AppID
     * </ul>
     */
    // public void testConstructor_typeCodeMessage()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests SystemEvent(int typeCode, String message, long date, AppID id).
     * <ul>
     * <li>Verify specified typeCode is returned
     * <li>Verify specified message is returned
     * <li>Verify specified date is returned
     * <li>Verify specified id is returned
     * </ul>
     */
    // public void testConstructor_typeCodeMessageDateId()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests illegal argument checks for constructors.
     */
    // public void testConstructors_illegal()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests property security checks for constructors.
     */
    public void testConstructors_security()
    {
        SystemEvent event;
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            event = new SystemEvent(SystemEvent.BEGIN_SYS_CAT_ERROR_EVENT_TYPES, "this is a test",
                    System.currentTimeMillis(), new org.dvb.application.AppID(1, 1));
            assertNotNull("SystemEvent(int, String, long, AppID) should check with SecurityManager", sm.p);
            assertTrue("constructor should check for AllPermission", sm.p instanceof java.security.AllPermission);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests getAppID().
     */
    // public void testGetAppID()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests getTypeCode().
     */
    // public void testGetTypeCode()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests getMessage().
     */
    // public void testGetMessage()
    // {
    // fail("Unimplemented test");
    // }

    /**
     * Tests getDate().
     */
    // public void testGetDate()
    // {
    // fail("Unimplemented test");
    // }

    /*  ***** Boilerplate ***** */
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
        TestSuite suite = new TestSuite(SystemEventTest.class);
        return suite;
    }

    public SystemEventTest(String name)
    {
        super(name);
    }
}
