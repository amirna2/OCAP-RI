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
import java.io.*;
import java.util.Date;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Tests the org.dvb.io.persistent.FileAttributesTest class.
 */
public class FileAttributesTest extends TestCase
{
    private final boolean PERM_WORLD_READ = false;

    private final boolean PERM_WORLD_WRITE = false;

    private final boolean PERM_ORG_READ = true;

    private final boolean PERM_ORG_WRITE = false;

    private final boolean PERM_APP_READ = true;

    private final boolean PERM_APP_WRITE = true;

    private String TestDir = "/persistent/";

    private String TestFile1 = "TestPersistentStats";

    private String TestFileDelete = "TestPersistentStats-delete";

    private String TestFileRenameBefore = "Test.before";

    private String TestFileRenameAfter = "Test.after";

    private String TestFileRetVal = "RetValFile";

    private final FileAccessPermissions fperms = new FileAccessPermissions(PERM_WORLD_READ, PERM_WORLD_WRITE,
            PERM_ORG_READ, PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE);

    private final FileAccessPermissions fperms_default = new FileAccessPermissions(false, false, false, false, true,
            true);

    int[] readAccessOrgIDs = { 2850, 1695, 2239, 4872, 2111 };

    int[] writeAccessOrgIDs = { 2850, 2790, 4200, 3001 };

    private final ExtendedFileAccessPermissions extPerms = new ExtendedFileAccessPermissions(PERM_WORLD_READ,
            PERM_WORLD_WRITE, PERM_ORG_READ, PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE, readAccessOrgIDs,
            writeAccessOrgIDs);

    int[] emptyReadAccessOrgIDs = {};

    int[] emptyWriteAccessOrgIDs = {};

    private final ExtendedFileAccessPermissions emptyExtPerms = new ExtendedFileAccessPermissions(PERM_WORLD_READ,
            PERM_WORLD_WRITE, PERM_ORG_READ, PERM_ORG_WRITE, PERM_APP_READ, PERM_APP_WRITE, emptyReadAccessOrgIDs,
            emptyWriteAccessOrgIDs);

    // These are the default attributes as defined in Annex K of MHP (GEM)
    private final FileAttributes defaultAttribs = new FileAttributes(null, fperms_default, FileAttributes.PRIORITY_LOW);

    private final Date time1 = new Date();

    private final Date time2 = new Date(1000000);

    public FileAttributesTest(String name)
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
        TestSuite suite = new TestSuite(FileAttributesTest.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        String persistentRoot = MPEEnv.getSystemProperty("dvb.persistent.root");

        if (persistentRoot == null)
        {
            persistentRoot = ".";
        }

        // Create test directory first
        File dir = new File(persistentRoot + TestDir);
        dir.mkdir();

        // Creat test filenames
        TestFile1 = persistentRoot + TestDir + TestFile1;
        TestFileDelete = persistentRoot + TestDir + TestFileDelete;
        TestFileRenameBefore = persistentRoot + TestDir + TestFileRenameBefore;
        TestFileRenameAfter = persistentRoot + TestDir + TestFileRenameAfter;
        TestFileRetVal = persistentRoot + TestDir + TestFileRetVal;

        PrintWriter out = new PrintWriter(new FileOutputStream(TestFile1));
        out.print("test of org.dvb.io.persistent");
        out.close();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();

        // Cleanup the files
        new File(TestFile1).delete();
        new File(TestFileDelete).delete();
        new File(TestFileRenameBefore).delete();
        new File(TestFileRenameAfter).delete();
        new File(TestFileRetVal).delete();
    }

    public void testConstructor()
    {
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        assertNotNull("FileAttributes object wasn't instantiated", fattribs);
        assertTrue("FileAttributes object instansiation didn't set proper date", Math.abs(time1.getTime()
                - fattribs.getExpirationDate().getTime()) < 1000);
        assertTrue("FileAttributes object instantiation didn't set proper permissions", fileAccessPermissionsAreEqual(
                fperms, fattribs.getPermissions()));
        assertEquals("FileAttributes object instantiation didn't set proper priority", FileAttributes.PRIORITY_MEDIUM,
                fattribs.getPriority());
    }

    public void testtestEquals()
    {
        FileAttributes fattribs1 = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes fattribs2 = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes fattribs3 = fattribs1;

        assertTrue("FileAttributes object should be equal to itself", fattribs1.equals(fattribs1));
        assertTrue("FileAttributes object should be equal", fattribs1.equals(fattribs3));
        assertFalse("FileAttributes object should not be equal", fattribs1.equals(fattribs2));
        assertFalse("FileAttributes object should not be equal to null", fattribs1.equals(null));
    }

    public void testSets()
    {
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);

        assertTrue("FileAttributes object instansiation didn't set proper date", Math.abs(time1.getTime()
                - fattribs.getExpirationDate().getTime()) < 1000);
        assertTrue("FileAttributes object instansiation didn't set proper permissions", fileAccessPermissionsAreEqual(
                fperms, fattribs.getPermissions()));
        assertEquals("FileAttributes object instansiation didn't set proper priority", FileAttributes.PRIORITY_MEDIUM,
                fattribs.getPriority());

        fattribs.setExpirationDate(time2);
        assertTrue("FileAttributes.setExpirationDate() didn't set proper date", Math.abs(time2.getTime()
                - fattribs.getExpirationDate().getTime()) < 1000);

        FileAccessPermissions fpermsNew = new FileAccessPermissions(true, false, true, false, true, false);
        fattribs.setPermissions(fpermsNew);
        assertTrue("FileAttributes.setPermissions() didn't set proper permissions", fileAccessPermissionsAreEqual(
                fpermsNew, fattribs.getPermissions()));

        fattribs.setPriority(FileAttributes.PRIORITY_HIGH);
        assertEquals("FileAttributes.setPriority() didn't set proper priority", FileAttributes.PRIORITY_HIGH,
                fattribs.getPriority());
    }

    public void testStaticSetNGet()
    {
        File f = new File(TestFile1);

        /*
         * Test Standard File Attributes
         */
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);

        try
        {
            FileAttributes.setFileAttributes(fattribs, f);
        }
        catch (Exception e)
        {
            fail("exception during setFileAttributes()");
            return;
        }

        FileAttributes fattribsCheck = null;
        try
        {
            fattribsCheck = FileAttributes.getFileAttributes(f);
        }
        catch (Exception e)
        {
            fail("exception during getFileAttributes()");
            return;
        }
        assertTrue("FileAttributes.getFileAttributes() didn't get proper attributes that were set",
                fileAttributesAreEqual(fattribs, fattribsCheck));

        /*
         * Test File Attributes w/ ExtendedFileAccessPermissions
         */
        FileAttributes extAttribs = new FileAttributes(time1, extPerms, FileAttributes.PRIORITY_HIGH);

        try
        {
            FileAttributes.setFileAttributes(extAttribs, f);
        }
        catch (Exception e)
        {
            fail("exception during setFileAttributes()");
            return;
        }
        fattribsCheck = null;
        try
        {
            fattribsCheck = FileAttributes.getFileAttributes(f);
        }
        catch (Exception e)
        {
            fail("exception during getFileAttributes()");
            return;
        }
        assertTrue("FileAttributes.getFileAttributes() didn't get proper extended attributes that were set",
                fileAttributesAreEqual(extAttribs, fattribsCheck));
    }

    public void testReturnValue() throws Exception
    {
        File f1 = new File(TestFileRetVal);
        try
        {
            FileWriter fw = new FileWriter(f1);
            fw.close();
        }
        catch (Exception e)
        {
        }
        FileAttributes fattribsCheck = null;

        // create some standard attributes for this file
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes.setFileAttributes(fattribs, f1);

        // Ensure that this does not return ExtendedFileAccessPermissions
        fattribsCheck = FileAttributes.getFileAttributes(f1);
        assertFalse("getFileAttributes should not have returned an instance of ExtendedFileAccessPermissions",
                fattribsCheck.getPermissions() instanceof ExtendedFileAccessPermissions);

        // Now set ExtendedFileAccessPermissions and ensure that a proper
        // instance is returned
        fattribs.setPermissions(extPerms);
        FileAttributes.setFileAttributes(fattribs, f1);

        // Ensure that this does return ExtendedFileAccessPermissions
        fattribsCheck = FileAttributes.getFileAttributes(f1);
        assertTrue("getFileAttributes should not have returned an instance of ExtendedFileAccessPermissions",
                fattribsCheck.getPermissions() instanceof ExtendedFileAccessPermissions);
    }

    /*
     * Even if a file is set with extended file access permissions that contain
     * empty arrays of org read/write access IDs, querying that same file's
     * attributes should still return an instance of
     * ExtendedFileAccessPermissions
     */
    public void testEmptyExtendedAccess() throws Exception
    {
        // create new file and make sure any previous file with that filename is
        // deleted
        File f1 = new File(TestFile1);
        f1.delete();

        // Create the file on-disk
        try
        {
            FileWriter fw = new FileWriter(f1);
            fw.close();
        }
        catch (Exception e)
        {
        }

        // Set extended file attributes with empty arrays for read/write access
        // org ids
        FileAttributes fattribs = new FileAttributes(time1, emptyExtPerms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes.setFileAttributes(fattribs, f1);

        FileAttributes fattribsCheck = FileAttributes.getFileAttributes(f1);
        assertTrue("ExtendedFileAccessPermissions instance should have been returned",
                fattribsCheck.getPermissions() instanceof ExtendedFileAccessPermissions);
        assertTrue("File attributes don't match", fileAttributesAreEqual(fattribs, fattribsCheck));
    }

    public void testFileDelete() throws Exception
    {
        // create new file
        File f1 = new File(TestFileDelete);
        try
        {
            FileWriter fw = new FileWriter(f1);
            fw.close();
        }
        catch (Exception e)
        {
        }

        // create some attributes for this file
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes.setFileAttributes(fattribs, f1);
        FileAttributes fattribsCheck = FileAttributes.getFileAttributes(f1);
        assertTrue("file attributes weren't set correctly", fileAttributesAreEqual(fattribs, fattribsCheck));

        // delete this file
        f1.delete();

        // insure that the attributes cannot be accessed
        boolean gotException = false;
        try
        {
            fattribsCheck = FileAttributes.getFileAttributes(f1);
        }
        catch (Exception e)
        {
            // should have gotten an exception here
            gotException = true;
        }
        assertTrue("shouldn't be able to access attributes of deleted file", gotException);

        // create a new file with the same name and ensure that default
        // attributes exist
        File f2 = new File(TestFileDelete);
        f2.delete();
        try
        {
            FileWriter fw = new FileWriter(f2);
            fw.close();
        }
        catch (Exception e)
        {
        }

        // insure that the attributes for the first file have been deleted
        try
        {
            fattribsCheck = FileAttributes.getFileAttributes(f2);
        }
        catch (Exception e)
        {
            fail("Should be able to access default attributes");
        }
        assertTrue("File should have default attributes", fileAttributesAreEqual(defaultAttribs, fattribsCheck));

        // cleanup: delete this file
        f2.delete();
    }

    public void testFileRename() throws Exception
    {
        // create new file
        File f1 = new File(TestFileRenameBefore);
        File f2 = new File(TestFileRenameAfter);
        try
        {
            FileWriter fw1 = new FileWriter(f1);
            fw1.close();
        }
        catch (Exception e)
        {
        }

        // create some attributes for this file
        FileAttributes fattribs = new FileAttributes(time1, fperms, FileAttributes.PRIORITY_MEDIUM);
        FileAttributes.setFileAttributes(fattribs, f1);
        FileAttributes fattribsCheck = FileAttributes.getFileAttributes(f1);
        assertTrue("file attributes weren't set correctly", fileAttributesAreEqual(fattribs, fattribsCheck));

        // rename this file
        f1.renameTo(f2);

        // insure that the attributes cannot be accessed using the old name
        boolean gotException = false;
        try
        {
            fattribsCheck = FileAttributes.getFileAttributes(f1);
        }
        catch (Exception e)
        {
            // should have gotten an exception here
            gotException = true;
        }
        assertTrue("shouldn't be able to access attributes of renamed file using old filename", gotException);

        // insure that the attributes can be accessed using the new name
        fattribsCheck = FileAttributes.getFileAttributes(f2);
        assertTrue("cannot access file attributes using new name", fileAttributesAreEqual(fattribs, fattribsCheck));

        // cleanup: delete this file
        f2.delete();
        f1.delete();
    }

    public void testFileRenameNoAttributes() throws Exception
    {
        // create new file
        File f1 = new File(TestFileRenameBefore);
        File f2 = new File(TestFileRenameAfter);
        try
        {
            FileWriter fw1 = new FileWriter(f1);
            fw1.close();
        }
        catch (Exception e)
        {
        }

        // rename this file
        boolean result = f1.renameTo(f2);

        assertTrue("File rename with no attributes should have succeeded", result);

        // cleanup: delete this file
        f2.delete();
        f1.delete();
    }

    private boolean fileAccessPermissionsAreEqual(FileAccessPermissions fp1, FileAccessPermissions fp2)
    {
        boolean fp1IsInstance = fp1 instanceof ExtendedFileAccessPermissions;
        boolean fp2IsInstance = fp2 instanceof ExtendedFileAccessPermissions;

        /*
         * All of this hacky checking is due to the fact that the FileAttributes
         * interface only refers to FileAccessPermissions even though OCAP
         * extends to ExtendedFileAccessPermissions. So, the FileAttributes
         * permissions member could actually be pointing to either one.
         */
        if (fp1IsInstance)
        {
            ExtendedFileAccessPermissions efap1 = (ExtendedFileAccessPermissions) fp1;

            if (fp2IsInstance)
            {
                ExtendedFileAccessPermissions efap2 = (ExtendedFileAccessPermissions) fp2;

                if (efap1.getReadAccessOrganizationIds().length != efap2.getReadAccessOrganizationIds().length)
                {
                    return false;
                }
                if (efap1.getWriteAccessOrganizationIds().length != efap2.getWriteAccessOrganizationIds().length)
                {
                    return false;
                }
            }
            else
            {
                if (efap1.getReadAccessOrganizationIds().length != 0
                        || efap1.getWriteAccessOrganizationIds().length != 0)
                {
                    return false;
                }
            }
        }
        else if (fp2IsInstance)
        {
            ExtendedFileAccessPermissions efap2 = (ExtendedFileAccessPermissions) fp2;

            if (efap2.getReadAccessOrganizationIds().length != 0 || efap2.getWriteAccessOrganizationIds().length != 0)
            {
                return false;
            }
        }

        // Now we just need to attempt to compare the contents of the org ID
        // arrays.
        // We got through the above code, so we know that if they both are
        // instances of
        // ExtendedFileAccessPermissions, their org ID arrays are equal length.
        if (fp1IsInstance && fp2IsInstance)
        {
            ExtendedFileAccessPermissions efap1 = (ExtendedFileAccessPermissions) fp1;
            ExtendedFileAccessPermissions efap2 = (ExtendedFileAccessPermissions) fp2;

            // -- Read Access Org IDs --
            // For each item in the first array, search for its match in the
            // second
            // array. If a match is not found then the two FileAttribute classes
            // are
            // not equivalent
            int[] readAccess1 = efap1.getReadAccessOrganizationIds();
            int[] readAccess2 = efap2.getReadAccessOrganizationIds();
            for (int i = 0; i < readAccess1.length; ++i)
            {
                boolean found = false;
                for (int j = 0; j < readAccess2.length; ++j)
                {
                    if (readAccess1[i] == readAccess2[j])
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    return false;
                }
            }

            // -- Write Access Org IDs --
            // For each item in the first array, search for its match in the
            // second
            // array. If a match is not found then the two FileAttribute classes
            // are
            // not equivalent
            int[] writeAccess1 = efap1.getWriteAccessOrganizationIds();
            int[] writeAccess2 = efap2.getWriteAccessOrganizationIds();
            for (int i = 0; i < writeAccess1.length; ++i)
            {
                boolean found = false;
                for (int j = 0; j < writeAccess2.length; ++j)
                {
                    if (writeAccess1[i] == writeAccess2[j])
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    return false;
                }
            }
        }

        if (fp1.hasReadApplicationAccessRight() == fp2.hasReadApplicationAccessRight()
                && fp1.hasReadOrganisationAccessRight() == fp2.hasReadOrganisationAccessRight()
                && fp1.hasReadWorldAccessRight() == fp2.hasReadWorldAccessRight()
                && fp1.hasWriteApplicationAccessRight() == fp2.hasWriteApplicationAccessRight()
                && fp1.hasWriteOrganisationAccessRight() == fp2.hasWriteOrganisationAccessRight()
                && fp1.hasWriteWorldAccessRight() == fp2.hasWriteWorldAccessRight())
        {
            return true;
        }

        return false;
    }

    private boolean fileAttributesAreEqual(FileAttributes fa1, FileAttributes fa2)
    {
        // Priorities and permissions should be exactly equal, expiration dates
        // should be within 1000ms due to potential rounding errors
        FileAccessPermissions fp1 = fa1.getPermissions();
        FileAccessPermissions fp2 = fa2.getPermissions();

        if ((fa1.getExpirationDate() == null && fa2.getExpirationDate() != null)
                || (fa2.getExpirationDate() == null && fa1.getExpirationDate() != null))
        {
            return false;
        }

        if (fa1.getPriority() == fa2.getPriority()
                && fileAccessPermissionsAreEqual(fa1.getPermissions(), fa2.getPermissions())
                && ((fa1.getExpirationDate() == null && fa2.getExpirationDate() == null) || (Math.abs(fa1.getExpirationDate()
                        .getTime()
                        - fa2.getExpirationDate().getTime()) < 1000)))
        {
            return true;
        }

        return false;
    }

}
