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

package org.ocap.application;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.ProxySecurityManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.BitSet;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.CRC32;

import javax.tv.service.SIManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.DVBJProxy;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.dvb.application.AppProxyTest.Listener;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.system.MonitorAppPermission;

/*
 * How should this class be tested?
 * In a manner similar to AppsDatabase...?
 * Do we test basic functionality and then extended functionality for an implementation?
 */

// TODO: add tests for setApplicationPriority() (including Security tests)

/**
 * Tests AppManagerProxy interface.
 */
public class AppManagerProxyTest extends TestCase
{
    /**
     * Enables kludge to enable passing of tests... Current implementation of
     * unregisterUnboundApp() results in asynchronous change of state for app
     * (e.g., NOT_LOADED->DESTROYED or STARTED->DESTROYED). So, it might not be
     * complete by the time we test. Is this correct or not?
     */
    private static final boolean KLUDGE = true;

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        AppManagerProxy amp = AppManagerProxy.getInstance();

        assertNotNull("getInstance() should not return null", amp);
        assertSame("getInstance() should return a singleton", amp, AppManagerProxy.getInstance());
    }

    /**
     * Tests the reading and writing of addressing properties to persistent
     * storage. This must be the first addressing property test in this JUnit
     */
    public void testPersistentAddressingPropertiesStartup()
    {
        PersistentProperty[] props = readPersistentPropertyFile();
        if (props != null)
        {
            for (int i = 0; i < props.length; ++i)
            {
                assertTrue("A persistent property was expected, but not found!",
                        appmanagerproxy.getAddressingProperties().getProperty(props[i].key).equals(props[i].value));
            }
        }
    }

    /**
     * Test the setting of new property with a valid expiration date
     */
    public void testRegisterAddressingProperties_validPersistent()
    {
        // Test with expiration date in the past
        Properties p1 = new Properties();
        p1.setProperty("propkey1", "propvalue1");
        p1.setProperty("propkey2", "propvalue2");

        appmanagerproxy.registerAddressingProperties(p1, true, new Date(System.currentTimeMillis() + 800000));

        // Ensure that these properties are all found in the persistent file
        PersistentProperty[] props = readPersistentPropertyFile();
        if (props != null)
        {
            // Ensure that they are not persisted
            boolean found1 = false;
            boolean found2 = false;
            for (int i = 0; i < props.length; ++i)
            {
                if (props[i].key.equals("propkey1") && props[i].value.equals("propvalue1")) found1 = true;
                if (props[i].key.equals("propkey2") && props[i].value.equals("propvalue2")) found2 = true;
            }
            assertTrue("propkey1 should have persisted with valid expiration date!", found1);
            assertTrue("propkey2 should have persisted with valid expiration date!", found2);
        }
        else
        {
            fail("Persistent properties file not read!");
        }

        // Ensure that they are registered
        Properties regProps = appmanagerproxy.getAddressingProperties();
        String propVal = regProps.getProperty("propkey1");
        assertTrue("propkey1 should have been registered!", propVal != null);
        assertTrue("propkey1 should have correct value!", propVal.equals("propvalue1"));
        propVal = regProps.getProperty("propkey2");
        assertTrue("propkey2 should have been registered!", propVal != null);
        assertTrue("propkey2 should have corect value!", propVal.equals("propvalue2"));

        // Remove the properties to cleanup
        appmanagerproxy.removeAddressingProperties(new String[] { "propkey1", "propkey2" });
        // Make sure they no longer exist
        regProps = appmanagerproxy.getAddressingProperties();
        propVal = regProps.getProperty("propkey1");
        assertTrue("propkey1 should have been unregistered!", propVal == null);
        propVal = regProps.getProperty("propkey2");
        assertTrue("propkey2 should have been unregistered!", propVal == null);
    }

    /**
     * Test the setting of new property with expired expiration date
     */
    public void testRegisterAddressingProperties_exprDateInThePast()
    {
        // Test with expiration date in the past
        Properties p1 = new Properties();
        p1.setProperty("propkey1", "propvalue1");
        p1.setProperty("propkey2", "propvalue2");

        appmanagerproxy.registerAddressingProperties(p1, true, new Date(System.currentTimeMillis()));

        // Ensure that none of these properties are found in the persistent
        // file
        PersistentProperty[] props = readPersistentPropertyFile();
        if (props != null)
        {
            // Ensure that they are not persisted
            for (int i = 0; i < props.length; ++i)
            {
                assertFalse("propkey1 should not have persisted with expiration date in the past!",
                        props[i].key.equals("propkey1"));
                assertFalse("propkey2 should not have persisted with expiration date in the past!",
                        props[i].key.equals("propkey2"));
            }
        }

        // Ensure that they are registered
        Properties regProps = appmanagerproxy.getAddressingProperties();
        String propVal = regProps.getProperty("propkey1");
        assertTrue("propkey1 should have been registered!", propVal != null);
        assertTrue("propkey1 should have correct value!", propVal.equals("propvalue1"));
        propVal = regProps.getProperty("propkey2");
        assertTrue("propkey2 should have been registered!", propVal != null);
        assertTrue("propkey2 should have corect value!", propVal.equals("propvalue2"));

        // Remove the properties to cleanup
        appmanagerproxy.removeAddressingProperties(new String[] { "propkey1", "propkey2" });
    }

    /**
     * Test the setting of new property with expired expiration date
     */
    public void testRegisterAddressingProperties_noPersist()
    {
        // Test with persistence set to false, but a valid expiration date
        Properties p1 = new Properties();
        p1.setProperty("propkey1", "propvalue1");
        p1.setProperty("propkey2", "propvalue2");

        appmanagerproxy.registerAddressingProperties(p1, false, new Date(System.currentTimeMillis() + 200000));

        // Ensure that none of these properties are found in the persistent
        // file
        PersistentProperty[] props = readPersistentPropertyFile();
        if (props != null)
        {
            // Ensure that they are not persisted
            for (int i = 0; i < props.length; ++i)
            {
                assertFalse("propkey1 should not have persisted with expiration date in the past!",
                        props[i].key.equals("propkey1"));
                assertFalse("propkey2 should not have persisted with expiration date in the past!",
                        props[i].key.equals("propkey2"));
            }
        }

        // Ensure that they are registered
        Properties regProps = appmanagerproxy.getAddressingProperties();
        String propVal = regProps.getProperty("propkey1");
        assertTrue("propkey1 should have been registered!", propVal != null);
        assertTrue("propkey1 should have correct value!", propVal.equals("propvalue1"));
        propVal = regProps.getProperty("propkey2");
        assertTrue("propkey2 should have been registered!", propVal != null);
        assertTrue("propkey2 should have corect value!", propVal.equals("propvalue2"));

        // Cleanup
        appmanagerproxy.removeAddressingProperties(new String[] { "propkey1", "propkey2" });
    }

    /**
     * Test the setting of new property that is a Java system property (invalid)
     */
    public void testRegisterAddressingProperties_invalidProps()
    {
        // Test with invalid property names
        Properties p1 = new Properties();
        p1.setProperty("ocap.memory.total", "propvalue1");
        p1.setProperty("ocap.system.highdef", "propvalue2");

        appmanagerproxy.registerAddressingProperties(p1, false, new Date(System.currentTimeMillis() + 200000));

        // Make sure they are not registered
        Properties regProps = appmanagerproxy.getAddressingProperties();
        String propVal = regProps.getProperty("propkey1");
        assertTrue("propkey1 should not have been registered!", propVal == null);
        propVal = regProps.getProperty("propkey2");
        assertTrue("propkey2 should not have been registered!", propVal == null);
    }

    /**
     * Reads in the current contents of the persistent properties file and
     * returns and valid persistent properties contained therein.
     * 
     * @return a list of valid properties found in the persistent properties
     *         file, or null if the file does not exist. If an error was
     *         encountered while reading from the file, a JUnit failure is
     *         indicated and null is returned
     */
    private PersistentProperty[] readPersistentPropertyFile()
    {
        Vector props = new Vector();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(persistentPropertiesFile));

            while (true)
            {
                // Read in the first value for this entry. Null indicates
                // proper end-of-file
                String key = reader.readLine();
                if (key == null) break;

                // Read in rest of values, break on premature end-of-file
                String propVal = reader.readLine();
                String date = reader.readLine();
                if (propVal == null || date == null)
                {
                    fail("Premature end of persistent properties file!");
                    return null;
                }

                // Expiration Date
                boolean persist = true;
                long dateMillis = Long.parseLong(date);
                if (dateMillis != -1 && dateMillis < System.currentTimeMillis()) persist = false; // Persistence
                                                                                                  // is
                                                                                                  // expired

                if (persist)
                {
                    PersistentProperty p = new PersistentProperty();
                    p.key = key;
                    p.value = propVal;
                    if (dateMillis != -1) p.expiration = new Date(dateMillis);
                    props.add(p);
                }
            }
            reader.close();
        }
        catch (IOException e)
        {
            return null;
        }
        finally
        {
            if (reader != null) try
            {
                reader.close();
            }
            catch (IOException e)
            {
            }
        }

        PersistentProperty[] retVal = new PersistentProperty[props.size()];
        props.copyInto(retVal);
        return retVal;
    }

    private class PersistentProperty
    {
        public String key;

        public String value;

        public Date expiration;
    }

    /**
     * Verify the permission. Used to verify the permission request submitted to
     * the SecurityManager.
     */
    private void verifyPermission(Permission p, String name)
    {
        assertNotNull("non-null Permission should be checked with SecurityManager", p);
        assertTrue("Permission should be MonitorAppPermission", p instanceof MonitorAppPermission);
        assertEquals("Permission name should be", name, ((MonitorAppPermission) p).getName());
    }

    /**
     * Tests registerUnboundApp. This test attempts to register a bunch of apps
     * and then retrieve the AppProxy objects.
     */
    public void XtestRegisterUnboundApp() throws Exception
    {
        // TODO(AaronK): SIManager won't get stuff unless ServiceManager is
        // started!!!!
        // We don't want it to start up... it may start launching apps!!!!!
        // We could go directly to the SignallingManager... except then we may
        // be getting REAL signalling!
        // That's probably best...

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        final int FIRST = 1;
        final int MID = 5;
        final int LAST = 10;
        int svc = 0x200000;

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            // Register a bunch of apps...
            TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
            TestApp info[] = new TestApp[FIRST + LAST - FIRST + 1];
            XAITGenerator gen = new XAITGenerator();
            gen.add(svcinfo);
            for (int i = FIRST; i < MID; ++i)
                gen.add(info[i] = new TestApp(new AppID(10, i), svc));
            sm.p = null;
            appmanagerproxy.registerUnboundApp(gen.generate());
            verifyPermission(sm.p, "registrar");
            for (int i = MID; i < LAST + 1; ++i)
            {
                gen = new XAITGenerator();
                gen.add(new TestSvc(svc + i, "FLCL_" + i, false));
                gen.add(info[i] = new TestApp(new AppID(10, i), svc + i));
                sm.p = null;
                appmanagerproxy.registerUnboundApp(gen.generate());
                verifyPermission(sm.p, "registrar");
            }

            // Verify that the AbstractService w/ apps can be found
            Hashtable apps = getAppsInService(svc);
            for (int i = MID; i < LAST + 1; ++i)
                getAppsInService(svc + i, apps);
            for (int i = FIRST; i < LAST + 1; ++i)
            {
                AppID id = new AppID(10, i);
                OcapAppAttributes attrib = (OcapAppAttributes) apps.get(id);
                assertNotNull("AppAttributes should be in db for " + id, attrib);
                assertNotNull("Test internal error", info[i]);
                assertEquals("Unexpected appid", info[i].getAppID(), attrib.getIdentifier());
                assertEquals("Unexpected controlCode", info[i].getControlCode(), attrib.getApplicationControlCode());
                assertEquals("Unexpected priority", info[i].getPriority(), attrib.getPriority());
                assertEquals("Unexpected type", OcapAppAttributes.OCAP_J, attrib.getType());
                assertEquals("Unexpected name", info[i].getName(), attrib.getName());
                assertEquals("Unexpected basedir", info[i].getBaseDir(), attrib.getProperty("dvb.j.location.base"));
                assertEquals("Unexpected storage priority", info[i].getStoragePriority(), attrib.getStoragePriority());
                assertEquals("Unexpected isServiceBound", false, attrib.getIsServiceBound());
                // TODO Check icon
                // TODO Check service locator
                // TODO Check getProperty
            }
        }
        finally
        {
            // cleanup
            for (int i = FIRST; i < MID; ++i)
                appmanagerproxy.unregisterUnboundApp(svc, new AppID(10, i));
            for (int i = MID; i < LAST + 1; ++i)
                appmanagerproxy.unregisterUnboundApp(svc + i, new AppID(10, i));
            ProxySecurityManager.pop();
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
        }
    }

    private Hashtable getAppsInService(int svcId) throws Exception
    {
        return getAppsInService(svcId, null);
    }

    private Hashtable getAppsInService(int svcId, Hashtable apps) throws Exception
    {
        SIManager simgr = SIManager.createInstance();
        AbstractService service = (AbstractService) simgr.getService(new OcapLocator(svcId));
        assertNotNull("Registered service should be found in SIManager", service);
        if (apps == null) apps = new Hashtable();
        for (Enumeration e = service.getAppAttributes(); e.hasMoreElements();)
        {
            OcapAppAttributes attr = (OcapAppAttributes) e.nextElement();
            apps.put(attr.getIdentifier(), attr);
        }
        return apps;
    }

    /**
     * Tests updates/signalling of apps.
     */
    public void XtestRegisterUnboundApp_update()
    {
        // TODO(AaronK): updates/signalling of apps
        // Verify that, if so signalled, they can be autoloaded...
        // Check that they can be loaded
        // Reregister them with control code to cause state change
        // Verify that their states change
        // !!!! FINISH
        fail("Unimplemented test");
    }

    /**
     * Tests multiple application_information_sections.
     */
    public void XtestRegisterUnboundApp_multipleSections() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        // Create several XAITGenerators
        XAITGenerator gen[] = new XAITGenerator[3];
        TestApp info[][] = new TestApp[gen.length][];

        int svc = 0xa00000;
        int oid = 20;
        int aid = 1;
        for (int i = 0; i < gen.length; ++i)
        {
            gen[i] = new XAITGenerator();
            gen[i].add(new TestSvc(svc + i, "flcl", false));
            info[i] = new TestApp[i + 1];
            for (int j = 0; j < info[i].length; ++j)
            {
                info[i][j] = new TestApp(new AppID(oid + i, aid + j), svc + i);
                gen[i].add(info[i][j]);
            }
        }

        try
        {
            // Concatenate all of the InputStreams
            InputStream is = gen[0].generate();
            for (int i = 1; i < gen.length; ++i)
                is = new SequenceInputStream(is, gen[i].generate());

            // Register the apps
            appmanagerproxy.registerUnboundApp(is);

            // Verify that they apps are there
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            for (int i = 0; i < info.length; ++i)
                for (int j = 0; j < info[i].length; ++j)
                {
                    AppID id = info[i][j].getAppID();
                    AppProxy app = db.getAppProxy(id);
                    assertNotNull("An appProxy should be returned for " + id, app);
                }
        }
        finally
        {
            for (int i = 0; i < info.length; ++i)
                for (int j = 0; j < info[i].length; ++j)
                {
                    int svcid = info[i][j].getServiceID();
                    AppID appid = info[i][j].getAppID();
                    appmanagerproxy.unregisterUnboundApp(svcid, appid);
                }
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
        }
    }

    /**
     * Tests generation of IllegalArgumentException.
     * 
     * According to MHP 10.4.1 Data Errors:
     * 
     * AITs which contain errors shall be processed as follows:
     * 
     * <ul>
     * <li>An error in a descriptor shall result in that descriptor being
     * silently discarded. Processing of that descriptor loop shall continue
     * with the next descriptor (if any). The scope of error detection of a
     * descriptor should be limited to the application information section in
     * which it is carried.
     * 
     * <li>An error in an application loop outside a descriptor shall result in
     * that entry in the application loop being silently discarded. Processing
     * of that application loop shall continue with the next entry (if any).
     * <p>
     * NOTE: The consequence of the above is that an error in a mandatory
     * descriptor which results in that descriptor being silently ignored may
     * then result in a application loop which is missing such a mandatory
     * descriptor. Hence that application loop shall also be silently ignored.
     * <li>An error in an application information section outside of an
     * application loop shall result in that entire application information
     * section being silently discarded. Processing of the AIT shall continue
     * with the next application information section (if any). </ol>
     * 
     * Since errors in descriptors or an application loop shall be, more or
     * less, ignored, it seems that the only real error is if no
     * application_information_section is found (or perhaps something else is).
     */
    public void testRegisterUnboundApp_IllegalArgumentException() throws Exception
    {
        // Null data
        try
        {
            appmanagerproxy.registerUnboundApp(null);
            fail("Expected IllegalArgumentException for registerUnboundApp(null)");
        }
        catch (NullPointerException e)
        { /* empty */
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_empty() throws Exception
    {
        // Not enough data
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(new byte[0]));
            fail("Expected IllegalArgumentException for emptr");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_other() throws Exception
    {
        // No application_information_section
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(new byte[] { 0x73, (byte) 0x80, 0 }));
            fail("Expected IllegalArgumentException for non-application_information_section");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    private byte[] createSectionHeader()
    {
        byte[] section_header = { 0x74, // 0:table_id
                (byte) 0x80, 0, // 1-2: section_length
                0, 1, // 3-4: application_type
                1, // 5: version
                0, // 6: section_number
                0, // 7: last_section_number
                0, 3, // 8-9: common_descriptors_length
                0, 0, 0, // 10-12 dummy descriptor
                0, 12, // 13-14: application_loop_length
                6, 7, 8, 9, // 15-18: appID (OID)
                1, 2, // 19-20: appID (AID)
                0, // 21: application_control_code
                0, 3, // 22-23: application_descriptors_loop_length
                0, 0, 0, // 24-26: dummy descriptor
                0, 0, 0, 0 // 27-30: CRC-32
        };
        section_header[2] = (byte) (section_header.length - 3);

        CRC32 csum = new CRC32();
        csum.update(section_header, 0, section_header.length - 4);

        long value = csum.getValue();
        section_header[section_header.length - 4] = (byte) (value >> 24);
        section_header[section_header.length - 3] = (byte) (value >> 16);
        section_header[section_header.length - 2] = (byte) (value >> 8);
        section_header[section_header.length - 1] = (byte) (value);

        return section_header;
    }

    /**
     * Ensures that no exceptions are thrown given an InputStream created from
     * {@link #createSectionHeader}.
     * 
     * @throws Exception
     */
    public void testRegisterUnboundApp_IllegalArgumentException_self_test() throws Exception
    {
        // verify that createSectionHeader is otherwise valid
        appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(createSectionHeader()));
        // If a failure occurred, then createSectionHeader may not be quite
        // right...
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_section_syntax() throws Exception
    {
        byte[] section_header = createSectionHeader();
        section_header[1] &= ~0x80;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for section_syntax == 0");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_current_next() throws Exception
    {
        byte[] section_header = createSectionHeader();
        section_header[5] &= ~1;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for current_next == 0");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_section_length() throws Exception
    {
        // test >10-bit section length
        byte[] section_header = createSectionHeader();
        byte[] shorter = new byte[section_header.length - 1];
        System.arraycopy(section_header, 0, shorter, 0, shorter.length);

        section_header[1] = (byte) 0x8F;
        section_header[2] = (byte) 0xFF;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for section_length > 10 bits");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // test length < minimum
        section_header = shorter;
        section_header[2] = (byte) (section_header.length - 3);
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for section_length < minimum");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_application_type() throws Exception
    {
        // test bad app type
        byte[] section_header = createSectionHeader();
        section_header[4] = 10;

        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for invalid application_type");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_section_number() throws Exception
    {
        // test bad section_number (e.g., > last_section_number)
        byte[] section_header = createSectionHeader();
        section_header[6] = 1;
        section_header[7] = 0;

        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for section_number > last_section_number");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // TODO(AaronK): test bad section_number (not incremented in xait
        // fragment)
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_common_descriptors_length() throws Exception
    {
        // test bad common-desc loop length
        byte[] section_header = createSectionHeader();
        section_header[8] = 0x0F;
        section_header[9] = (byte) 0xFF;

        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for too large common desc length");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    public void testRegisterUnboundApp_IllegalArgumentException_application_loop_length() throws Exception
    {
        // test too large app loop length
        byte[] section_header = createSectionHeader();
        section_header[13] = 0x0F;
        section_header[14] = (byte) 0xFF;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for too large app loop length");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }

        // test too small app loop length
        section_header = createSectionHeader();
        section_header[13] = 0;
        section_header[14] = 1;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for 0 == app loop length");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Tests <i>no</i> IllegalArgumentException for invalid AppID.
     */
    public void testRegisterUnboundApp_NO_IllegalArgumentException_application_identifier() throws Exception
    {
        // test bad appId
        byte[] section_header = createSectionHeader();
        section_header[15] = 0;
        section_header[16] = 0;
        section_header[17] = 0;
        section_header[18] = 0;
        section_header[19] = (byte) 0xFF;
        section_header[20] = (byte) 0xFF;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
        }
        catch (IllegalArgumentException e)
        {
            fail("Unexpected IllegalArgumentException for bad AppID");
        }
    }

    /**
     * Tests <i>no</i> IllegalArgumentException for invalid app control code.
     */
    public void testRegisterUnboundApp_NO_IllegalArgumentException_application_control_code() throws Exception
    {
        // test bad app control code
        byte[] section_header = createSectionHeader();
        section_header[21] = 33;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
        }
        catch (IllegalArgumentException e)
        {
            fail("Unexpected IllegalArgumentException for bad app control code");
        }
    }

    /**
     * Tests <i>no</i> IllegalArgumentException for invalid app desc loop
     * length.
     */
    public void testRegisterUnboundApp_NO_IllegalArgumentException_app_desc_loop_length() throws Exception
    {
        // test too large app desc loop length
        byte[] section_header = createSectionHeader();
        section_header[22] = 0x0F;
        section_header[23] = (byte) 0xFF;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
        }
        catch (IllegalArgumentException e)
        {
            fail("Unexpected IllegalArgumentException for too large app desc loop length");
        }

        // test too small app desc loop length
        section_header = createSectionHeader();
        section_header[22] = 0;
        section_header[23] = 0;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
        }
        catch (IllegalArgumentException e)
        {
            fail("Unexpected IllegalArgumentException for 0 == app desc loop length");
        }
    }

    /**
     * Tests IllegalArgumentException for invalid section_header.
     */
    // TODO(AaronK): enable CRC test if CRC is validated in AIT parser
    public void XtestRegisterUnboundApp_IllegalArgumentException_CRC32() throws Exception
    {
        // test bad CRC-32
        byte[] section_header = createSectionHeader();
        section_header[section_header.length - 4] ^= (byte) 0xFF;
        section_header[section_header.length - 3] ^= (byte) 0xFF;
        section_header[section_header.length - 2] ^= (byte) 0xFF;
        section_header[section_header.length - 1] ^= (byte) 0xFF;
        try
        {
            appmanagerproxy.registerUnboundApp(new ByteArrayInputStream(section_header));
            fail("Expected IllegalArgumentException for invalid CRC-32");
        }
        catch (IllegalArgumentException e)
        { /* empty */
        }
    }

    /**
     * Test that bad descriptors are ignored.
     * <ul>
     * <li>Non-mandatory too short descriptors (length is too short for minimum
     * length)
     * <li>Non-mandatory too short descriptors (length is too short for variable
     * data contained within)
     * <li>Unknown tags.
     * </ul>
     */
    public void XtestRegisterUnboundApp_ignoredDescriptors() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        final int FIRST = 1;
        final int LAST = 10;

        int oid = 18;
        int svc = 0x700000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
        TestApp info[] = new TestApp[FIRST + LAST - FIRST + 1];
        for (int i = FIRST; i < LAST + 1; ++i)
            info[i] = new TestApp(new AppID(oid, i), svc);

        XAITGenerator badGen[] = {
        // Too short descriptors (non-mandatory)
                new XAITGenerator()
                {
                    public void application_icons_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        out.writeByte(0x0B); // tag
                        out.writeByte(2); // length
                        out.writeByte(0); // no locator
                        out.writeByte(1); // only partial flags
                    }

                    public String toString()
                    {
                        return "too_short_icon_descriptor";
                    }
                },
                // Too long descriptors (non-mandatory)
                new XAITGenerator()
                {
                    public void application_icons_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        out.writeByte(0x0B); // tag
                        out.writeByte(3); // length
                        out.writeByte(100); // invalid length for descriptor
                                            // length
                        out.writeShort(0xf1c1); // flags
                    }

                    public String toString()
                    {
                        return "too_long_icon_descriptor";
                    }
                }, new XAITGenerator()
                {
                    public void application_storage_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        out.writeByte(0xB0);
                        out.writeByte(0); // length
                    }

                    public String toString()
                    {
                        return "application_storage_descriptor too short";
                    }
                },
                // Unknown tags
                new XAITGenerator()
                {
                    byte tag = 0x0E;

                    byte length;

                    void junk(DataOutputStreamEx out, Vector fix) throws Exception
                    {
                        if (tag == 0)
                            tag = 0x0E;
                        else if (tag == 0x5F) ++tag;
                        out.writeByte(tag++);

                        if (length < 0) length = 0;
                        out.writeByte(length);
                        if (length > 0)
                        {
                            for (int i = 0; i < length; ++i)
                                out.writeByte(i);
                        }
                        length = (byte) (length + 1 * 11 / 2); // not-so-random
                                                               // number
                    }

                    protected void more_common_descriptors0(DataOutputStreamEx out, Vector fix) throws Exception
                    {
                        junk(out, fix);
                    }

                    protected void more_common_descriptors1(DataOutputStreamEx out, Vector fix) throws Exception
                    {
                        junk(out, fix);
                    }

                    protected void more_common_descriptors2(DataOutputStreamEx out, Vector fix) throws Exception
                    {
                        junk(out, fix);
                    }

                    protected void more_app_descriptors0(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        junk(out, fix);
                    }

                    protected void more_app_descriptors1(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        junk(out, fix);
                    }

                    protected void more_app_descriptors2(DataOutputStreamEx out, Vector fix, AppInfo app)
                            throws Exception
                    {
                        junk(out, fix);
                    }

                    public String toString()
                    {
                        return "unknown tags";
                    }
                }, };
        for (int badi = 0; badi < badGen.length; ++badi)
        {
            XAITGenerator gen = badGen[badi];
            try
            {
                for (int i = FIRST; i < LAST + 1; ++i)
                    gen.add(info[i]);
                gen.add(svcinfo);
                appmanagerproxy.registerUnboundApp(gen.generate());

                AppsDatabase db = AppsDatabase.getAppsDatabase();
                for (int i = FIRST; i < LAST + 1; ++i)
                {
                    AppID id = new AppID(oid, i);
                    AppProxy app = db.getAppProxy(id);
                    assertNotNull("An appProxy should be returned", app);
                }
            }
            finally
            {
                // cleanup
                for (int i = FIRST; i < LAST + 1; ++i)
                    appmanagerproxy.unregisterUnboundApp(svc, new AppID(oid, i));
                if (KLUDGE) Thread.sleep(200); // kludge because of async
                                               // destruction
            }
        }
    }

    /**
     * Test that bad mandatory descriptors are ignored; and that they cause the
     * app to be ignored. Possibilities...
     * <ul>
     * <li>descriptors too short
     * <li>too short application_profiles_length in app_descriptor
     * <li>no name, or only partial ISO in app_name_descriptor
     * <li>name too long for descriptor in app_name_descriptor
     * <li>paramter_length too long for dvbj_application_descriptor
     * <li>lack of base_directory in dvbj_appilcation_location_descriptor
     * <li>too much base_directory in dvbj_appilcation_location_descriptor
     * <li>too much classpath_extension_length in
     * dvbj_appilcation_location_descriptor
     * <li>lack of initial_class_name in dvbj_appilcation_location_descriptor
     * <li>not enough info for integral number routing descriptor loops
     * </ul>
     */
    public void XtestRegisterUnboundApp_badMandatoryApp() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        final int FIRST = 1;
        final int LAST = 10;

        int oid = 18;
        int svc = 0x800000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
        TestApp info[] = new TestApp[FIRST + LAST - FIRST + 1];
        for (int i = FIRST; i < LAST + 1; ++i)
            info[i] = new TestApp(new AppID(oid, i), svc);

        XAITGenerator badGen[] = { new XAITGenerator()
        {
            public void application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x00);
                    out.writeByte(0x00);
                }
            }

            public String toString()
            {
                return "application_descriptor too short 1";
            }
        }, new XAITGenerator()
        {
            public void application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x00);
                    out.writeByte(0x02);
                    out.writeByte(0x00);
                    out.writeByte(0xc0);
                }
            }

            public String toString()
            {
                return "application_descriptor too short 2";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    out.writeByte(0x00);
                }
            }

            public String toString()
            {
                return "application_name_descriptor too short 1";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    string(out, "en"); // too short, no name following
                }
            }

            public String toString()
            {
                return "application_name_descriptor too short 2";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    string(out, "eng"); // no name following
                }
            }

            public String toString()
            {
                return "application_name_descriptor too short 3";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    out.writeByte(12);
                    string(out, "eng", false); // 3
                    string(out, "dummy"); // 1+5
                    string(out, "spa"); // 3
                    // no name following
                }
            }

            public String toString()
            {
                return "application_name_descriptor too short 4";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    out.writeByte(11);
                    string(out, "eng", false); // 3
                    string(out, "dummy"); // 1+5
                    string(out, "sp"); // 2, too short, no name following
                }
            }

            public String toString()
            {
                return "application_name_descriptor too short 5";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.application_name_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x01);
                    out.writeByte(5);
                    string(out, "eng", false); // 3
                    out.writeByte(100); // 1 - but specifies too much!
                    out.writeByte('a'); // 1
                }
            }

            public String toString()
            {
                return "application_name_descriptor too long 1";
            }
        }, new XAITGenerator()
        {
            public void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.dvb_j_application_descriptor(out, fix, app);
                else
                {
                    out.writeByte(0x04);
                    out.writeByte(0);
                }
            }

            public String toString()
            {
                return "dvb_j_application_descriptor too short";
            }
        }, new XAITGenerator()
        {
            public void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.dvb_j_application_descriptor(out, fix, app);
                else
                {
                    int length_pos;

                    out.writeByte(0x04);
                    length_pos = out.getWritten();
                    out.writeByte(0); // length

                    out.writeByte(0); // no basedir
                    out.writeByte(0); // no classpath
                    string(out, app.getClassName(), false);
                    fixLength(fix, length_pos, 1, out.getWritten());
                }
            }

            public String toString()
            {
                return "dvb_j_application_descriptor no basedir";
            }
        }, new XAITGenerator()
        {
            public void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) != 0)
                    super.dvb_j_application_descriptor(out, fix, app);
                else
                {
                    int length_pos;

                    out.writeByte(0x04);
                    length_pos = out.getWritten();
                    out.writeByte(0); // length

                    string(out, app.getBaseDir());
                    out.writeByte(0); // no classpath
                    fixLength(fix, length_pos, 1, out.getWritten());
                }
            }

            public String toString()
            {
                return "dvb_j_application_descriptor no class name";
            }
        }, };
        for (int badi = 0; badi < badGen.length; ++badi)
        {
            XAITGenerator gen = badGen[badi];
            try
            {
                for (int i = FIRST; i < LAST + 1; ++i)
                    gen.add(info[i]);
                gen.add(svcinfo);
                appmanagerproxy.registerUnboundApp(gen.generate());

                AppsDatabase db = AppsDatabase.getAppsDatabase();
                for (int i = FIRST; i < LAST + 1; ++i)
                {
                    AppID id = new AppID(oid, i);
                    AppProxy app = db.getAppProxy(id);
                    if ((i % 2) == 0)
                        assertNull("Expected app with bad descriptor(s) '" + gen + "' to be skipped", app);
                    else
                        assertNotNull("An appProxy should be returned", app);
                }
            }
            finally
            {
                // cleanup
                for (int i = FIRST; i < LAST + 1; ++i)
                    appmanagerproxy.unregisterUnboundApp(svc, new AppID(oid, i));
                if (KLUDGE) Thread.sleep(200); // kludge because of async
                                               // destruction
            }
        }
    }

    /**
     * Test that bad app entries are ignored.
     */
    public void XtestRegisterUnboundApp_missingMandatoryApp() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        final int FIRST = 1;
        final int LAST = 10;

        int oid = 17;
        int svc = 0x500000;
        TestApp info[] = new TestApp[FIRST + LAST - FIRST + 1];
        for (int i = FIRST; i < LAST + 1; ++i)
            info[i] = new TestApp(new AppID(oid, i), svc + i);

        // Missing mandatory descriptors
        XAITGenerator[] badGen = { new XAITGenerator()
        {
            public void application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) == 0) return; // skip
                                                                // mandatory
                                                                // descriptor
                super.application_descriptor(out, fix, app);
            }

            public String toString()
            {
                return "application_descriptor";
            }
        }, new XAITGenerator()
        {
            public void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) == 0) return; // skip
                                                                // mandatory
                                                                // descriptor
                super.application_name_descriptor(out, fix, app);
            }

            public String toString()
            {
                return "application_name_descriptor";
            }
        }, new XAITGenerator()
        {
            public void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                if ((app.getAppID().getAID() % 2) == 0) return; // skip
                                                                // mandatory
                                                                // descriptor
                super.dvb_j_application_descriptor(out, fix, app);
            }

            public String toString()
            {
                return "dvb_j_application_descriptor";
            }
        }, new XAITGenerator()
        {
            public void dvb_j_application_location_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
                    throws Exception
            {
                if ((app.getAppID().getAID() % 2) == 0) return; // skip
                                                                // mandatory
                                                                // descriptor
                super.dvb_j_application_location_descriptor(out, fix, app);
            }

            public String toString()
            {
                return "dvb_j_application_location_descriptor";
            }
        },
        /*
         * new XAITGenerator() { public void
         * abstract_service_descriptor(DataOutputStreamEx out, Vector fix,
         * SvcInfo svc) throws Exception { if ((svc.getId() % 2) == 0) return;
         * // skip mandatory descriptor super.abstract_service_descriptor(out,
         * fix, svc); } public String toString() { return
         * "abstract_service_descriptor"; } },
         */
        };
        for (int badi = 0; badi < badGen.length; ++badi)
        {
            XAITGenerator gen = badGen[badi];
            try
            {
                for (int i = FIRST; i < LAST + 1; ++i)
                {
                    gen.add(info[i]);
                    gen.add(new TestSvc(svc + i, "FLCL", false));
                }
                appmanagerproxy.registerUnboundApp(gen.generate());

                AppsDatabase db = AppsDatabase.getAppsDatabase();
                for (int i = FIRST; i < LAST + 1; ++i)
                {
                    AppID id = new AppID(oid, i);
                    AppProxy app = db.getAppProxy(id);
                    if ((i % 2) == 0)
                        assertNull("Expected app with missing '" + gen + "' to be skipped", app);
                    else
                        assertNotNull("An appProxy should be returned", app);
                }
            }
            finally
            {
                // cleanup
                for (int i = FIRST; i < LAST + 1; ++i)
                    appmanagerproxy.unregisterUnboundApp(svc, new AppID(oid, i));
                if (KLUDGE) Thread.sleep(200); // kludge because of async
                                               // destruction
            }
        }
    }

    /**
     * Test that bad app entries are ignored.
     */
    public void XtestRegisterUnboundApp_missingBadAppLoop() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        // Bad app loop section (basically empty)
        XAITGenerator gen = new XAITGenerator()
        {
            public void application_loop(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
            {
                // AppID only
                app_id(out, app.getAppID());
            }
        };
        int oid = 18;
        int svc = 0x900000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
        AppID id = new AppID(oid, 95);
        try
        {
            gen.add(new TestApp(id, svc));
            gen.add(svcinfo);
            appmanagerproxy.registerUnboundApp(gen.generate());
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            AppProxy app = db.getAppProxy(id);
            assertNull("Expected app with bad app loop to be skipped", app);
        }
        finally
        {
            appmanagerproxy.unregisterUnboundApp(svc, id);
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
        }
    }

    /**
     * Test that unknown section's are silently ignored.
     */
    public void XtestRegisterUnboundApp_unknownSection() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        // Generate a good section
        XAITGenerator gen = new XAITGenerator();
        int svc = 0xf00000;
        AppID id = new AppID(101, 1010);
        gen.add(new TestApp(id, svc));
        gen.add(new TestSvc(svc, "FLCL", false));

        // Section to be skipped
        byte[] badSection = { 0x73, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00 };
        InputStream is = new SequenceInputStream(new ByteArrayInputStream(badSection), gen.generate());

        try
        {
            appmanagerproxy.registerUnboundApp(gen.generate());
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            AppProxy app = db.getAppProxy(id);
            assertNotNull("An appProxy should be returned", app);
        }
        finally
        {
            appmanagerproxy.unregisterUnboundApp(svc, id);
        }
    }

    /**
     * Tests that apps in bad application_information_section's are ignored.
     */
    public void XtestRegisterUnboundApp_ignoredSection() throws Exception
    {
        // TODO(AaronK): test section ignored because of bad mandatory
        // descriptors in common area
        fail("Unimplemented test");
    }

    /**
     * Tests generation/propagation of IOException as appropriate.
     */
    public void testRegisterUnboundApp_IOException() throws Exception
    {
        AppID id = new AppID(11, 99);
        int svc = 0x300000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
        try
        {
            TestApp info = new TestApp(id, svc);

            XAITGenerator gen = new XAITGenerator();
            gen.add(info);
            gen.add(svcinfo);

            // Throw IOException after arbitrary number (4) bytes
            InputStream shorted = new FilterInputStream(gen.generate())
            {
                private int avail = 4;

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
            };
            appmanagerproxy.registerUnboundApp(shorted);
            fail("Expected IOException to be thrown");
        }
        catch (IOException e)
        {
            // expected
        }
        finally
        {
            appmanagerproxy.unregisterUnboundApp(svc, id);
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
        }
    }

    /**
     * Tests unregisterUnboundApp().
     */
    public void XtestUnregisterUnboundApp() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        final int FIRST = 1;
        final int LAST = 10;

        int svc = 0x100000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            // Register a bunch of apps...
            XAITGenerator gen = new XAITGenerator();
            for (int i = FIRST; i < LAST + 1; ++i)
                gen.add(new TestApp(new AppID(12, i), svc));
            gen.add(svcinfo);
            appmanagerproxy.registerUnboundApp(gen.generate());

            // Verify that they can be looked up in the AppsDatabase
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            for (int i = FIRST; i < LAST + 1; ++i)
            {
                AppProxy app = db.getAppProxy(new AppID(12, i));
                assertNotNull("An appProxy should be returned", app);
            }

            // Unregister the apps
            // Verify that they are no longer in the AppsDatabase
            for (int i = FIRST; i < LAST + 1; ++i)
            {
                sm.p = null;
                appmanagerproxy.unregisterUnboundApp(1, new AppID(12, i));
                verifyPermission(sm.p, "registrar");

                if (KLUDGE) Thread.sleep(200); // kludge because of async
                                               // destruction
                AppProxy app = db.getAppProxy(new AppID(12, i));
                assertNull("An appProxy should NOT be returned (" + i + ")", app);

                for (int j = i + 1; j < LAST + 1; ++j)
                {
                    app = db.getAppProxy(new AppID(12, j));
                    assertNotNull("An appProxy should STILL be returned (" + j + ")", app);
                }
            }
        }
        finally
        {
            // cleanup
            for (int i = FIRST; i < LAST + 1; ++i)
                appmanagerproxy.unregisterUnboundApp(svc, new AppID(12, i));
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
            ProxySecurityManager.pop();
        }

        // Unknown AppID shouldn't be a problem
        appmanagerproxy.unregisterUnboundApp(svc, new AppID(100, 100));
        // Unknown service id shouldn't be a problem
        appmanagerproxy.unregisterUnboundApp(100, new AppID(12, 1));
    }

    /**
     * Tests unregisterUnboundApp() IllegalArgumentException.
     */
    public void XtestUnregisterUnboundApp_IllegalArgumentException()
    {
        // TODO(AaronK): Should verify IllegalArgumentException on
        // unregister!!!!
        fail("Unimplemented test");
    }

    /**
     * Tests setAppFilter().
     */
    public void XtestSetAppFilter() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        DVBJProxy app = null;
        final AppID id = new AppID(10, 98);
        final int svc = 0x400000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            class MyDBFilter extends AppsDatabaseFilter
            {
                public boolean called;

                public boolean accept(AppID appid)
                {
                    called = true;
                    return false;
                }
            }
            class MyAppFilter extends AppFilter
            {
                public boolean called;

                public boolean accept(AppID appid)
                {
                    called = true;
                    return false;
                }
            }

            // Add an app
            XAITGenerator gen = new XAITGenerator();
            gen.add(new TestApp(id, svc));
            gen.add(svcinfo);
            InputStream is = gen.generate();
            appmanagerproxy.registerUnboundApp(is);

            // By default, any apps should be launchable
            app = (DVBJProxy) AppsDatabase.getAppsDatabase().getAppProxy(id);
            Listener listener = new Listener();
            app.addAppStateChangeEventListener(listener);
            AppStateChangeEvent e;
            synchronized (listener)
            {
                app.load();
                e = listener.waitNextEvent(2000);
            }
            app.stop(true); // kill it off...
            Thread.sleep(200); // kludge because of async destruction
            assertNotNull("Expected state change event", e);
            assertFalse("Expected state change to have succeeded", e.hasFailed());

            // Set a filter
            MyDBFilter filter = new MyDBFilter();
            sm.p = null;
            appmanagerproxy.setAppFilter(filter);
            verifyPermission(sm.p, "handler.appFilter");

            /*
             * !!!!FAILURE HERE: new events were expected No
             * NOT_LOADED->LOADED/failed event is generated. Instead we have a
             * NOT_LOADED->NOT_LOADED event. Problem is that
             * PARTIALLY_LOADED=NOT_LOADED|EXT_STATE. So
             * NOT_LOADED->PARTIALLY_LOADED => NOT_LOADED->NOT_LOADED.
             * 
             * Possible fixes?
             */

            // Try to load app
            app = (DVBJProxy) AppsDatabase.getAppsDatabase().getAppProxy(id);
            listener = new Listener();
            app.addAppStateChangeEventListener(listener);
            synchronized (listener)
            {
                app.load();
                e = listener.waitNextEvent(2000);
            }
            app.removeAppStateChangeEventListener(listener);
            app.stop(true); // kill it off...
            Thread.sleep(200); // kludge because of async destruction
            assertNotNull("Expected state change event", e);
            assertTrue("Expected state change to have failed", e.hasFailed());

            // Ensure that filter was consulted
            assertTrue("Filter should be consulted", filter.called);
            filter.called = false;

            // Replace filter
            MyAppFilter filter2 = new MyAppFilter();
            sm.p = null;
            appmanagerproxy.setAppFilter(filter2);
            verifyPermission(sm.p, "handler.appFilter");

            // Try to load app
            app = (DVBJProxy) AppsDatabase.getAppsDatabase().getAppProxy(id);
            listener = new Listener();
            app.addAppStateChangeEventListener(listener);
            synchronized (listener)
            {
                app.load();
                e = listener.waitNextEvent(2000);
            }
            app.removeAppStateChangeEventListener(listener);
            app.stop(true); // kill it off...
            Thread.sleep(200); // kludge because of async destruction
            assertNotNull("Expected state change event", e);
            assertTrue("Expected state change to have failed", e.hasFailed());

            // Ensure new filter is consulted (and not other one)
            assertTrue("Filter should be consulted", filter2.called);
            assertFalse("Old filter should not be consulted", filter.called);

            // Make sure we can remove filter with null
            appmanagerproxy.setAppFilter(null);

            // By default, any apps should be launchable
        }
        finally
        {
            appmanagerproxy.setAppFilter(null);
            appmanagerproxy.unregisterUnboundApp(svc, id);
            if (app != null) app.stop(true);
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
            ProxySecurityManager.pop();
        }
    }

    /**
     * Verifies that the <code>p1</code> PermissionCollection is implied by the
     * <code>p2</code> PermissionCollection.
     * 
     * @param p1
     * @param p2
     */
    private void verifyPermissionCollection(PermissionCollection p1, PermissionCollection p2)
    {
        for (Enumeration e = p1.elements(); e.hasMoreElements();)
        {
            assertTrue("PermissionCollection's should be equivalent", p2.implies((Permission) e.nextElement()));
        }
    }

    /**
     * Kludge to gain access to the last permissionDomain object. Will be
     * written to by the Kludge test xlet (must be built separately).
     */
    private static Object lastPD;

    public static void lastProtectionDomain(Object obj)
    {
        lastPD = obj;
    }

    /**
     * Tests setSecurityPolicyHandler().
     */
    public void XtestSetSecurityPolicyHandler() throws Exception
    {
        // TODO(AaronK): rewrite to not use AppsDatabase (use SIManager instead)
        // ...at least until mon app can see all apps on system, not just in
        // same ServiceContext

        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        class Handler1 implements SecurityPolicyHandler
        {
            public boolean called;

            public PermissionCollection returned;

            public PermissionCollection getAppPermissions(PermissionInformation pi)
            {
                return returned = pi.getRequestedPermissions();
            }
        }
        class Handler2 implements SecurityPolicyHandler
        {
            public boolean called;

            public PermissionCollection returned;

            public PermissionCollection getAppPermissions(PermissionInformation pi)
            {
                return returned = new Permissions();
            }
        }

        AppID id = new AppID(15, 97);
        int svc = 0x600000;
        TestSvc svcinfo = new TestSvc(svc, "FLCL", false);
        DVBJProxy app = null;

        class MyApp extends TestApp
        {
            public MyApp(AppID id, int service)
            {
                super(id, service);
            }

            public String getClassName()
            {
                return "org.cablelabs.xlet.Kludge";
            }

            public String[] getParameters()
            {
                return new String[] { "pd", "org.dvb.application.AppManagerProxyTest", "lastProtectionDomain" };
            }
        }

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            // Add an app
            XAITGenerator gen = new XAITGenerator();
            gen.add(new TestApp(id, svc));
            gen.add(svcinfo);
            InputStream is = gen.generate();
            appmanagerproxy.registerUnboundApp(is);

            // By default, app class should be loaded with
            // "standard" permissions... how can we check this?

            Handler1 h1 = new Handler1();
            Handler2 h2 = new Handler2();

            // Set the handler
            sm.p = null;
            appmanagerproxy.setSecurityPolicyHandler(h1);
            verifyPermission(sm.p, "security");

            // Try to load app
            app = (DVBJProxy) AppsDatabase.getAppsDatabase().getAppProxy(id);
            Listener listener = new Listener();
            app.addAppStateChangeEventListener(listener);
            AppStateChangeEvent e;
            synchronized (listener)
            {
                app.init();
                e = listener.waitNextEvent(2000);
            }
            app.removeAppStateChangeEventListener(listener);
            app.stop(true); // kill it off...
            Thread.sleep(200); // kludge because of async destruction
            assertNotNull("Expected state change event", e);
            // assertTrue("Expected state change to have failed",
            // e.hasFailed());

            // Ensure handler is consulted for permissions
            assertTrue("The SecurityPolicyHandler should've been consulted", h1.called);
            // check the permissions that are returned
            assertNotNull("The last ProtectionDomain should've been set", lastPD);
            assertTrue("The last ProtectionDomain should've been set", lastPD instanceof ProtectionDomain);
            verifyPermissionCollection(h1.returned, ((ProtectionDomain) lastPD).getPermissions());
            lastPD = null;

            // Replace handler
            sm.p = null;
            appmanagerproxy.setSecurityPolicyHandler(h2);
            verifyPermission(sm.p, "security");

            // Try to load app
            h1.called = false;
            app = (DVBJProxy) AppsDatabase.getAppsDatabase().getAppProxy(id);
            listener = new Listener();
            app.addAppStateChangeEventListener(listener);
            synchronized (listener)
            {
                app.init();
                e = listener.waitNextEvent(2000);
            }
            app.removeAppStateChangeEventListener(listener);
            app.stop(true); // kill it off...
            Thread.sleep(200); // kludge because of async destruction
            assertNotNull("Expected state change event", e);
            // assertTrue("Expected state change to have failed",
            // e.hasFailed());

            // Ensure handler is consulted for permissions
            assertTrue("The SecurityPolicyHandler should've been consulted", h2.called);
            // Ensure *other* handler is not consulted
            assertFalse("The old SecurityPolicyHandler should NOT have been consulted", h1.called);
            // check the permissions that are returned
            assertNotNull("The last ProtectionDomain should've been set", lastPD);
            assertTrue("The last ProtectionDomain should've been set", lastPD instanceof ProtectionDomain);
            verifyPermissionCollection(h2.returned, ((ProtectionDomain) lastPD).getPermissions());
            lastPD = null;
        }
        finally
        {
            appmanagerproxy.setSecurityPolicyHandler(null);
            appmanagerproxy.unregisterUnboundApp(svc, id);
            if (app != null) app.stop(true);
            if (KLUDGE) Thread.sleep(200); // kludge because of async
                                           // destruction
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests setAppSignalHandler().
     */
    public void testSetAppSignalHandler()
    {
        assertNotNull("Should have a valid AppManagerProxy instance", appmanagerproxy);

        // For now, just check security...
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            sm.p = null;
            appmanagerproxy.setAppSignalHandler(new AppSignalHandler()
            {
                public boolean notifyXAITUpdate(OcapAppAttributes[] newApps)
                {
                    return true;
                }
            });
            verifyPermission(sm.p, "registrar");
        }
        finally
        {
            appmanagerproxy.setAppSignalHandler(null);
            ProxySecurityManager.pop();
        }

        // TODO(AaronK): finish testing setAppSignalHandler()
        // How??? Send signalling through ServicesDatabase...?
        // Implement dummy ServiceManager?
        // fail("Unfinished test");
    }

    /**
     * Verify that the AppManagerProxy doesn't leak old filters.
     * 
     * @see "bug 4334"
     */
    public void testSetAppFilter_ClearLeak()
    {
        // First ensure that WeakReference works as expected here
        Object o = new Object();
        Reference r0 = new WeakReference(o);
        assertNotNull("Reference should not be null", r0.get());
        o = null;
        // After GC, should be deleted
        System.gc();
        assertNull("Expected weak reference to return null", r0.get());

        AppFilter f1 = new AppFilter();
        Reference r1 = new WeakReference(f1);

        // Set security policy handler
        appmanagerproxy.setAppFilter(f1);
        f1 = null;

        // Clear security policy handler
        appmanagerproxy.setAppFilter(null);

        // After GC, should be deleted
        System.gc();
        assertNull("The cleared filter has apparently been leaked", r1.get());
    }

    /**
     * Verify that the AppManagerProxy doesn't leak old filters.
     * 
     * @see "bug 4334"
     */
    public void testSetAppFilter_ReplaceLeak()
    {
        AppFilter f1 = new AppFilter();
        AppFilter f2 = new AppFilter();
        Reference r1 = new WeakReference(f1);
        Reference r2 = new WeakReference(f2);

        // Set first filter
        appmanagerproxy.setAppFilter(f1);
        f1 = null;

        // Replace filter
        appmanagerproxy.setAppFilter(f2);
        f2 = null;

        // After GC, original should be collected
        System.gc();

        try
        {
            assertNotNull("The new filter should not have been collected", r2.get());
            assertNull("A repalced filter appears to have been leaked", r1.get());
        }
        finally
        {
            appmanagerproxy.setAppFilter(null);
        }
    }

    /**
     * Verify that the AppManagerProxy doesn't leak old handlers.
     * 
     * @see "bug 4334"
     */
    public void testAppSignalHandler_ClearLeak()
    {
        // First ensure that WeakReference works as expected here
        Object o = new Object();
        Reference r0 = new WeakReference(o);
        assertNotNull("Reference should not be null", r0.get());
        o = null;
        // After GC, should be deleted
        System.gc();
        assertNull("Expected weak reference to return null", r0.get());

        AppSignalHandler f1 = new Handler();
        Reference r1 = new WeakReference(f1);

        // Set security policy handler
        appmanagerproxy.setAppSignalHandler(f1);
        f1 = null;

        // Clear security policy handler
        appmanagerproxy.setAppSignalHandler(null);

        // After GC, should be deleted
        System.gc();
        assertNull("The cleared SignalHandler has apparently been leaked", r1.get());

    }

    /**
     * Verify that the AppManagerProxy doesn't leak old handlers.
     * 
     * @see "bug 4334"
     */
    public void testAppSignalHandler_ReplaceLeak()
    {
        AppSignalHandler f1 = new Handler();
        AppSignalHandler f2 = new Handler();
        Reference r1 = new WeakReference(f1);
        Reference r2 = new WeakReference(f2);

        // Set first SignalHandler
        appmanagerproxy.setAppSignalHandler(f1);
        f1 = null;

        // Replace SignalHandler
        appmanagerproxy.setAppSignalHandler(f2);
        f2 = null;

        // After GC, original should be collected
        System.gc();

        try
        {
            assertNotNull("The new SignalHandler should not have been collected", r2.get());
            assertNull("A repalced SignalHandler appears to have been leaked", r1.get());
        }
        finally
        {
            appmanagerproxy.setAppSignalHandler(null);
        }

    }

    /**
     * Verify that handler is cleaned up after app destruction.
     * 
     * @see "bug 5100"
     */
    public void testAppSignalHandler_CallerContext() throws Exception
    {
        replaceCCMgr();

        try
        {
            AppSignalHandler f1 = new Handler();
            Context cc = new Context(new AppID(1, 2));
            try
            {
                // Set handler
                cc.setAppSignalHandler(appmanagerproxy, f1);

                // TODO: ensure that handler is called in context
            }
            finally
            {
                // Destroy caller context
                cc.cleanup();
            }

            // Ensure no leaks
            Reference r1 = new WeakReference(f1);
            Reference r2 = new WeakReference(cc);
            f1 = null;
            cc = null;

            System.gc();
            System.gc();
            System.gc();

            assertNull("Handler still referenced after app destroyed", r1.get());
            assertNull("CallerConetxt still referenced after app destroyed", r2.get());
        }
        finally
        {
            restoreCCMgr();
        }
    }

    public static class Context extends org.dvb.event.EventManagerTest.Context
    {
        public Context(AppID id)
        {
            super(id);
        }

        public void setAppSignalHandler(final AppManagerProxy apm, final AppSignalHandler ash)
        {
            doRun(new Runnable()
            {
                public void run()
                {
                    apm.setAppSignalHandler(ash);
                }
            });
        }
    }

    /**
     * Class used to generate an XAIT <code>InputStream</code> from a
     * description of applications.
     */
    public static class XAITGenerator
    {
        private Vector applist = new Vector();

        private Vector svclist = new Vector();

        int table_id = 0x74; // application_information_section

        int application_type = 0x0001; // application_type

        public XAITGenerator()
        { /* empty */
        }

        public void add(AppInfo info)
        {
            applist.addElement(info);
        }

        public void add(SvcInfo svc)
        {
            svclist.addElement(svc);
        }

        protected static class DataOutputStreamEx extends DataOutputStream
        {
            public DataOutputStreamEx(OutputStream os)
            {
                super(os);
            }

            public int getWritten()
            {
                return written;
            }
        }

        /**
         * Generates an InputStream for the application information that's been
         * added to this XAITGenerator.
         * 
         * @returns an <code>InputStream</code> that can be passed to
         *          <code>AppManagerProxy.registerUnboundApp()</code>.
         */
        public InputStream generate() throws Exception
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStreamEx out = new DataOutputStreamEx(os);
            Vector cleanup = new Vector();

            // Generate table
            application_information_section(out, cleanup);

            // Fix up 16-bit length locations
            byte[] bytes = os.toByteArray();
            for (Enumeration e = cleanup.elements(); e.hasMoreElements();)
            {
                int[] fix = (int[]) e.nextElement();

                // fix[0] = offset
                // fix[1] = size
                // fix[2] = ubyte/ushort/uint to copy
                switch (fix[1])
                {
                    case 1:
                        bytes[fix[0]] |= (fix[2]) & 0xFF;
                        break;
                    case 2:
                        bytes[fix[0]] |= (fix[2] >> 8) & 0xFF;
                        bytes[fix[0] + 1] |= (fix[2]) & 0xFF;
                        break;
                    case 4:
                        bytes[fix[0]] |= (fix[2] >> 24) & 0xFF;
                        bytes[fix[0] + 1] |= (fix[2] >> 16) & 0xFF;
                        bytes[fix[0] + 2] |= (fix[2] >> 8) & 0xFF;
                        bytes[fix[0] + 3] |= (fix[2]) & 0xFF;
                        break;
                }
            }

            return new ByteArrayInputStream(bytes);
        }

        /*
         * <pre> application_information_section() { table_id 8 uimsbf
         * section_syntax_indicator 1 bslbf reserved_future_use 1 bslbf reserved
         * 2 bslbf section_length 12 uimsbf application_type 16 uimsbf reserved
         * 2 bslbf version_number 5 uimsbf current_next_indicator 1 bslbf
         * section_number 8 uimsbf last_section_number 8 uimsbf
         * reserved_future_use 4 bslbf common_descriptors_length 12 uimsbf
         * for(i=0;i<N;i++){ descriptor() } reserved_future_use 4 bslbf
         * application_loop_length 12 uimsbf for(i=0;i<N;i++){
         * application_identifier() application_control_code 8 uimsbf
         * reserved_future_use 4 bslbf application_descriptors_loop_length 12
         * uimsbf for(j=0;j<N;j++){ descriptor() } } CRC_32 32 rpchof } </pre>
         */
        protected void application_information_section(DataOutputStreamEx out, Vector fix) throws Exception
        {
            int length_pos;
            int common_pos;
            int apps_pos;

            out.writeByte((byte) table_id); // table_id
            length_pos = out.getWritten();
            out.writeShort(0x8000); // section_syntax_indicator + length
            out.writeShort(application_type); // OCAP_J
            out.writeByte(0x03); // version_number + current_next_indicator
            out.writeByte(0x00); // section_number
            out.writeByte(0x00); // last_section_number
            common_pos = out.getWritten();
            out.writeShort(0x0000); // common_descriptors_length

            // Common descriptors
            more_common_descriptors0(out, fix);
            // -- abstract services
            for (Enumeration svcs = svclist.elements(); svcs.hasMoreElements();)
            {
                SvcInfo svc = (SvcInfo) svcs.nextElement();
                abstract_service_descriptor(out, fix, svc);
            }
            more_common_descriptors1(out, fix);
            // -- external authorization
            // -- transport_protocol
            more_common_descriptors2(out, fix);

            fixLength(fix, common_pos, 2, out.getWritten());

            apps_pos = out.getWritten();
            out.writeShort(0x0000); // application_loop_length !!!!
            for (Enumeration apps = applist.elements(); apps.hasMoreElements();)
            {
                AppInfo app = (AppInfo) apps.nextElement();
                application_loop(out, fix, app);
            }
            fixLength(fix, apps_pos, 2, out.getWritten());

            out.writeInt(0x00000000); // CRC32

            fixLength(fix, length_pos, 2, out.getWritten());
        }

        protected void more_common_descriptors0(DataOutputStreamEx out, Vector fix) throws Exception
        {
            // empty
        }

        protected void more_common_descriptors1(DataOutputStreamEx out, Vector fix) throws Exception
        {
            // empty
        }

        protected void more_common_descriptors2(DataOutputStreamEx out, Vector fix) throws Exception
        {
            // empty
        }

        protected void application_loop(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int app_pos;

            app_id(out, app.getAppID());
            out.writeByte(app.getControlCode());

            app_pos = out.getWritten();
            out.writeShort(0x0000); // application_descriptors_loop_length!!!!!

            more_app_descriptors0(out, fix, app);
            application_descriptor(out, fix, app);
            application_name_descriptor(out, fix, app);
            application_icons_descriptor(out, fix, app);
            more_app_descriptors1(out, fix, app);
            dvb_j_application_descriptor(out, fix, app);
            dvb_j_application_location_descriptor(out, fix, app);
            unbound_application_descriptor(out, fix, app);
            application_storage_descriptor(out, fix, app);
            transport_protocol_descriptor(out, fix, app);
            more_app_descriptors2(out, fix, app);

            fixLength(fix, app_pos, 2, out.getWritten());
        }

        protected void more_app_descriptors0(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            // empty
        }

        protected void more_app_descriptors1(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            // empty
        }

        protected void more_app_descriptors2(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            // empty
        }

        protected void fixLength(Vector fix, int pos, int size, int written)
        {
            fix.addElement(new int[] { pos, size, written - pos - size });
        }

        protected void app_id(DataOutputStreamEx out, AppID id) throws Exception
        {
            out.writeInt(id.getOID());
            out.writeShort(id.getAID());
        }

        /**
         * <pre>
         * application_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   application_profiles_length 8 uimsbf
         *   for( i=0; i<N; i++ ) {
         *     application_profile 16 uimsbf
         *     version.major 8 uimsbf
         *     version.minor 8 uimsbf
         *     version.micro 8 uimsbf
         *   }
         *   service_bound_flag 1 bslbf
         *   visibility 2 bslbf
         *   reserved_future_use 5 bslbf
         *   application_priority 8 uimsbf
         *   for( i=0; i<N; i++ ) {
         *     transport_protocol_label 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int length_pos;
            int prof_pos;

            out.writeByte(0x00); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length !!!

            prof_pos = out.getWritten();
            out.writeByte(0x00); // application_profiles length!!!

            // profiles
            // According to OCAP 13.2.1.10.1...
            // There is only one profile with version 1.0
            out.writeShort(1); // assume ocap.profile.basic_profile == 1
            out.writeByte(1); // assume 1.0==1.0.0
            out.writeByte(0);
            out.writeByte(0);

            fixLength(fix, prof_pos, 1, out.getWritten());

            out.writeByte(0x80 | (app.getVisibility() << 5)); // service_bound +
                                                              // visibility
            out.writeByte(app.getPriority());

            AppInfo.TPInfo[] tp = app.getTransportProtocols();
            for (int i = 0; i < tp.length; ++i)
                out.writeByte(tp[i].getLabel()); // transport_protocol_label

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * application_name_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for (i=0; i<N; i++) {
         *     ISO_639_language_code 24 bslbf
         *     application_name_length 8 uimsbf
         *     for (i=0; i<N; i++) {
         *       application_name_char 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         */
        protected void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int length_pos;

            out.writeByte(0x01); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length !!!

            // iso_lang
            string(out, "eng", false);
            string(out, app.getName());

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        protected void string(DataOutputStreamEx out, String str, boolean length) throws Exception
        {
            byte[] bytes = str.getBytes();

            if (length) out.writeByte(bytes.length);
            for (int i = 0; i < bytes.length; ++i)
                out.writeByte(bytes[i]);
        }

        protected void string(DataOutputStreamEx out, String str) throws Exception
        {
            string(out, str, true);
        }

        /**
         * <pre>
         * application_icons_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   icon_locator_length 8 uimsbf
         *   for (i=0; i<N; i++) {
         *     icon_locator_byte 8 uimsbf
         *   }
         *   icon_flags 16 bslbf
         *   for (i=0; i<N; i++) {
         *     reserved_future_use 8 bslbf
         *   }
         * }
         * </pre>
         */
        protected void application_icons_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            AppIcon icon = app.getAppIcon();
            if (icon == null) return;

            int length_pos;

            out.writeByte(0x0B); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            string(out, icon.getLocator().toExternalForm());

            BitSet set = icon.getIconFlags();
            int icon_flags = 0;
            for (int i = 0; i < 16; ++i)
                if (set.get(i)) icon_flags |= 1 << i;
            out.writeShort(icon_flags);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * dvb_j_application_descriptor(){
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i<N; i++) {
         *     parameter_length 8 uimsbf
         *     for(j=0; j<parameter_length; j++) {
         *       parameter_byte 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         */
        protected void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int length_pos;

            out.writeByte(0x03); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            String[] parms = app.getParameters();
            for (int i = 0; i < parms.length; ++i)
                string(out, parms[i]);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * dvb_j_application_location_descriptor {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   base_directory_length 8 uimsbf
         *   for(i=0; i<N; i++) {
         *     base_directory_byte 8 uimsbf
         *   }
         *   classpath_extension_length 8 uimsbf
         *   for(i=0; i<N; i++) {
         *     classpath_extension_byte 8 uimsbf
         *   }
         *   for(i=0; i<N; i++) {
         *     initial_class_byte 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void dvb_j_application_location_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
                throws Exception
        {
            int length_pos;

            out.writeByte(0x04); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            string(out, app.getBaseDir());
            string(out, app.getClasspath());
            string(out, app.getClassName(), false);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * Handles the abstract_service_descriptor.
         * 
         * <pre>
         * abstract_service_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAE
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   reserved_for_future_use 7 uimsbf
         *   auto_select 1 bslbf
         *   for (i=0; i<N; i++) {
         *     service_name_byte 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void abstract_service_descriptor(DataOutputStreamEx out, Vector fix, SvcInfo svc) throws Exception
        {
            int length_pos;

            out.writeByte(0xAE); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            int service_id = svc.getId();
            out.writeByte((service_id >> 16) & 0xFF);
            out.writeByte((service_id >> 8) & 0xFF);
            out.writeByte((service_id) & 0xFF);

            out.writeByte(svc.isAutoSelect() ? 1 : 0);

            string(out, svc.getName(), false);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * unbound_application_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAF
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   version_number 32 uimsbf
         * }
         * </pre>
         */
        protected void unbound_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int length_pos;

            out.writeByte(0xAF); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            int service_id = app.getServiceID();
            out.writeByte((service_id >> 16) & 0xFF);
            out.writeByte((service_id >> 8) & 0xFF);
            out.writeByte((service_id) & 0xFF);

            out.writeInt(app.getVersion());

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * application_storage_descriptor() {
         *   descriptor_tag 8 uimsbf 0xB0
         *   descriptor_length 8 uimsbf
         *   storage_priority 16 uimsbf
         *   launch_order 8 uimsbf
         * }
         * </pre>
         */
        protected void application_storage_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            int length_pos;

            out.writeByte(0xB0); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            out.writeShort(app.getStoragePriority());
            out.writeByte(app.getLaunchOrder());

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * transport_protocol_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   protocol_id 16 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i<N; i++) {
         *     selector_byte 8 uimsbf N1
         *   }
         * }
         * </pre>
         */
        protected void transport_protocol_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
        {
            AppInfo.TPInfo[] tp = app.getTransportProtocols();
            for (int i = 0; i < tp.length; ++i)
            {
                int length_pos;

                out.writeByte(0x02); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                out.writeShort(tp[i].getId());
                out.writeByte(tp[i].getLabel());

                out.writeByte(tp[i].isRemote() ? 0x80 : 0x00);
                if (tp[i].isRemote())
                {
                    out.writeShort(tp[i].getNetId());
                    out.writeShort(tp[i].getTsId());
                    out.writeShort(tp[i].getSId());
                }
                if (tp[i] instanceof AppInfo.OCInfo)
                {
                    out.writeByte(((AppInfo.OCInfo) tp[i]).getComponent());
                }
                else if (tp[i] instanceof AppInfo.IPInfo)
                {
                    String[] urls = ((AppInfo.IPInfo) tp[i]).getUrls();
                    for (int ii = 0; ii < urls.length; ++ii)
                    {
                        string(out, urls[ii]);
                    }
                }

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        public static interface SvcInfo
        {
            public int getId();

            public boolean isAutoSelect();

            public String getName();
        }

        public static interface AppInfo
        {
            public static final int VIS_NONVIS = 0;

            public static final int VIS_LISTING = 1;

            public static final int VIS_FULL = 3;

            public AppID getAppID();

            public String getName();

            public int getControlCode();

            public int getVisibility();

            public int getPriority();

            public int getLaunchOrder();

            public int[] getPlatformVersion();

            public int getVersion();

            public int getServiceID();

            public String getBaseDir();

            public String getClasspath();

            public String getClassName();

            public String[] getParameters();

            public AppIcon getAppIcon();

            public int getStoragePriority();

            public TPInfo[] getTransportProtocols();

            public static interface TPInfo
            {
                public int getId();

                public int getLabel();

                public boolean isRemote();

                public int getNetId();

                public int getTsId();

                public int getSId();
            }

            public static interface OCInfo extends TPInfo
            {
                public int getComponent();
            }

            public static interface IPInfo extends TPInfo
            {
                public boolean isAligned();

                public String[] getUrls();
            }
        }
    }

    /**
     * A basic SvcInfo skeleton.
     */
    public static class TestSvc implements XAITGenerator.SvcInfo
    {
        int id;

        String name;

        boolean auto;

        public TestSvc(int id, String name, boolean auto)
        {
            this.id = id;
            this.name = name;
            this.auto = auto;
        }

        public int getId()
        {
            return id;
        }

        public boolean isAutoSelect()
        {
            return auto;
        }

        public String getName()
        {
            return name;
        }
    }

    /**
     * A basic AppInfo skeleton.
     */
    private static class TestApp implements XAITGenerator.AppInfo
    {
        private AppID id;

        private int svcId;

        public TestApp(AppID id, int svcId)
        {
            this.id = id;
            this.svcId = svcId;
        }

        public AppID getAppID()
        {
            return id;
        }

        public String getName()
        {
            return toString();
        }

        public int getControlCode()
        {
            return OcapAppAttributes.PRESENT;
        }

        public int getVisibility()
        {
            return VIS_FULL;
        }

        public int getPriority()
        {
            return 255;
        }

        public int getLaunchOrder()
        {
            return 1;
        }

        public int[] getPlatformVersion()
        {
            return new int[] { 1, 0, 0 };
        }

        public int getVersion()
        {
            return 1;
        }

        public int getServiceID()
        {
            return svcId;
        }

        public String getBaseDir()
        {
            return "/project/RI_Stack/java/xlet";
        }

        public String getClasspath()
        {
            return "";
        }

        public String getClassName()
        {
            return "org.cablelabs.xlet.Test";
        }

        public String[] getParameters()
        {
            String[] parms = new String[id.getAID() % 4];
            for (int i = 0; i < parms.length; ++i)
                parms[i] = "" + i;
            return parms;
        }

        public AppIcon getAppIcon()
        {
            return new AppIcon()
            {
                public org.davic.net.Locator getLocator()
                {
                    try
                    {
                        return new org.ocap.net.OcapLocator("ocap:/app-icons/" + id);
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                }

                public BitSet getIconFlags()
                {
                    BitSet flags = new BitSet();
                    final int len = id.getAID() % 5;
                    for (int i = 0; i < len; ++i)
                        flags.set(i);
                    return flags;
                }
            };
        }

        public int getStoragePriority()
        {
            return 1;
        }

        public TPInfo[] getTransportProtocols()
        {
            return new TPInfo[] { new OCInfo()
            {
                public int getId()
                {
                    return 0x0001;
                }

                public int getLabel()
                {
                    return 10;
                }

                public boolean isRemote()
                {
                    return false;
                }

                public int getNetId()
                {
                    return 0;
                }

                public int getTsId()
                {
                    return 0;
                }

                public int getSId()
                {
                    return 1;
                }

                public int getComponent()
                {
                    return 0;
                }
            }, new IPInfo()
            {
                public int getId()
                {
                    return 0x0002;
                }

                public int getLabel()
                {
                    return 11;
                }

                public boolean isRemote()
                {
                    return false;
                }

                public int getNetId()
                {
                    return 0;
                }

                public int getTsId()
                {
                    return 0;
                }

                public int getSId()
                {
                    return 2;
                }

                public boolean isAligned()
                {
                    return false;
                }

                public String[] getUrls()
                {
                    return new String[] { "file:/" + getBaseDir() };
                }
            }, };
        }
    }

    private class Handler implements AppSignalHandler
    {
        public boolean notifyXAITUpdate(OcapAppAttributes[] apps)
        {
            return false;
        }
    }

    public AppManagerProxyTest(String name)
    {
        super(name);
    }

    protected AppManagerProxy createAppManagerProxy()
    {
        return AppManagerProxy.getInstance();
    }

    private CallerContextManager save;

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    protected AppManagerProxy appmanagerproxy;

    protected void setUp() throws Exception
    {
        super.setUp();
        appmanagerproxy = createAppManagerProxy();

        // Determine persistent storage file name
        String dir;
        if ((dir = MPEEnv.getEnv("OCAP.persistent.address_props")) == null)
        {
            dir = MPEEnv.getEnv("OCAP.persistent.root");
        }
        persistentPropertiesFile = dir + File.separator + "address_props";
    }

    protected void tearDown() throws Exception
    {
        appmanagerproxy = null;
        super.tearDown();
    }

    private String persistentPropertiesFile;

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AppManagerProxyTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AppManagerProxyTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppManagerProxyTest.class);
        return suite;
    }
}
