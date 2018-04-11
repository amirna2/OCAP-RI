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

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.PropertyPermission;

import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContextPermission;

import junit.framework.AssertionFailedError;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppsControlPermission;
import org.dvb.media.DripFeedPermission;
import org.dvb.net.rc.RCPermission;
import org.dvb.net.tuning.TunerPermission;
import org.dvb.spi.ProviderPermission;
import org.dvb.ui.FontFormatException;
import org.dvb.user.UserPreferencePermission;
import org.ocap.application.OcapIxcPermission;
import org.ocap.service.ServiceTypePermission;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.RegisteredApiUserPermission;

import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.security.AppPermissions;
import org.cablelabs.impl.security.PermissionCollectionTest;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests XmlManager implementations.
 * 
 * @todo Complete!
 * 
 * @note Might want to refactor... make the ManagerTest an internal class... and
 *       the main XmlManagerTest tests things using the internal class against
 *       the default xml manager that is returned...
 * 
 * @author Aaron Kamienski
 */
public class XmlManagerTest extends ManagerTest
{
    // **************************** Font Index ********************************

    /**
     * Tests parseFontIndex().
     */
    public void testParseFontIndex() throws Exception
    {
        int styles[][] = { { Font.BOLD, Font.ITALIC, Font.PLAIN, Font.BOLD | Font.ITALIC }, { Font.PLAIN },
                { Font.BOLD }, { Font.ITALIC }, { Font.BOLD | Font.ITALIC }, { Font.PLAIN, Font.BOLD },
                { Font.ITALIC, Font.BOLD, Font.BOLD | Font.ITALIC }, };
        int sizes[][] = { { -1, -1 }, { 0, Integer.MAX_VALUE }, { 1, Integer.MAX_VALUE - 1 }, { 16, 144 }, { -2, -2 }, };
        String names[] = { "FLCL", "Fooly Cooly" };
        String filenames[] = { "flcl.pfr", "FoolyCooly.pfr" };

        // Test reading of single font
        FontInfo[] infos = new FontInfo[styles.length * names.length * filenames.length * sizes.length];
        int i = 0;
        for (int stylesi = 0; stylesi < styles.length; ++stylesi)
        {
            for (int namesi = 0; namesi < names.length; ++namesi)
            {
                for (int filenamesi = 0; filenamesi < filenames.length; ++filenamesi)
                {
                    for (int sizesi = 0; sizesi < sizes.length; ++sizesi)
                    {
                        FontInfo[] info = { new FontInfo(names[namesi], "PFR", filenames[filenamesi], new StyleSet(
                                styles[stylesi]), sizes[sizesi][0], sizes[sizesi][1]), };
                        infos[i++] = info[0];
                        InputStream is = createInputStream(createFontIndex(info));

                        XmlManager.FontInfo[] parsed = xmlmgr.parseFontIndex(is);

                        // Compare FontInfo's!
                        assertNotNull("null should not be returned for " + info[0], parsed);
                        assertEquals("Only one element should be returned for " + info[0], info.length, parsed.length);
                        assertEquals("Unexpected FontInfo returned", info[0], parsed[0]);
                    }
                }
            }
        }

        // Test reading of multiple fonts
        InputStream is = createInputStream(createFontIndex(infos));
        XmlManager.FontInfo[] parsed = xmlmgr.parseFontIndex(is);
        // Compare FontInfo's!
        assertNotNull("null should not be returned", parsed);
        assertEquals("Wrong number of elements returned", infos.length, parsed.length);
        for (i = 0; i < infos.length; ++i)
        {
            assertEquals("Unexpected FontInfo returned (" + i + ")", infos[i], parsed[i]);
        }
    }

    /**
     * Tests parseFontIndex() generation of IOException.
     * <ul>
     * <li>Error reading from file (should be propogated)
     * </ul>
     */
    public void testParseFontIndex_IO() throws Exception
    {
        FontInfo info[] = { new FontInfo("Cool", "PFR", "cool.pfr", null, 0, Integer.MAX_VALUE) };
        // Throw IOException after arbitrary number (4) bytes
        InputStream shorted = new ShortedInputStream(createInputStream(createFontIndex(info)));

        try
        {
            xmlmgr.parseFontIndex(shorted);
            fail("Expected IOException to be propagated");
        }
        catch (IOException e)
        { /* OKAY */
        }
    }

    /**
     * Tests parseFontIndex() generation of FontFormatException.
     * <ul>
     * <li>Non integers for size.
     * <li>"PFR" is not the font format.
     * <li>Required fields are missing.
     * <li>Other errors. ???
     * </ul>
     */
    public void testParseFontIndex_Format() throws Exception
    {
        // fontdirectory isn't the root element
        try
        {
            InputStream is = createInputStream("<junk/>");
            xmlmgr.parseFontIndex(is);

            fail("Expected FontFormatException for missing <fontdirectory>");
        }
        catch (FontFormatException e)
        { /* expected */
        }

        // garbage max
        try
        {
            FontInfo info[] = { new FontInfo("blah", "PFR", "blah.pfr", null, 0, -3) };
            InputStream is = createInputStream(createFontIndex(info));
            xmlmgr.parseFontIndex(is);

            fail("Expected FontFormatException for invalid max size (xyz)");
        }
        catch (FontFormatException e)
        { /* expected */
        }

        // garbage min
        try
        {
            FontInfo info[] = { new FontInfo("blah", "PFR", "blah.pfr", null, -3, Integer.MAX_VALUE) };
            InputStream is = createInputStream(createFontIndex(info));
            xmlmgr.parseFontIndex(is);

            fail("Expected FontFormatException for invalid min size (xyz)");
        }
        catch (FontFormatException e)
        { /* expected */
        }

        if (false) // not verified by XmlManager but by FontFactory
        {
            // type must be PFR
            try
            {
                FontInfo info[] = { new FontInfo("blah", "TrueType", "blah.tt", null, 0, Integer.MAX_VALUE) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for non PFR font");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // name is required
            try
            {
                FontInfo info[] = { new FontInfo(null, "PFR", "blah.pfr", null, 0, Integer.MAX_VALUE) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for missing name");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // format is required
            try
            {
                FontInfo info[] = { new FontInfo("blah", null, "blah.pfr", null, 0, Integer.MAX_VALUE) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for missing format");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // filename is required
            try
            {
                FontInfo info[] = { new FontInfo("blah", "PFR", null, null, 0, Integer.MAX_VALUE) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for missing filename");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // invalid max
            try
            {
                FontInfo info[] = { new FontInfo("blah", "PFR", "blah.pfr", null, 0, -1) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for invalid max size (-1)");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // invalid min
            try
            {
                FontInfo info[] = { new FontInfo("blah", "PFR", "blah.pfr", null, -1, Integer.MAX_VALUE) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for invalid min size (-1)");
            }
            catch (FontFormatException e)
            { /* expected */
            }

            // min > max
            try
            {
                FontInfo info[] = { new FontInfo("blah", "PFR", "blah.pfr", null, 10, 9) };
                InputStream is = createInputStream(createFontIndex(info));
                xmlmgr.parseFontIndex(is);

                fail("Expected FontFormatException for invalid size (min>max)");
            }
            catch (FontFormatException e)
            { /* expected */
            }
        }
    }

    /**
     * Tests parseFontIndex() ignoring of garbage. Essentially unknown tags and
     * attributes.
     */
    public void testParseFontIndex_ignore() throws Exception
    {
        FontInfo[] info = { new FontInfo("FLCL", "PFR", "flcl.pfr", null, 1, 72),
                new FontInfo("abcd", "PFR", "abcd.pfr", new StyleSet(Font.ITALIC), 1, 72),
                new FontInfo("wxyz", "PFR", "wxyz.pfr", new StyleSet(new int[] { Font.PLAIN, Font.BOLD }), 1, 72), };

        InputStream is = createInputStream(createFontIndex(info, true));
        XmlManager.FontInfo[] parsed = xmlmgr.parseFontIndex(is);
        // Compare FontInfo's!
        assertNotNull("null should not be returned", parsed);
        assertEquals("Wrong number of elements returned", info.length, parsed.length);
        for (int i = 0; i < info.length; ++i)
        {
            assertEquals("Unexpected FontInfo returned (" + i + ")", info[i], parsed[i]);
        }
    }

    // **************************** Permission Request
    // ********************************

    private String permissionRequestFile(AppID id, String contents)
    {
        int AID = id.getAID();
        int OID = id.getOID();

        return PRF_header + "\n" + "\n<permissionrequestfile " + "appid=\"0x" + Integer.toHexString(AID) + "\" "
                + "orgid=\"0x" + Integer.toHexString(OID) + "\">" + "\n" + contents + "\n</permissionrequestfile>";
    }

    private void doTestParsePRF_string(String prfEntry, boolean monApp, AppID id, Long serviceContextID,
            PermissionCollection expected) throws Exception
    {
        doTestParsePRF_string(prfEntry, monApp, monApp, id, serviceContextID, expected, null);
    }

    private void doTestParsePRF_string(String prfEntry, boolean monApp, AppID id, Long serviceContextID,
            PermissionCollection expected, PermissionCollection unexpected) throws Exception
    {
        doTestParsePRF_string(prfEntry, monApp, monApp, id, serviceContextID, expected, unexpected);
    }

    private void doTestParsePRF_string(String prfEntry, boolean ocapApp, boolean monApp, AppID id,
            Long serviceContextID, PermissionCollection expected, PermissionCollection unexpected) throws Exception
    {
        doTestParsePRF_string(prfEntry, ocapApp, monApp, id, serviceContextID, expected, unexpected, false);
    }

    private void doTestParsePRF_string(String prfEntry, boolean ocapApp, boolean monApp, AppID id,
            Long serviceContextID, PermissionCollection expected, PermissionCollection unexpected, boolean oneWay)
            throws Exception
    {
        byte[] prfData = permissionRequestFile(id, prfEntry).getBytes();

        PermissionCollection parsed = xmlmgr.parsePermissionRequest(prfData, ocapApp, monApp, id, serviceContextID);

        if (expected == null)
            assertSame("Expected parsePermissionRequest() to fail", null, parsed);
        else if (!expected.elements().hasMoreElements())
            assertEquals("Expected parsed collection to be empty", expected.elements().hasMoreElements(),
                    parsed.elements().hasMoreElements());
        else
        {
            assertNotNull("Expected no failures in parsing", parsed);
            assertTrue("Expected instance of AppPermissions", parsed instanceof AppPermissions);

            // Expect that parsed implies expected
            PermissionCollectionTest.checkImplies(parsed, expected);

            // Expect that expected implies parsed
            if (oneWay == false)
            {
                PermissionCollectionTest.checkImplies(expected, parsed);
            }
        }

        if (unexpected != null)
        {
            assertNotNull("Expected no failures in parsing", parsed);
            assertTrue("Expected instance of AppPermissions", parsed instanceof AppPermissions);

            // Expect that parsed does NOT imply unexpected
            checkNotImplied(parsed, unexpected);
        }
    }

    /**
     * The OCAP and/or MHP specs dictate that certain permissions be included
     * for signed apps even if they don't show up in the PRF.
     * 
     * Adds the SelectPermission("*","own") to the expected permissions
     * collection Adds the default OcapIxcPermissions
     * 
     * @param serviceContextID
     *            the serviceContextID for OcapIxcPermissions
     * @param appID
     *            the appID for OcapIxcPermissions
     * @return the base expected permissions collection
     */
    private PermissionCollection buildBaseExpectedPermissions(Long serviceContextID, AppID appID)
    {
        return buildBaseExpectedPermissions(true, true, serviceContextID, appID);
    }

    /**
     * 
     * @param addSelectPerm
     * @param addIxcPerm
     * @param serviceContextID
     * @param appID
     * @return
     */
    private PermissionCollection buildBaseExpectedPermissions(boolean addSelectPerm, boolean addIxcPerm,
            Long serviceContextID, AppID appID)
    {
        PermissionCollection pc = new Permissions();

        if (addSelectPerm) pc.add(new SelectPermission("*", "own"));

        if (addIxcPerm)
        {
            pc.add(new OcapIxcPermission("/service-" + serviceContextID.longValue() + "/signed/*/*/*", "lookup"));
            pc.add(new OcapIxcPermission("/service-" + serviceContextID.longValue() + "/signed" + "/"
                    + Integer.toHexString(appID.getOID()) + "/" + Integer.toHexString(appID.getAID()) + "/*", "bind"));
        }

        return pc;

    }

    private void checkNotImplied(PermissionCollection pc1, PermissionCollection pc2)
    {
        for (Enumeration e = pc2.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            if (pc1.implies(p)) fail("Expected permission to not be implied: " + p);
        }
    }

    // private static final PermissionCollection REALLY_EMPTY = new
    // Permissions();
    private static final PermissionCollection EMPTY = new Permissions();

    private static final Long SC_ID = new Long(1); // Test service context ID

    private static final AppID SIGNED_ID = new AppID(0xbeef, 0x4567);

    private static final AppID DUALSIGNED_ID = new AppID(0xcafe, 0x6789);
    static
    {
        EMPTY.add(new SelectPermission("*", "own"));
    }

    /**
     * Tests &lt;file&gt;.
     */
    public void testParsePRF_file() throws Exception
    {
        String original = MPEEnv.getSystemProperty("dvb.persistent.root");
        try
        {
            String root = "/test/persistent/root";
            System.setProperty("dvb.persistent.root", root);

            // Test negative case
            PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
            doTestParsePRF_string("<file value=\"false\"/>", false, SIGNED_ID, SC_ID, perms);
            doTestParsePRF_string("<file value=\"false\">\nblah</file>", false, SIGNED_ID, SC_ID, perms);

            // read access to persistent root is expected
            root += "/";
            perms.add(new FilePermission(root, "read"));
            perms.add(new FilePermission(root + "*", "read"));

            // read/write access to OID dir is expected
            root += Integer.toHexString(SIGNED_ID.getOID()) + "/";
            perms.add(new FilePermission(root, "read, write"));
            perms.add(new FilePermission(root + "*", "read,write,delete"));

            // read/write access to AID dir is expected
            root += Integer.toHexString(SIGNED_ID.getAID()) + "/";
            perms.add(new FilePermission(root, "read, write"));
            perms.add(new FilePermission(root + "-", "read,write,delete"));

            // Test expected values
            doTestParsePRF_string("<file/>", false, SIGNED_ID, SC_ID, perms);
            doTestParsePRF_string("<file value=\"true\"/>", false, SIGNED_ID, SC_ID, perms);
            doTestParsePRF_string("<file value=\"true\">\nblah</file>", false, SIGNED_ID, SC_ID, perms);
        }
        finally
        {
            if (original == null) original = "/";
            System.setProperty("dvb.persistent.root", original);
        }
    }

    /**
     * Tests &lt;applifecyclecontrol&gt;
     */
    public void testParsePRF_applifecyclecontrol() throws Exception
    {
        // Test negative values
        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("<applifecyclecontrol value=\"false\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<applifecyclecontrol value=\"false\">\nblah</applifecyclecontrol>", false, SIGNED_ID,
                SC_ID, perms);

        // Test expected values
        perms.add(new AppsControlPermission(null, null));

        doTestParsePRF_string("<applifecyclecontrol/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<applifecyclecontrol value=\"true\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<applifecyclecontrol value=\"true\">\nblah</applifecyclecontrol>", false, SIGNED_ID,
                SC_ID, perms);
    }

    /**
     * Tests &lt;returnchannel&gt;.
     */
    public void testParsePRF_returnchannel() throws Exception
    {
        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("<returnchannel/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<returnchannel></returnchannel>", false, SIGNED_ID, SC_ID, perms);

        String[] names = { "default", "555-1234", "555", // match any number
                                                         // starting with 555
                "3" // match any number starting with
        };

        String tags = "";
        for (int i = 0; i < names.length; ++i)
        {
            if ("default".equals(names[i]))
                tags += "<defaultisp/>\n";
            else
                tags += "<phonenumber>\n" + names[i] + "</phonenumber>\n";
            perms.add(new RCPermission("target:" + names[i]));

            doTestParsePRF_string("<returnchannel>" + tags + "</returnchannel>", false, false, SIGNED_ID, SC_ID, perms,
                    null, true);
        }
    }

    /**
     * Tests &lt;tuning&gt;.
     */
    public void testParsePRF_tuning() throws Exception
    {
        // Test negative values
        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("<tuning value=\"false\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<tuning value=\"false\">\nblah</tuning>", false, SIGNED_ID, SC_ID, perms);

        // Test expected values
        perms.add(new TunerPermission(null));
        doTestParsePRF_string("<tuning/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<tuning value=\"true\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<tuning value=\"true\">\nblah</tuning>", false, SIGNED_ID, SC_ID, perms);
    }

    /**
     * Tests &lt;servicesel&gt;.
     */
    public void testParsePRF_servicesel() throws Exception
    {
        // Test negative values
        PermissionCollection perms = buildBaseExpectedPermissions(false, true, SC_ID, SIGNED_ID);
        doTestParsePRF_string("<servicesel value=\"false\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<servicesel value=\"false\">\nblah</servicesel>", false, SIGNED_ID, SC_ID, perms);

        // Test expected values
        perms.add(new SelectPermission("*", "own"));
        doTestParsePRF_string("", false, SIGNED_ID, SC_ID, perms); // get unless
                                                                   // denied!
        doTestParsePRF_string("<servicesel/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<servicesel value=\"true\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<servicesel value=\"true\">\nblah</servicesel>", false, SIGNED_ID, SC_ID, perms);
    }

    /**
     * Tests &lt;userpreferences&gt;.
     */
    public void testParsePRF_userpreferences() throws Exception
    {
        // Test negative case
        PermissionCollection base = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("<userpreferences read=\"false\"/>", false, SIGNED_ID, SC_ID, base);
        doTestParsePRF_string("<userpreferences read=\"false\" write=\"false\"/>", false, SIGNED_ID, SC_ID, base);

        // Test read perms
        PermissionCollection read = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        read.add(new UserPreferencePermission("read"));
        doTestParsePRF_string("<userpreferences/>", false, SIGNED_ID, SC_ID, read);
        doTestParsePRF_string("<userpreferences read=\"true\"/>", false, SIGNED_ID, SC_ID, read);
        doTestParsePRF_string("<userpreferences write=\"false\"/>", false, SIGNED_ID, SC_ID, read);
        doTestParsePRF_string("<userpreferences read=\"true\" write=\"false\"/>", false, SIGNED_ID, SC_ID, read);

        // Test write perms
        PermissionCollection write = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        write.add(new UserPreferencePermission("write"));
        doTestParsePRF_string("<userpreferences read=\"false\" write=\"true\"/>", false, SIGNED_ID, SC_ID, write);

        // Test read/write perms
        PermissionCollection readWrite = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        readWrite.add(new UserPreferencePermission("read"));
        readWrite.add(new UserPreferencePermission("write"));
        doTestParsePRF_string("<userpreferences write=\"true\"/>", false, SIGNED_ID, SC_ID, readWrite);
        doTestParsePRF_string("<userpreferences read=\"true\" write=\"true\"/>", false, SIGNED_ID, SC_ID, readWrite);
    }

    /**
     * Tests &lt;ocap:ixc&gt;.
     */
    public void testParsePRF_ixc() throws Exception
    {
        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);

        // Test empty PRF
        doTestParsePRF_string("", true, false, SIGNED_ID, SC_ID, perms, null);

        // Test default bind action
        perms = buildBaseExpectedPermissions(true, false, SC_ID, SIGNED_ID);
        perms.add(new OcapIxcPermission("/service-" + SC_ID.longValue() + "/signed" + "/"
                + Integer.toHexString(SIGNED_ID.getOID()) + "/" + Integer.toHexString(SIGNED_ID.getAID()) + "/*",
                "bind"));
        doTestParsePRF_string("<ocap:ixc action=\"bind\" />", true, false, SIGNED_ID, SC_ID, perms, null);

        // Test default lookup action
        perms = buildBaseExpectedPermissions(true, false, SC_ID, SIGNED_ID);
        perms.add(new OcapIxcPermission("/service-" + SC_ID.longValue() + "/signed/*/*/*", "lookup"));
        doTestParsePRF_string("<ocap:ixc action=\"lookup\" />", true, false, SIGNED_ID, SC_ID, perms, null);

        // Test Restricted bind name
        perms = buildBaseExpectedPermissions(true, false, SC_ID, SIGNED_ID);
        perms.add(new OcapIxcPermission("/service-" + SC_ID.longValue() + "/signed" + "/"
                + Integer.toHexString(SIGNED_ID.getOID()) + "/" + Integer.toHexString(SIGNED_ID.getAID()) + "/Name*",
                "bind"));
        doTestParsePRF_string("<ocap:ixc name=\"Name*\" action=\"bind\" />", true, false, SIGNED_ID, SC_ID, perms, null);

        // Test orgID/appID restricted lookup
        perms = buildBaseExpectedPermissions(true, false, SC_ID, SIGNED_ID);
        perms.add(new OcapIxcPermission("/service-" + SC_ID.longValue() + "/signed" + "/"
                + Integer.toHexString(SIGNED_ID.getOID()) + "/" + Integer.toHexString(SIGNED_ID.getAID()) + "/*",
                "lookup"));
        doTestParsePRF_string("<ocap:ixc " + "oid= \"" + Integer.toHexString(SIGNED_ID.getOID()) + "\" " + "aid= \""
                + Integer.toHexString(SIGNED_ID.getAID()) + "\" " + "action=\"lookup\" />", true, false, SIGNED_ID,
                SC_ID, perms, null);

        // Test service wildcard lookup
        perms = buildBaseExpectedPermissions(true, false, SC_ID, SIGNED_ID);
        perms.add(new OcapIxcPermission("/service-*/signed/*/*/*", "lookup"));
        doTestParsePRF_string("<ocap:ixc scope=\"xservice\" action=\"lookup\" />", true, false, SIGNED_ID, SC_ID,
                perms, null);
    }

    /**
     * Tests &lt;network&gt;.
     */
    public void testParsePRF_network() throws Exception
    {
        SocketPermission socketPerms[] = {
                // NOTE: if names cannot be resolved, they are ignored (unless
                // they have a wildcard)
                // On the STB, names often cannot be resolved, so we won't test
                // specific names (including localhost!)
                // new SocketPermission("www.cablelabs.org", "connect"),
                // new SocketPermission("www.sun.com:80", "connect"),
                new SocketPermission("*.sun.com", "connect"), new SocketPermission("*.edu", "resolve"),
                new SocketPermission("204.160.241.0", "connect"),
                new SocketPermission("*.cablelabs.org:1024-65535", "listen"),
                new SocketPermission("204.160.241.0:1024-65535", "connect"), };

        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("", false, SIGNED_ID, SC_ID, perms, null);

        String tags = "";
        for (int i = 0; i < socketPerms.length; ++i)
        {
            tags += "\n<host action=\"" + socketPerms[i].getActions() + "\"> " + socketPerms[i].getName() + "</host>";
            perms.add(socketPerms[i]);
            doTestParsePRF_string("<network>" + tags + "</network>", false, SIGNED_ID, SC_ID, perms);
        }
        doTestParsePRF_string("<network></network>", false, SIGNED_ID, SC_ID, null);
    }

    /**
     * Tests PRF parsing of &lt;dripfeed&gt;.
     */
    public void testParsePRF_dripfeed() throws Exception
    {
        // Test negative values
        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        doTestParsePRF_string("<dripfeed value=\"false\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<dripfeed value=\"false\">\nblah</dripfeed>", false, SIGNED_ID, SC_ID, perms);

        // Test expected values
        perms.add(new DripFeedPermission(null, null));
        doTestParsePRF_string("<dripfeed/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<dripfeed value=\"true\"/>", false, SIGNED_ID, SC_ID, perms);
        doTestParsePRF_string("<dripfeed value=\"true\">\nblah</dripfeed>", false, SIGNED_ID, SC_ID, perms);
    }

    /**
     * Produce a string representation of <i>i</i>, padded out to <i>n</i>
     * characters with preceding <code>0</code>'s.
     * 
     * @param i
     *            integer to represent as a string
     * @param n
     *            the number of characters
     * @return the string representation
     */
    private String toString(int i, int n)
    {
        String str = Integer.toString(i);
        while (str.length() < n)
            str = "0" + str;
        return str;
    }

    /**
     * Tests PRF parsing of &lt;persistentfilecredential&gt;.
     */
    public void testParsePRF_persistentfilecredential() throws Exception
    {
        String files[] = { "beef/cafe/PUBLIC/-", "beef/cafe/PUBLIC/PRIVATE/-" };
        String val00[] = { "false", "false" };
        String val01[] = { "false", "true" };
        String val10[] = { "true", "false" };
        String val11[] = { "true", "true" };
        String valxx[] = { null, null };
        String val0x[] = { "false", null };
        String valx0[] = { null, "false" };

        doTestParsePRF_persistentfilecredential(files, valxx, val00);
        doTestParsePRF_persistentfilecredential(files, val00, valxx);
        doTestParsePRF_persistentfilecredential(files, valxx, valxx);
        doTestParsePRF_persistentfilecredential(files, valxx, valx0);
        doTestParsePRF_persistentfilecredential(files, val0x, valxx);
        doTestParsePRF_persistentfilecredential(files, val00, val00);
        doTestParsePRF_persistentfilecredential(files, val01, val00);
        doTestParsePRF_persistentfilecredential(files, val11, val01);
        doTestParsePRF_persistentfilecredential(files, val10, val01);
    }

    /**
     * Implements testing PRF parsing of &lt;persistentfilecredential&gt;.
     */
    private void doTestParsePRF_persistentfilecredential(String[] files, String[] read, String[] write)
            throws Exception
    {
        Date date = new Date(System.currentTimeMillis() + 3600 * 1000 * 24);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);
        // Note: dd/MM/yyyy
        String dateString = toString(day, 2) + "/" + toString(month, 2) + "/" + toString(year, 4);
        String grantor = "0xabcdef";
        String sigStr = "..."; // TODO(AaronK): generate signature bytes
        String certStr = "1";

        doTestParsePRF_persistentfilecredential(files, read, write, dateString, grantor, sigStr, certStr);
        // TODO(AaronK): test expiration date on PFC
        // TODO(AaronK): test that PFC object is returned
    }

    /**
     * Implements testing PRF parsing of &lt;persistentfilecredential&gt;.
     */
    private void doTestParsePRF_persistentfilecredential(String[] files, String[] read, String[] write,
            String dateString, String grantor, String sigStr, String certStr) throws Exception
    {
        // TODO(AaronK): need certificate to test with!
        // For now, we assume that certificate/signature isn't tested
        String original = MPEEnv.getSystemProperty("dvb.persistent.root");
        try
        {
            String root = "/test/persistent/root";
            System.setProperty("dvb.persistent.root", root);

            String tagStart = "<persistentfilecredential>\n" + "<grantoridentifier id=\"" + grantor + "\" />\n"
                    + "<expirationdate date=\"" + dateString + "\" />\n";
            // filenames
            String tagEnd = "<signature>" + sigStr + "</signature>\n" + "<certchainfileid>" + certStr
                    + "</certchainfileid>\n" + "</persistentfilecredential>\n";

            // PermissionCollection perms =
            // buildBaseExpectedPermissions(SC_ID,SIGNED_ID);
            PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
            String fileTags = "";
            for (int i = 0; i < files.length; ++i)
            {
                String actions = "";
                String sep = "";
                if (!"false".equals(read[i]))
                {
                    actions += "read";
                    sep = ",";
                }
                if (!"false".equals(write[i])) actions += sep + "write,delete";
                if (!"".equals(actions)) perms.add(new FilePermission(root + "/" + files[i], actions));

                fileTags += "<filename" + ((read[i] == null) ? "" : " read=\"" + read[i] + "\"")
                        + ((write[i] == null) ? "" : " write=\"" + write[i] + "\"") + ">" + files[i] + "</filename>\n";
            }

            doTestParsePRF_string(fileTags, false, SIGNED_ID, SC_ID, perms, null);
            doTestParsePRF_string(tagStart + fileTags + tagEnd, false, SIGNED_ID, SC_ID, perms, null);
        }
        finally
        {
            if (original == null) original = "/";
            System.setProperty("dvb.persistent.root", original);
        }
    }

    /**
     * Tests PRF parsing of &lt;ocap:monitorapplication&gt;.
     */
    public void testParsePRF_monitorapplication() throws Exception
    {
        doTestParsePRF_monitorapplication("registrar");
        doTestParsePRF_monitorapplication("security");
        doTestParsePRF_monitorapplication("reboot");
        doTestParsePRF_monitorapplication("systemevent");
        doTestParsePRF_monitorapplication("handler.appFilter");
        doTestParsePRF_monitorapplication("handler.resource");
        doTestParsePRF_monitorapplication("handler.closedCaptioning");
        doTestParsePRF_monitorapplication("filterUserEvents");
        doTestParsePRF_monitorapplication("handler.eas");
        doTestParsePRF_monitorapplication("setVideoPort");
        doTestParsePRF_monitorapplication("podApplication");
        doTestParsePRF_monitorapplication("signal.configured");
        doTestParsePRF_monitorapplication("storage");
        if (PRE_ECO_852)
            doTestParsePRF_monitorapplication("registeredapi", "registeredapi.manager",
                    extraMonAppPermissions("registeredapi"));
        else
        {
            boolean failed = false;
            try
            {
                doTestParsePRF_monitorapplication("registeredapi", "registeredapi.manager",
                        extraMonAppPermissions("registeredapi"));
            }
            catch (AssertionFailedError e)
            {
                failed = true;
            }
            assertTrue("Should not accept pre ECO 852 'registeredapi'", failed);
        }
        doTestParsePRF_monitorapplication("registeredapi.manager");
        doTestParsePRF_monitorapplication("vbifiltering");
        doTestParsePRF_monitorapplication("codeDownload");
        doTestParsePRF_monitorapplication("mediaAccess");
        doTestParsePRF_monitorapplication("service", null, extraMonAppPermissions("service"));
        doTestParsePRF_monitorapplication("servicemanager", null, extraMonAppPermissions("servicemanager"));
        doTestParsePRF_monitorapplication("properties", null, extraMonAppPermissions("properties"));
    }

    private PermissionCollection extraMonAppPermissions(String name)
    {
        return extraMonAppPermissions(name, false);
    }

    private PermissionCollection extraMonAppPermissions(String name, boolean serviceType)
    {
        Permissions extra = new Permissions();

        if ("service".equals(name))
        {
            // "service": OCAP 10.2.2.2.3.3
            extra.add(new ServiceContextPermission("access", "*"));
            extra.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
            extra.add(new ServiceContextPermission("create", "own"));
            extra.add(new ServiceContextPermission("destroy", "own"));
            extra.add(new ServiceContextPermission("stop", "*"));
            extra.add(new SelectPermission("*", "own"));
            if (!serviceType) extra.add(new ServiceTypePermission("*", "own")); // unless
                                                                                // overridden
                                                                                // by
                                                                                // ServiceTypePermission...
        }
        else if ("servicemanager".equals(name))
        {
            // "servicemanager": OCAP 10.2.2.2.3.3
            extra.add(new ServiceContextPermission("access", "*"));
            extra.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
            extra.add(new ServiceContextPermission("create", "own"));
            extra.add(new ServiceContextPermission("destroy", "own"));
            extra.add(new ServiceContextPermission("stop", "*"));
            extra.add(new SelectPermission("*", "own"));
            if (!serviceType) extra.add(new ServiceTypePermission("*", "*")); // unless
                                                                              // overridden
                                                                              // by
                                                                              // ServiceTypePermission...
            // OCAP 10.2.2.2.5
            extra.add(new ProviderPermission("*", "system"));
            // OCAP 10.2.2.3
            extra.add(new AppsControlPermission(null, null));
        }
        else if ("properties".equals(name))
        {
            // "properties": 21.2.1.20, 13.3.12.3
            extra.add(new PropertyPermission("ocap.hardware.vendor_id", "read"));
            extra.add(new PropertyPermission("ocap.hardware.version_id", "read"));
            extra.add(new PropertyPermission("ocap.hardware.createdate", "read"));
            extra.add(new PropertyPermission("ocap.hardware.serialnum", "read"));
            extra.add(new PropertyPermission("ocap.memory.video", "read"));
            extra.add(new PropertyPermission("ocap.memory.total", "read"));
        }
        else if ("registeredapi".equals(name)) // PRE_ECO_852
        {
            extra.add(new RegisteredApiUserPermission("*"));
        }
        else
            return null;
        return extra;
    }

    /**
     * Implements PRF parsing of &lt;ocap:monitorapplication&gt; test. Tests a
     * single MonitorAppPermission. Invokes
     * {@link #doTestParsePRF_monitorapplication(String, String, PermissionCollection)}
     * with a <code>null</code> extra set of permissions.
     */
    private void doTestParsePRF_monitorapplication(String name) throws Exception
    {
        doTestParsePRF_monitorapplication(name, null, null);
    }

    /**
     * Implements PRF parsing of &lt;ocap:monitorapplication&gt; test. Tests a
     * single MonitorAppPermission multiple times with different parameters.
     * Invokes
     * {@link #doTestParsePRF_monitorApplication_value(String, String, boolean, String, PermissionCollection)}
     * with different values for <i>monApp</i> and <i>ocap:value</i>.
     */
    private void doTestParsePRF_monitorapplication(String name, String monAppName, PermissionCollection extra)
            throws Exception
    {
        doTestParsePRF_monitorApplication_value(name, monAppName, true, "true", extra);
        doTestParsePRF_monitorApplication_value(name, monAppName, false, "true", extra);
        doTestParsePRF_monitorApplication_value(name, monAppName, true, "false", extra);
        doTestParsePRF_monitorApplication_value(name, monAppName, false, "false", extra);
    }

    /**
     * Implements PRF parsing of &lt;ocap:monitorapplication&gt; test. Tests a
     * single MonitorAppPermission multiple times with different parameters.
     * 
     * @param name
     *            the name of the requested permission in the PRF
     * @param monAppName
     *            the name of the MonitorAppPermission (if null, then name is
     *            used)
     * @param monApp
     *            true if MonitorAppPermission should be supported
     * @param value
     *            the ocap:value attribute value
     * @param extra
     *            any additional permissions expected to be granted
     */
    private void doTestParsePRF_monitorApplication_value(String name, String monAppName, boolean monApp, String value,
            PermissionCollection extra) throws Exception
    {
        if (monAppName == null) monAppName = name;

        String pfx = I15_MONAPP ? "ocap:" : "";

        String tag = "<ocap:monitorapplication " + pfx + "name=\"" + name + "\" "
                + ((value == null) ? "" : pfx + "value=\"" + value + "\" ") + " />";

        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, DUALSIGNED_ID);
        if ("true".equals(value) && monApp)
        {
            perms.add(new MonitorAppPermission(monAppName));

            if (extra != null) for (Enumeration e = extra.elements(); e.hasMoreElements();)
                perms.add((Permission) e.nextElement());
        }

        doTestParsePRF_string(tag, monApp, DUALSIGNED_ID, SC_ID, perms, null);
    }

    private static final boolean I16_PRF = "true".equals(MPEEnv.getEnv("OCAP.prf.I16"));

    private static final boolean PRE_ECO_852 = I16_PRF;

    private static final boolean I15_PRF = "true".equals(MPEEnv.getEnv("OCAP.prf.I15"));

    private static final boolean I15_SERVICETYPE = I15_PRF;

    private static final boolean I15_MONAPP = I15_PRF;

    /**
     * Tests PRF parsing of &lt;servicetypepermission&gt;
     */
    public void testParsePRF_servicetypepermission() throws Exception
    {
        doTestParsePRF_servicetypepermission(true);
    }

    /**
     * Tests PRF parsing of &lt;servicetypepermission&gt; for non-OCAP PRF.
     */
    public void testParsePRF_servicetypepermission_DVB() throws Exception
    {
        doTestParsePRF_servicetypepermission(false);
    }

    /**
     * Tests PRF parsing of &lt;servicetypepermission&gt; for either OCAP or DVB
     * prf.
     */
    private void doTestParsePRF_servicetypepermission(boolean ocap) throws Exception
    {
        doTestParsePRF_servicetypepermission(null, null, null, ocap);
        doTestParsePRF_servicetypepermission(null, null, "true", ocap);
        doTestParsePRF_servicetypepermission("broadcast", "own", "false", ocap);
        doTestParsePRF_servicetypepermission(null, "*", "true", ocap);
        doTestParsePRF_servicetypepermission(null, "own", "true", ocap);
        doTestParsePRF_servicetypepermission("broadcast", null, "true", ocap);
        doTestParsePRF_servicetypepermission("abstract.mso", null, "true", ocap);
        doTestParsePRF_servicetypepermission("abstract.manufacturer", null, "true", ocap);
        doTestParsePRF_servicetypepermission("abstract.mso", "own", "true", ocap);
    }

    /**
     * Tests PRF parsing of &lt;servicetypepermission&gt with different
     * parameters.
     * 
     * @param type
     *            ocap:type attribute or null if default
     * @param actions
     *            ocap:actions attribute or null if default
     * @param value
     *            ocap:value attribute or null if default
     * @param ocap
     *            if parsing an OCAP PRF or a DVB PRF
     */
    private void doTestParsePRF_servicetypepermission(String type, String actions, String value, boolean ocap)
            throws Exception
    {
        // DTD specifies "all", but ServiceTypePermission specifies "*"
        if ("all".equals(actions)) actions = "*";

        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        if (ocap && "true".equals(value))
        {
            perms.add(new ServiceTypePermission((type == null) ? "broadcast" : type, (actions == null) ? "*" : actions));
        }

        // I16 DTD specifies "all", but ServiceTypePermission specifies "*"
        if (!I15_SERVICETYPE && "*".equals(actions)) actions = "all";
        String aType, aActions, aValue;
        if (I15_SERVICETYPE)
        {
            aType = "ocap:type";
            aActions = "ocap:actions";
            aValue = "ocap:value";
        }
        else
        {
            aType = "type";
            aActions = "action";
            aValue = "value";
        }

        String tag = "<ocap:servicetypepermission " + ((type == null) ? "" : aType + "=\"" + type + "\" ")
                + ((actions == null) ? "" : aActions + "=\"" + actions + "\" ")
                + ((value == null) ? "" : aValue + "=\"" + value + "\" ") + "/>";

        doTestParsePRF_string(tag, ocap, false, SIGNED_ID, SC_ID, perms, null);
    }

    /**
     * Verify that "servicemanager" or "service" MonAppPermission doesn't
     * include ServiceTypePermission, if one is already included.
     */
    public void testParsePRF_monitorapplication_servicetype() throws Exception
    {
        doTestParsePRF_monitorapplication_servicetype("servicemanager", true, new ServiceTypePermission("*", "*"));
        doTestParsePRF_monitorapplication_servicetype("servicemanager", false, new ServiceTypePermission("*", "*"));

        // Give "service" permission
        // Give a service type permission, ensure don't get default service type
        // permission
        doTestParsePRF_monitorapplication_servicetype("service", true, new ServiceTypePermission("*", "own"));
        doTestParsePRF_monitorapplication_servicetype("service", false, new ServiceTypePermission("*", "own"));
    }

    private void doTestParsePRF_monitorapplication_servicetype(String name, boolean value, ServiceTypePermission exclude)
            throws Exception
    {
        String pfx = I15_MONAPP ? "ocap:" : "";
        String aType, aActions, aValue;
        if (I15_SERVICETYPE)
        {
            aType = "ocap:type";
            aActions = "ocap:actions";
            aValue = "ocap:value";
        }
        else
        {
            aType = "type";
            aActions = "action";
            aValue = "value";
        }

        String monappTag = "<ocap:monitorapplication " + pfx + "name=\"" + name + "\" " + pfx + "value=\"true\" "
                + " />";
        String typeTag = "<ocap:servicetypepermission " + aType + "=\"broadcast\" " + aActions + "=\"own\" " + aValue
                + "=\"" + value + "\" " + "/>";

        Permissions exclPerms = new Permissions();
        exclPerms.add(exclude);

        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, DUALSIGNED_ID);
        perms.add(new MonitorAppPermission(name));
        if (value) perms.add(new ServiceTypePermission(ServiceTypePermission.BROADCAST, "own"));

        PermissionCollection extra = extraMonAppPermissions(name, true);
        if (extra != null) for (Enumeration e = extra.elements(); e.hasMoreElements();)
        {
            perms.add((Permission) e.nextElement());
        }

        doTestParsePRF_string(monappTag + typeTag, true, DUALSIGNED_ID, SC_ID, perms, exclPerms);
    }

    /**
     * Test parsing of <ocap:registeredapi.user name="..."/>.
     */
    public void testParsePRF_registeredApiUser() throws Exception
    {
        String[] apis = { "api1", "a", "b", "1234" };
        doTestParsePRF_registeredApiUser_value(true, apis);
        doTestParsePRF_registeredApiUser_value(false, apis);
    }

    /**
     * Implements PRF parsing of &lt;ocap:registeredapi.user&gt; test. Tests
     * multiple RegisteredApiUserPermissions
     * 
     * @param ocapApp
     *            true if OCAP permissions should be supported
     * @param names
     *            the name attribute values
     */
    private void doTestParsePRF_registeredApiUser_value(boolean ocapApp, String[] names) throws Exception
    {
        String tag = "";

        for (int i = 0; i < names.length; ++i)
        {
            String name = names[i];
            tag += "\n" + "<ocap:registeredapi.user " + "name=\"" + name + "\" " + " />";
        }

        PermissionCollection perms = buildBaseExpectedPermissions(SC_ID, SIGNED_ID);
        if (ocapApp)
        {
            for (int i = 0; i < names.length; ++i)
                perms.add(new RegisteredApiUserPermission(names[i]));
        }

        doTestParsePRF_string(tag, ocapApp, false, SIGNED_ID, SC_ID, perms, null);
    }

    class ShortedInputStream extends FilterInputStream
    {
        private int avail = 15;

        public ShortedInputStream(InputStream is)
        {
            super(is);
        }

        public ShortedInputStream(int avail, InputStream is)
        {
            super(is);
            this.avail = avail;
        }

        public int available()
        {
            return avail;
        }

        int fail() throws IOException
        {
            throw new IOException("FAIL");
        }

        public int read() throws IOException
        {
            if (avail-- > 0)
                return super.read();
            else
                return fail();
        }

        public int read(byte[] b) throws IOException
        {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int ofs, int len) throws IOException
        {
            if (avail - len > 0)
            {
                int bytes = super.read(b, ofs, len);
                avail -= bytes;
                return bytes;
            }
            else
                return fail();
        }

        public long skip(long n) throws IOException
        {
            if (avail > 0)
            {
                long bytes = super.skip(n);
                avail -= (int) bytes;
                return bytes;
            }
            else
                return fail();
        }
    }

    /**
     * Tests parsePermissionRequest() propagation of IOExceptions.
     */
    public void testParsePRF_IO()
    {
        byte[] prfData = permissionRequestFile(SIGNED_ID, "<file/>").getBytes();
        prfData[0] = (byte) 0xFF;

        try
        {
            xmlmgr.parsePermissionRequest(prfData, true, true, null, SC_ID);
            fail("Expected IOException to be propagated");
        }
        catch (IOException e)
        { /* expected */
        }
    }

    /**
     * Tests parsePermissionRequest() handles incorrect orgid or appid
     */
    public void testParsePRF_BadAppID() throws IOException
    {
        String[] names = { "api1", "a", "b", "1234" };
        String tag = "";
        for (int i = 0; i < names.length; ++i)
        {
            tag += "\n" + "<ocap:registeredapi.user " + "name=\"" + names[i] + "\" " + " />";
        }

        byte[] prfData = permissionRequestFile(SIGNED_ID, tag).getBytes();
        PermissionCollection parsed = xmlmgr.parsePermissionRequest(prfData, true, false, SIGNED_ID, SC_ID);
        assertNotNull("Expected parsePermissionRequest() to succeed", parsed);

        prfData = permissionRequestFile(SIGNED_ID, tag).getBytes();
        AppID badOID = new AppID(DUALSIGNED_ID.getOID(), SIGNED_ID.getAID());
        parsed = xmlmgr.parsePermissionRequest(prfData, true, false, badOID, SC_ID);
        assertNull("Expected parsePermissionRequest() to fail due to wrong OID", parsed);

        prfData = permissionRequestFile(SIGNED_ID, tag).getBytes();
        AppID badAID = new AppID(SIGNED_ID.getOID(), DUALSIGNED_ID.getAID());
        parsed = xmlmgr.parsePermissionRequest(prfData, true, false, badAID, SC_ID);
        assertNull("Expected parsePermissionRequest() to fail due to wrong AID", parsed);
    }

    // **************************** App Description
    // ********************************

    /**
     * Tests parseAppDescription().
     */
    /*
     * public void testParseAppDescription() throws Exception {
     * AppDescriptionInfo.FileInfo[] files = { new FileInfo("gits2.xml",
     * 12345L), new DirInfo("batou", new FileInfo[] { new FileInfo("abcd", 2L),
     * new FileInfo("wxyz", 20L), new FileInfo("water\u6C34", 100L) }), new
     * DirInfo("major", new AppDescriptionInfo.FileInfo[] { new FileInfo("*",
     * 99L) }), new FileInfo("water\u6C34", 101L), new DirInfo("kusanagi", new
     * AppDescriptionInfo.FileInfo[] { new DirInfo("Tachikoma", new
     * AppDescriptionInfo.FileInfo[] { new FileInfo("*", -1L) }), new
     * DirInfo("major", new AppDescriptionInfo.FileInfo[] { new FileInfo("abcd",
     * 27L), new FileInfo("ABCD", 99L), new FileInfo("batou", 28L), }) }), new
     * FileInfo("puppet-master", 13L), new DirInfo("pailof\u6C34", new
     * AppDescriptionInfo.FileInfo[0]), }; AppDescriptionInfo orig = new
     * AppDesc(files);
     * 
     * InputStream is = createInputStream(createAppDesc(orig, true));
     * 
     * AppDescriptionInfo parsed = xmlmgr.parseAppDescription(is);
     * 
     * // Compare resulting AppDescription
     * assertNotNull("null should not be returned", parsed);
     * assertEquals("Unexpected AppDesc returned:", orig, parsed); }
     */

    /**
     * Tests parseAppDescription() fails appropriately given invalid names.
     * <ul>
     * <li>Is "." or ".."
     * <li>Contains "*" but isn't "*"
     * <li>??Contains unescaped reserved characters?? (not tested currently)
     * </ul>
     */
    /*
     * public void testParseAppDescription_invalidName() throws Exception {
     * String[] invalidNames = { ".", "..", "stuff*", "*stuff", "stu*ff", "x.*",
     * "*.x", };
     * 
     * // Try invalid file names... for(int i = 0; i < invalidNames.length; ++i)
     * { AppDescriptionInfo.FileInfo[] files = { new DirInfo("batou", new
     * FileInfo[] { new FileInfo("abcd", 2L), new FileInfo("wxyz", 20L), }), new
     * FileInfo(invalidNames[i], 12345L), new DirInfo("major", new
     * AppDescriptionInfo.FileInfo[] { new FileInfo("*", 99L) }), new
     * FileInfo("tachikoma", 101L), }; AppDescriptionInfo orig = new
     * AppDesc(files);
     * 
     * InputStream is = createInputStream(createAppDesc(orig, true));
     * 
     * try { xmlmgr.parseAppDescription(is);
     * fail("Expected IOException for invalid file name ('"
     * +invalidNames[i]+"') to result in null AppDesc"); } catch(IOException e)
     * {
     * 
     * } }
     * 
     * // Try invalid dir names... for(int i = 0; i < invalidNames.length; ++i)
     * { AppDescriptionInfo.FileInfo[] files = { new DirInfo("batou", new
     * FileInfo[] { new FileInfo("abcd", 2L), new FileInfo("wxyz", 20L), }), new
     * FileInfo("gits.sac", 12345L), new DirInfo(invalidNames[i], new
     * AppDescriptionInfo.FileInfo[] { new FileInfo("*", 99L) }), new
     * FileInfo("tachikoma", 101L), }; AppDescriptionInfo orig = new
     * AppDesc(files);
     * 
     * InputStream is = createInputStream(createAppDesc(orig, true));
     * 
     * try { xmlmgr.parseAppDescription(is);
     * fail("Expected IOException for invalid dir name ('"
     * +invalidNames[i]+"') to result in null AppDesc"); } catch(IOException e)
     * {
     * 
     * } } }
     */

    // **************************** Support Stuff
    // ********************************

    /**
     * Creates an input stream from the given String.
     */
    private static InputStream createInputStream(String string)
    {
        if (DEBUG)
        {
            System.out.println("START================================");
            System.out.println(string);
            System.out.println("END==================================");
        }

        byte[] bytes = string.getBytes();
        return new ByteArrayInputStream(bytes);
    }

    private static final String PRF_header = "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE permissionrequestfile PUBLIC \"-//OCAP//DTD Permission Request File 1.0//EN\" "
            + "\"http://www.opencable.com/ocap/dtd/ocappermissionrequestfile-1-0.dtd\">\n";

    /**
     * Creates a Font Index from the given FontInfo data.
     * 
     * @param info
     *            the FontInfo data
     */
    private static String createFontIndex(XmlManager.FontInfo[] info)
    {
        return createFontIndex(info, false);
    }

    /**
     * Creates a Font Index from the given FontInfo data.
     * 
     * @param info
     *            the FontInfo data
     * @param junk
     *            if true then introduce extraneous tags and attributes (to be
     *            ignored)
     */
    private static String createFontIndex(XmlManager.FontInfo[] info, boolean junk)
    {
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<!DOCTYPE fontdirectory PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" ").append(
                "\"http://www.opencable.com/ocap/dtd/fontdirectory-1-0.dtd\">\n");
        // DVB
        /*
         * sb.append("<!DOCTYPE fontdirectory PUBLIC \"-//DVB//DTD Font Directory 1.0//EN\" "
         * ) .append("\"http://www.dvb.org/mhp/dtd/fontdirectory-1-0.dtd\">\n");
         */

        if (junk)
            sb.append("<fontdirectory junk=\"0\"> junk text\n");
        else
            sb.append("<fontdirectory>\n");

        for (int i = 0; i < info.length; ++i)
        {
            if (junk)
                sb.append("<font junk=\"0\"> junk text\n");
            else
                sb.append("<font>\n");

            if (junk) sb.append("\t<junk>other junk we don't need</junk>\n");

            // name
            if (info[i].name != null)
            {
                if (junk)
                    sb.append("<name junk=\"0\">");
                else
                    sb.append("<name>");
                if (junk) sb.append("<junk>other junk we don't \nneed</junk>");

                sb.append(info[i].name).append("</name>\n");
            }
            // format
            if (info[i].format != null)
            {
                if (junk)
                    sb.append("<fontformat junk=\"0\">");
                else
                    sb.append("<fontformat>");
                if (junk) sb.append("<junk>other junk we don't need\n</junk>");

                sb.append(info[i].format).append("</fontformat>\n");
            }
            // filename
            if (info[i].filename != null)
            {
                if (junk)
                    sb.append("<filename junk=\"junk\">");
                else
                    sb.append("<filename>");
                if (junk) sb.append("<junk>other \njunk we don't need</junk>");

                sb.append(info[i].filename).append("</filename>\n");
            }
            // size
            if (info[i].min != -1 && info[i].max != -1)
            {
                sb.append("<size ");
                if (junk) sb.append("junk1=\"blah\" ");
                if (info[i].min != -2)
                {
                    sb.append("min=\"");
                    switch (info[i].min)
                    {
                        case -3:
                            sb.append("xyz"); // not a number
                            break;
                        case Integer.MIN_VALUE:
                            sb.append("minint"); // invalid
                            break;
                        default:
                            sb.append(Integer.toString(info[i].min));
                            break;
                    }
                    sb.append("\" ");
                }
                if (junk) sb.append("junk2=\"blah\" ");
                if (info[i].max != -2)
                {
                    sb.append("max=\"");
                    switch (info[i].max)
                    {
                        case -3:
                            sb.append("xyz"); // not a number
                            break;
                        case Integer.MAX_VALUE:
                            sb.append("maxint");
                            break;
                        default:
                            sb.append(Integer.toString(info[i].max));
                            break;
                    }
                    sb.append("\" ");
                }
                if (junk) sb.append("junk3=\"blah\" ");
                sb.append("/>\n");
            }
            // style
            if (info[i].style != null)
            {
                for (int style = 0; style < info[i].style.length(); ++style)
                {
                    if (!info[i].style.get(style)) continue;
                    if (junk)
                        sb.append("<style junk=\"0\"><junk> junk \ntext </junk>");
                    else
                        sb.append("<style>");
                    switch (style)
                    {
                        case Font.PLAIN:
                            sb.append("PLAIN");
                            break;
                        case Font.BOLD:
                            sb.append("BOLD");
                            break;
                        case Font.ITALIC:
                            sb.append("ITALIC");
                            break;
                        case (Font.BOLD | Font.ITALIC):
                            sb.append("BOLD_ITALIC");
                            break;
                        default:
                            fail("Could generate test data");
                    }
                    if (junk) sb.append("<junk>other junk \n we don't need</junk>");
                    sb.append("</style>\n");
                }
            }

            if (junk) sb.append("\t<junk>other junk we don't need</junk>\n");
            sb.append("</font>\n");
        }

        if (junk) sb.append("\t<junk>other junk we don't need</junk>");
        sb.append("</fontdirectory>\n");

        return sb.toString();
    }

    /**
     * Extension of BitSet tailored to storing a set of styles.
     */
    private static class StyleSet extends BitSet
    {
        public StyleSet(int style)
        {
            this();
            set(style);
        }

        public StyleSet(int[] styles)
        {
            this();
            for (int i = 0; i < styles.length; ++i)
                set(styles[i]);
        }

        public StyleSet()
        {
            super(4);
        }
    }

    private static final int[] ALL_STYLES = { Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD + Font.ITALIC };

    private static final BitSet allStyles = new StyleSet(ALL_STYLES);

    /**
     * Creates an app description file from the given AppDescriptionInfo data.
     * All name strings are escaped properly.
     * 
     * @param info
     *            the AppDesc data
     */
    public static String createAppDesc(AppDescriptionInfo info)
    {
        return createAppDesc(info, true);
    }

    /**
     * Creates an app description file from the given AppDescriptionInfo data.
     * 
     * @param info
     *            the AppDesc data
     * @param escape
     *            if true, then name strings should be escaped properly
     */
    private static String createAppDesc(AppDescriptionInfo info, boolean escape)
    {
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<!DOCTYPE applicationdescription PUBLIC \"-//OCAP//DTD Font Directory 1.0//EN\" ").append(
                "\"http://www.opencable.com/ocap/dtd/applicationdescriptionfile-1-0.dtd\">\n");

        sb.append("<applicationdescription>\n");
        for (int i = 0; i < info.files.length; ++i)
        {
            dumpFileInfo(sb, info.files[i], escape);
        }
        sb.append("</applicationdescription>\n");

        return sb.toString();
    }

    private static void dumpFileInfo(StringBuffer sb, AppDescriptionInfo.FileInfo file, boolean escape)
    {
        if (file instanceof AppDescriptionInfo.DirInfo)
        {
            dumpFileInfo(sb, (AppDescriptionInfo.DirInfo) file, escape);
        }
        else
        {
            sb.append("<file name=\"").append(escape(file.name, escape)).append("\" size=\"").append(file.size).append(
                    "\" />\n");
        }
    }

    private static void dumpFileInfo(StringBuffer sb, AppDescriptionInfo.DirInfo dir, boolean escape)
    {
        sb.append("<dir name=\"").append(escape(dir.name, escape)).append("\" >\n");

        for (int i = 0; i < dir.files.length; ++i)
            dumpFileInfo(sb, dir.files[i], escape);

        sb.append("</dir>\n");
    }

    private static final String PCHAR = "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789"
            + "-_.!~*'()" + ":@&=+$,";

    private static String escape(String str, boolean escape)
    {
        return !escape ? str : escape(str);
    }

    /**
     * Escapes pchars as necessary.
     * 
     * <pre>
     * pchar = unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ","
     * UNRESERVED = alphanum | mark
     * 
     * alphanum = alpha | digit
     * alpha = lowalpha | upalpha
     * lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k"
     *          | "l" | "m" | "n" | "o" | "p" | "q" | "r" | "s" | "t" | "u"
     *          | "v" | "w" | "x" | "y" | "z"
     * upalpha = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K"
     *         | "L" | "M" | "N" | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
     *         | "V" | "W" | "X" | "Y" | "Z"
     * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
     * </pre>
     * 
     * @param str
     * @return escaped version of str
     */
    private static String escape(String str)
    {
        StringBuffer sb = new StringBuffer(str.length());
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; ++i)
        {
            if (PCHAR.indexOf(chars[i]) >= 0)
            {
                sb.append(chars[i]);
            }
            else
            {
                // Get UTF-8 for char[i]
                String string = new String(new char[] { chars[i] });
                byte utf8[];
                try
                {
                    utf8 = string.getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    // encoding failed. In this case just put in the char value.
                    utf8 = string.getBytes();
                }
                for (int j = 0; j < utf8.length; j++)
                {
                    sb.append('%');
                    if ((utf8[j] & 0xff) < 0xf)
                    {
                        sb.append('0');
                    }
                    sb.append(Integer.toHexString(utf8[j] & 0xff));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Tests two FontInfo objects.
     */
    public static void assertEquals(String pfx, XmlManager.FontInfo expected, XmlManager.FontInfo actual)
    {
        // System.out.println("expected: "+FontInfo.toString(expected));
        // System.out.println("actual: "+FontInfo.toString(actual));

        assertNotNull(pfx + ": expected non-null", actual);

        int min = (expected.min < 0) ? 0 : expected.min;
        int max = (expected.max < 0) ? Integer.MAX_VALUE : expected.max;
        assertEquals(pfx + ": minimum size", min, actual.min);
        assertEquals(pfx + ": maximum size", max, actual.max);

        assertNotNull(pfx + ": null name", actual.name);
        assertEquals(pfx + ": name", expected.name, actual.name);

        assertNotNull(pfx + ": null format", actual.format);
        assertEquals(pfx + ": format", expected.format, actual.format);

        assertNotNull(pfx + ": null filename", actual.filename);
        assertEquals(pfx + ": filename", expected.filename, actual.filename);

        BitSet style = (expected.style != null) ? expected.style : allStyles;
        assertNotNull(pfx + ": null style", actual.style);
        assertEquals(pfx + ": styles", style, actual.style);
    }

    /**
     * Extension of standard FontInfo which provides toString() and a
     * specialized constructor.
     */
    private static class FontInfo extends XmlManager.FontInfo
    {
        public FontInfo(String name, String format, String filename, BitSet style, int min, int max)
        {
            this.name = name;
            this.format = format;
            this.filename = filename;
            this.style = style;
            this.min = min;
            this.max = max;
        }

        /*
         * public boolean equals(Object o) { XmlManager.FontInfo fi; if ((o
         * instanceof XmlManager.FontInfo) && (fi = (XmlManager.FontInfo)o) !=
         * null && min == fi.min && max == fi.max && (name != null || fi.name ==
         * null || name.equals(fi.name)) && (format != null || fi.format == null
         * || format.equals(fi.format)) && (filename != null || fi.filename ==
         * null || filename.equals(fi.filename))) { if (style == null) return
         * fi.style == null; if (style.length != fi.style.length) return false;
         * BitSet set1 = new BitSet(); BitSet set2 = new BitSet(); for(int i =
         * 0; i < style.length; ++i) { set1.set(style[i]);
         * set2.set(fi.style[i]); } return set1.equals(set2); } return false; }
         * public int hashCode() { return min ^ max ^ (name == null ? 0 :
         * name.hashCode()) ^ (format == null ? 0 : format.hashCode()); }
         */

        public String toString()
        {
            return toString(this);
        }

        public static String toString(XmlManager.FontInfo info)
        {
            String str = "FontInfo[" + info.name + "," + info.filename + "," + info.min + "-" + info.max;
            if (info.style != null)
            {
                for (int style = 0; style < info.style.length(); ++style)
                {
                    switch (style)
                    {
                        case Font.PLAIN:
                            str += ",PLAIN";
                            break;
                        case Font.ITALIC:
                            str += ",ITALIC";
                            break;
                        case Font.BOLD:
                            str += ",BOLD";
                            break;
                        case Font.ITALIC | Font.BOLD:
                            str += ",BOLD_ITALIC";
                            break;
                        default:
                            str += "?" + style + "?";
                            break;
                    }
                }
            }
            return str + "]";
        }
    }

    /**
     * Tests two AppDescriptionInfo objects.
     */
    public static void assertEquals(String pfx, AppDescriptionInfo expected, AppDescriptionInfo actual)
    {
        assertEquals(pfx, expected.files, actual.files);
    }

    public static void assertEquals(String pfx, AppDescriptionInfo.FileInfo[] expected,
            AppDescriptionInfo.FileInfo[] actual)
    {
        if (expected == actual) return;
        assertNotNull(pfx + " should not be null", actual);
        assertEquals(pfx + " incorrect length", expected.length, actual.length);

        for (int i = 0; i < expected.length; ++i)
        {
            assertEquals(pfx + expected[i].name, expected[i], actual[i]);
        }
    }

    public static void assertEquals(String pfx, AppDescriptionInfo.FileInfo expected, AppDescriptionInfo.FileInfo actual)
    {
        assertEquals(pfx + " wrong name", expected.name, actual.name);
        if (expected instanceof AppDescriptionInfo.DirInfo)
        {
            assertTrue(pfx + " not instanceof DirInfo", actual instanceof AppDescriptionInfo.DirInfo);

            assertEquals(pfx + "/", ((AppDescriptionInfo.DirInfo) expected).files,
                    ((AppDescriptionInfo.DirInfo) actual).files);
        }
        else if (!expected.name.equals("*"))
        {
            assertEquals(pfx + " bad size", expected.size, actual.size);
        }
    }

    /*
     * private static class FileInfo extends AppDescriptionInfo.FileInfo {
     * public FileInfo(String name, long size) { this.name = name; this.size =
     * size; } }
     * 
     * private static class DirInfo extends AppDescriptionInfo.DirInfo { public
     * DirInfo(String name, AppDescriptionInfo.FileInfo[] files) { this.name =
     * name; this.files = files; } }
     */

    private static class AppDesc extends AppDescriptionInfo
    {
        public AppDesc(AppDescriptionInfo.FileInfo[] files)
        {
            this.files = files;
        }
    }

    /* Boilerplate */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(XmlManagerTest.class);
        suite.setName(XmlManager.class.getName());
        return suite;
    }

    public XmlManagerTest(String name, ImplFactory f)
    {
        super(name, XmlManager.class, f);
    }

    protected XmlManager createXmlManager()
    {
        return (XmlManager) createManager();
    }

    private XmlManager xmlmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        xmlmgr = (XmlManager) mgr;

        if (DEBUG) System.out.println(getName());
    }

    protected void tearDown() throws Exception
    {
        xmlmgr = null;
        super.tearDown();
    }

    private static final boolean DEBUG = false;
}
