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

package org.cablelabs.xlet.FileAccessPermissionTest;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.dvb.io.persistent.FileAccessPermissions;
import org.dvb.io.persistent.FileAttributes;
import org.havi.ui.HScene;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.ocap.storage.ExtendedFileAccessPermissions;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

public class FileAccessTest implements Xlet, Driveable
{
    public static class Other4010 extends FileAccessTest
    {
    }

    public static class Other4011 extends FileAccessTest
    {
    }

    public static class Other4012 extends FileAccessTest
    {
    }

    public static class Other4013 extends FileAccessTest
    {
    }

    public static class Other4015 extends FileAccessTest
    {
    }

    public static class Other4016 extends FileAccessTest
    {
    }

    public void initXlet(XletContext ctx)
    {
        this.ctx = ctx;

        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();

        log.log("FileAccessTest - initXlet()");
        scene = (HScene) javax.tv.graphics.TVContainer.getRootContainer(ctx);
        scene.setLayout(new FlowLayout());
        scene.setLocation(0, 0);
        scene.setSize(640, 480);
        log.log("FileAccessTest - initXlet() done");
    }

    public void startXlet()
    {
        log.log("FileAccessTest - startXlet()");
        axc.clearTestResults(); // clear old test results

        parseArgs(); // run tests

        Color errColor = Color.orange.darker();
        Color okColor = Color.green.darker();

        HStaticText banner = new HStaticText("FileAccessPermission Test Xlet", 0, 40, 640, 35);
        banner.setBackground(Color.darkGray);
        banner.setForeground(Color.green.darker());
        banner.setBackgroundMode(HVisible.BACKGROUND_FILL);
        scene.add(banner);

        TestResult results = test.getTestResult();
        if (results.wasSuccessful())
        {
            HStaticText text = new HStaticText("Passed!", 200, 300, 100, 35);
            text.setBackground(Color.darkGray);
            text.setForeground(okColor);
            text.setBackgroundMode(HVisible.BACKGROUND_FILL);
            scene.add(text);
        }
        else
        {
            Enumeration e = results.failures();
            while (e.hasMoreElements())
            {
                TestFailure failure = (TestFailure) e.nextElement();
                HStaticText text = new HStaticText(failure.toString());
                text.setBackground(Color.darkGray);
                text.setBackground(errColor);
                text.setBackgroundMode(HVisible.BACKGROUND_FILL);
                scene.add(text);
            }
        }

        scene.validate();
        scene.show();

        log.log(test.getTestResult());

        log.log("FileAccessTest - startXlet() done");
    }

    public void pauseXlet()
    {
        log.log("FileAccessTest - pauseXlet()");
        scene.setVisible(false);
        scene.removeAll();
        log.log("FileAccessTest - pauseXlet() done");
    }

    public void destroyXlet(boolean uncond)
    {
        log.log("FileAccessTest - destroyXlet()");
        if (scene != null)
        {
            scene.setVisible(false);
            scene.removeAll();
            scene.dispose();
        }
        scene = null;
        log.log("FileAccessTest - destroyXlet() done");
    }

    private void parseArgs()
    {
        log.log("FileAccessTest - parseArgs()");
        parseArgs((String[]) ctx.getXletProperty("dvb.caller.parameters"));

        parseArgs((String[]) ctx.getXletProperty(XletContext.ARGS));

        log.log("FileAccessTest - parseArgs() done");
    }

    private void parseArgs(String[] args)
    {
        log.log("FileAccessTest - parseArgs(String[])");
        if (args == null) return;
        for (int i = 0; i < args.length; ++i)
        {
            if ("create".equals(args[i]))
            {
                create(args[++i]);
            }
            else if ("remove".equals(args[i]))
            {
                remove(args[++i]);
            }
            else if ("read".equals(args[i]))
            {
                read(args[++i]);
            }
            else if ("write".equals(args[i]))
            {
                write(args[++i]);
            }
            else if ("fap".equals(args[i]))
            {
                String fap = args[++i];
                String fileName = args[++i];

                fap(fap, fileName);

                if (fileName.endsWith("org1.txt"))
                {
                    String dirName = fileName.substring(0, fileName.lastIndexOf("org1.txt"));
                    fap(fap, dirName);
                }
            }
            else if ("xfap".equals(args[i]))
            {
                String xfap = args[++i];
                xfap(xfap, args[++i]);
            }
            else if ("ok".equals(args[i]))
            {
                expectSuccess = true;
            }
            else if ("fail".equals(args[i]))
            {
                expectSuccess = false;
            }
        }
        log.log("FileAccessTest - parseArgs(String[]) done");

        // success("SetPerms "+filename);
    }

    private String replace(String str, String key, String value)
    {
        int idx;

        if (DEBUG) log.log(str + " ~= s/" + key + "/" + value + "/g");

        while ((idx = str.indexOf(key)) >= 0)
        {
            String pre = str.substring(0, idx);
            String post = str.substring(idx + key.length());

            if (DEBUG) log.log("Found pre=" + pre + ", post=" + post);

            str = pre + value + post;
        }
        if (DEBUG) log.log("<- " + str);
        return str;
    }

    private String resolve(String filename)
    {
        filename = replace(filename, "$root", System.getProperty("dvb.persistent.root"));
        filename = replace(filename, "$oid", (String) ctx.getXletProperty("dvb.org.id"));
        filename = replace(filename, "$aid", (String) ctx.getXletProperty("dvb.app.id"));

        return filename;
    }

    private void create(String filename)
    {
        try
        {
            filename = resolve(filename);

            File f = new File(filename);
            File parentDir = new File(f.getParent());

            test.assertTrue("Parent Directory should exist", parentDir.exists());
            test.assertTrue("Parent Directory should be a directory", parentDir.isDirectory());

            if (parentDir.exists() && parentDir.isDirectory())
            {
                log.log("FileAccessTest - atempting to create " + filename);
                FileOutputStream fos = new FileOutputStream(filename);
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                }
                log.log("FileAccessTest - created " + filename);
                test.assertTrue("Should not have been allowed to create file", expectSuccess);
            }
            else
            {
                return;
            }
        }
        catch (IOException io)
        {
            io.printStackTrace();
            test.fail(io.getMessage());
        }
        catch (SecurityException e)
        {
            log.log("FileAccessTest - caught SecurityException atempting to create " + filename);
            e.printStackTrace();
            test.assertFalse("Test failed trying to create file\n" + e.getMessage(), expectSuccess);
        }
    }

    private void remove(String filename)
    {
        filename = resolve(filename);
        try
        {
            boolean remove = (new File(filename)).delete();
            boolean exists = (new File(filename)).exists();

            if (remove)
            {
                test.assertTrue(remove);
                log.log(filename + " removed");
            }
            else
            {
                test.fail(filename + " not found");
                log.log(filename + " not found");
            }

            /*
             * if (exists) { test.fail(filename + " not removed");
             * log.log(filename + " not removed"); }
             */
        }
        catch (SecurityException e)
        {
            log.log("FileAccessTest - caught SecurityException atempting to remove " + filename);
            e.printStackTrace();
            e.printStackTrace();

            test.assertFalse("Test failed trying to remove filename\n" + e.getMessage(), expectSuccess);
        }
    }

    private void read(String filename)
    {
        try
        {
            filename = resolve(filename);
            log.log("Trying to read " + filename);
            FileInputStream fis = new FileInputStream(filename);
            try
            {
                fis.close();
            }
            catch (IOException e)
            {
            }
            test.assertTrue("Should not have been allowed to read file", expectSuccess);
        }
        catch (IOException io)
        {
            io.printStackTrace();
            test.fail(io.getMessage());
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
            e.printStackTrace();
            test.assertFalse("Test failed trying to read file\n" + e.getMessage(), expectSuccess);
        }
    }

    private void write(String filename)
    {
        filename = resolve(filename);
        try
        {
            log.log("Trying to write " + filename);
            FileOutputStream fos = new FileOutputStream(filename, true);
            try
            {
                fos.close();
            }
            catch (IOException e)
            {
            }
            test.assertTrue("Should not have been allowed to write to \n" + filename, expectSuccess);
        }
        catch (IOException io)
        {
            io.printStackTrace();
            test.fail(io.getMessage());
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
            e.printStackTrace();
            test.assertFalse("Test failed trying to write to file " + filename + "\n" + e.getMessage(), expectSuccess);
        }
    }

    private FileAccessPermissions parseFap(String fap)
    {
        fap = fap.toLowerCase();
        char[] chars = fap.toCharArray();

        return new FileAccessPermissions(chars.length > 0 ? chars[0] == 'r' : false, chars.length > 1 ? chars[1] == 'w'
                : false, chars.length > 2 ? chars[2] == 'r' : false, chars.length > 3 ? chars[3] == 'w' : false,
                chars.length > 4 ? chars[4] == 'r' : false, chars.length > 5 ? chars[5] == 'w' : false);

    }

    private int[] getOIDs(String oids)
    {
        StringTokenizer tok = new StringTokenizer(oids, ",");
        Vector v = new Vector();
        while (tok.hasMoreTokens())
        {
            try
            {
                int oid = (int) (Long.parseLong(tok.nextToken(), 16) & 0xFFFFFFFFL);
                v.addElement(new Integer(oid));
            }
            catch (Exception e)
            {
            }
        }
        int[] array = new int[v.size()];
        for (int i = 0; i < v.size(); ++i)
            array[i] = ((Integer) v.elementAt(i)).intValue();
        return array;
    }

    private ExtendedFileAccessPermissions parseXFap(String xfap)
    {
        xfap = xfap.toLowerCase();

        String fap = xfap;
        String rOids;
        String wOids;
        int i = xfap.indexOf(':');
        if (i < 0)
        {
            rOids = "";
        }
        else
        {
            fap = xfap.substring(0, i);
            rOids = xfap.substring(i + 1);
        }
        i = rOids.indexOf(':');
        if (i < 0)
        {
            wOids = "";
        }
        else
        {
            String tmp = rOids;
            rOids = tmp.substring(0, i);
            wOids = tmp.substring(i + 1);
        }

        FileAccessPermissions base = parseFap(fap);

        return new ExtendedFileAccessPermissions(base.hasReadWorldAccessRight(), base.hasWriteWorldAccessRight(),
                base.hasReadOrganisationAccessRight(), base.hasWriteOrganisationAccessRight(),
                base.hasReadApplicationAccessRight(), base.hasWriteApplicationAccessRight(), getOIDs(rOids),
                getOIDs(wOids));
    }

    private void fap(String fap, String filename)
    {
        setFap(parseFap(fap), filename);
    }

    private void xfap(String xfap, String filename)
    {
        setFap(parseXFap(xfap), filename);
    }

    private void setFap(FileAccessPermissions fap, String filename)
    {
        filename = resolve(filename);

        try
        {
            File f = new File(filename);
            FileAttributes attr = FileAttributes.getFileAttributes(f);
            attr.setPermissions(fap);
            FileAttributes.setFileAttributes(attr, f);

            // success("SetPerms "+filename);
            test.assertTrue("Test should not have succeeded setting permissions", expectSuccess);

            attr = FileAttributes.getFileAttributes(f);
            FileAccessPermissions fap2 = attr.getPermissions();

            // testAssert("Expected a FAP to be returned for "+filename, fap2 !=
            // null);
            // testAssert("Unexpected readWorld for "+filename,
            // fap.hasReadWorldAccessRight(), fap2.hasReadWorldAccessRight());
            // testAssert("Unexpected writeWorld for "+filename,
            // fap.hasWriteWorldAccessRight(), fap2.hasWriteWorldAccessRight());
            // testAssert("Unexpected readOrg for "+filename,
            // fap.hasReadOrganisationAccessRight(),
            // fap2.hasReadOrganisationAccessRight());
            // testAssert("Unexpected writeOrg for "+filename,
            // fap.hasWriteOrganisationAccessRight(),
            // fap2.hasWriteOrganisationAccessRight());
            // testAssert("Unexpected readApplication for "+filename,
            // fap.hasReadApplicationAccessRight(),
            // fap2.hasReadApplicationAccessRight());
            // testAssert("Unexpected writeApplication for "+filename,
            // fap.hasWriteApplicationAccessRight(),
            // fap2.hasWriteApplicationAccessRight());

            test.assertTrue("Expected a FAP to be returned for " + filename, fap2 != null);
            test.assertEquals("Unexpected readWorld for " + filename, fap.hasReadWorldAccessRight(),
                    fap2.hasReadWorldAccessRight());
            test.assertEquals("Unexpected writeWorld for " + filename, fap.hasWriteWorldAccessRight(),
                    fap2.hasWriteWorldAccessRight());
            test.assertEquals("Unexpected readOrg for " + filename, fap.hasReadOrganisationAccessRight(),
                    fap2.hasReadOrganisationAccessRight());
            test.assertEquals("Unexpected writeOrg for " + filename, fap.hasWriteOrganisationAccessRight(),
                    fap2.hasWriteOrganisationAccessRight());
            test.assertEquals("Unexpected readApplication for " + filename, fap.hasReadApplicationAccessRight(),
                    fap2.hasReadApplicationAccessRight());
            test.assertEquals("Unexpected writeApplication for " + filename, fap.hasWriteApplicationAccessRight(),
                    fap2.hasWriteApplicationAccessRight());

            // Disabled because of bug #1989
            // testAssert("Unexpected instanceof XFAP for "+filename, fap
            // instanceof ExtendedFileAccessPermissions, fap2 instanceof
            // ExtendedFileAccessPermissions);
            if (fap instanceof ExtendedFileAccessPermissions)
            {
                // testAssert("Unexpected read OIDs for "+filename,
                // ((ExtendedFileAccessPermissions)fap).getReadAccessOrganizationIds(),
                // ((ExtendedFileAccessPermissions)fap2).getReadAccessOrganizationIds());
                // testAssert("Unexpected write OIDs for "+filename,
                // ((ExtendedFileAccessPermissions)fap).getWriteAccessOrganizationIds(),
                // ((ExtendedFileAccessPermissions)fap2).getWriteAccessOrganizationIds());

                int[] readOIDs1 = ((ExtendedFileAccessPermissions) fap).getReadAccessOrganizationIds();
                int[] readOIDs2 = ((ExtendedFileAccessPermissions) fap2).getReadAccessOrganizationIds();
                boolean match = true;
                if (readOIDs1 == null || readOIDs2 == null)
                    test.fail("OIDs array is null: " + readOIDs1 + ", " + readOIDs2);
                else if (readOIDs1.length == readOIDs2.length)
                {
                    for (int i = 0; i < readOIDs1.length; i++)
                    {
                        for (int j = 0; j < readOIDs2.length; j++)
                        {
                            if (readOIDs1[i] == readOIDs2[j])
                            {
                                match = true;
                                break;
                            }
                            else
                                match = false;
                        }
                    }
                    test.assertTrue("Unexpected read OIDs for " + filename, match);
                }
                test.assertTrue("Unexpected read OIDs for " + filename, match);

                int[] writeOIDs1 = ((ExtendedFileAccessPermissions) fap).getReadAccessOrganizationIds();
                int[] writeOIDs2 = ((ExtendedFileAccessPermissions) fap2).getReadAccessOrganizationIds();
                match = true;
                if (writeOIDs1 == null || writeOIDs2 == null)
                    test.fail("OIDs array is null: " + readOIDs1 + ", " + readOIDs2);
                else if (writeOIDs1.length == writeOIDs2.length)
                {
                    for (int i = 0; i < writeOIDs1.length; i++)
                    {
                        for (int j = 0; j < writeOIDs2.length; j++)
                        {
                            if (writeOIDs1[i] == writeOIDs2[j])
                            {
                                match = true;
                                break;
                            }
                            else
                                match = false;
                        }
                    }
                    test.assertTrue("Unexpected read OIDs for " + filename, match);
                }

                /*
                 * test.assertEquals("Unexpected read OIDs for "+filename,
                 * ((ExtendedFileAccessPermissions
                 * )fap).getReadAccessOrganizationIds(),
                 * ((ExtendedFileAccessPermissions
                 * )fap2).getReadAccessOrganizationIds());
                 * test.assertEquals("Unexpected write OIDs for "+filename,
                 * ((ExtendedFileAccessPermissions
                 * )fap).getWriteAccessOrganizationIds(),
                 * ((ExtendedFileAccessPermissions
                 * )fap2).getWriteAccessOrganizationIds());
                 */
            }

            test.assertTrue("Test should not have succeeded getting permissions", expectSuccess);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
            e.printStackTrace();
            test.assertFalse("Test failed trying to set permission on file \n" + e.getMessage(), expectSuccess);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            test.fail(e.getMessage());
        }
    }

    private XletContext ctx;

    private boolean expectSuccess = true;

    private HScene scene;

    private static final boolean DEBUG = false;

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Logger log;

    private Test test;

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        log.log("AutoXlet EventReceived, but no events are needed by this Xlet");
    }
}
