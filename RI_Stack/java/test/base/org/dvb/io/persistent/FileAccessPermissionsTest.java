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

package org.dvb.io.persistent;

import junit.framework.*;

/**
 * Tests the org.dvb.dsmcc.io.persistent.FileAccessPermissions class.
 */
public class FileAccessPermissionsTest extends TestCase
{
    private final boolean PERM_WORLD_READ = false;

    private final boolean PERM_WORLD_WRITE = false;

    private final boolean PERM_ORG_READ = true;

    private final boolean PERM_ORG_WRITE = false;

    private final boolean PERM_APP_READ = true;

    private final boolean PERM_APP_WRITE = true;

    public FileAccessPermissionsTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite(FileAccessPermissionsTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testConstructor()
    {
        FileAccessPermissions fperm = new FileAccessPermissions(PERM_WORLD_READ, PERM_WORLD_WRITE, PERM_ORG_READ,
                PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE);
        assertNotNull("FileAccessPermissions object wasn't instantiated", fperm);
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_WORLD_READ,
                fperm.hasReadWorldAccessRight());
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_WORLD_WRITE,
                fperm.hasWriteWorldAccessRight());
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_ORG_READ,
                fperm.hasReadOrganisationAccessRight());
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_ORG_WRITE,
                fperm.hasWriteOrganisationAccessRight());
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_APP_READ,
                fperm.hasReadApplicationAccessRight());
        assertEquals("FileAccessPermissions object's world-read permissions incorrect", PERM_APP_WRITE,
                fperm.hasWriteApplicationAccessRight());
    }

    public void testEquals()
    {
        FileAccessPermissions fperm1 = new FileAccessPermissions(PERM_WORLD_READ, PERM_WORLD_WRITE, PERM_ORG_READ,
                PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE);
        FileAccessPermissions fperm2 = new FileAccessPermissions(PERM_WORLD_READ, PERM_WORLD_WRITE, PERM_ORG_READ,
                PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE);
        FileAccessPermissions fperm3 = fperm1;

        assertTrue("FileAccessPermissions should be equal to itself", fperm1.equals(fperm1));
        assertTrue("FileAccessPermissions should be equal", fperm1.equals(fperm3));
        assertFalse("FileAccessPermissions should not be equal", fperm1.equals(fperm2));
        assertFalse("FileAccessPermissions should not be equal to null", fperm1.equals(null));
    }

    public void testSet()
    {

        FileAccessPermissions fperm = new FileAccessPermissions(PERM_WORLD_READ, PERM_WORLD_WRITE, PERM_ORG_READ,
                PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE);

        fperm.setPermissions(false, false, false, false, false, false);
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadWorldAccessRight());
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteWorldAccessRight());
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadOrganisationAccessRight());
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteOrganisationAccessRight());
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadApplicationAccessRight());
        assertFalse("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteApplicationAccessRight());

        fperm.setPermissions(true, true, true, true, true, true);
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadWorldAccessRight());
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteWorldAccessRight());
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadOrganisationAccessRight());
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteOrganisationAccessRight());
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasReadApplicationAccessRight());
        assertTrue("FileAccessPermissions object's world-read permissions not set to false",
                fperm.hasWriteApplicationAccessRight());
    }

}
