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

package org.dvb.application;

import junit.framework.*;

/**
 * Tests AppID.
 * 
 * @author Aaron Kamienski
 */
public class AppIDTest extends TestCase
{
    /**
     * Tests the contructor. Note that there is NO range checking on the
     * numbers. I.e., no check for zero org_id or app_id or out of range app_id.
     * 
     * <ol>
     * <li>0x0000...0x3fff - unsigned apps
     * <li>0x4000...0x7fff - signed apps
     * <li>0x8000...0xfffd - resvd
     * <li>0xfffe - special wildcard for signed apps of an org
     * <li>0xffff - special wildcard for all apps of an org
     * </ol>
     */
    public void testAppID()
    {
        for (int i = 0; i < oids.length; ++i)
            for (int j = 0; j < aids.length; ++j)
            {
                AppID appid = new AppID(oids[i], aids[j]);

                assertEquals("The Organization ID should be as set in constructor", appid.getOID(), oids[i]);

                assertEquals("The Application ID should be as set in constructor", appid.getAID(), aids[j]);
            }
    }

    /**
     * Tests toString().
     */
    public void testToString() throws Exception
    {
        for (int i = 0; i < oids.length; ++i)
            for (int j = 0; j < aids.length; ++j)
            {
                AppID appid = new AppID(oids[i], aids[j]);

                long id = (((long) oids[i]) << 16 | aids[j]) & 0xFFFFFFFFFFFFL;
                assertEquals("AppID.toString should return 48-bit hex representation with "
                        + "OID in most-significant 32 bits", Long.toHexString(id), appid.toString());
            }
    }

    /**
     * Tests equals().
     */
    public void testEquals() throws Exception
    {
        AppID id = new AppID(100, 200);

        assertTrue("An AppID should be equal to itself", id.equals(id));
        assertTrue("An AppID should be equal to an equivalent AppID", id.equals(new AppID(id.getOID(), id.getAID())));
        assertFalse("An AppID should NOT be equal to other AppIDs", id.equals(new AppID(id.getOID() + 1, id.getAID())));
        assertFalse("An AppID should NOT be equal to other AppIDs", id.equals(new AppID(id.getOID(), id.getAID() + 1)));
    }

    /**
     * Tests hashCode().
     */
    public void testHashCode() throws Exception
    {
        AppID id = new AppID(100, 200);

        assertEquals("AppID.hashCode should return the same value for successive calls", id.hashCode(), id.hashCode());
        assertEquals("AppID.hashCode should return the same for equivalent appIDs", id.hashCode(), (new AppID(
                id.getOID(), id.getAID())).hashCode());
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
        TestSuite suite = new TestSuite(AppIDTest.class);
        return suite;
    }

    public AppIDTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    static final int[] oids = { 0, 1, 10, 0xff, 0x100, 0xffff, 0x10000, 0xffffff, 0x1000000, 0x7fffffff, 0x80000000,
            0xffffffff, 0xaaaa5555 };

    static final int[] aids = { 0, 1, 10, 0xff, 0x100, 0x3fff, 0x4000, 0x7fff, 0x8000, 0xfffd, 0xfffe, 0xffff };
}
