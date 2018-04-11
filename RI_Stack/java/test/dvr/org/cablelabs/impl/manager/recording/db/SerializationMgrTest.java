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

package org.cablelabs.impl.manager.recording.db;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.impl.manager.RecordingDBManagerTest;
import org.cablelabs.impl.manager.recording.db.SerializationMgr.SegmentedLeaf;
import org.cablelabs.impl.persistent.PersistentData;
import org.cablelabs.impl.persistent.PersistentDataSerializer;
import org.cablelabs.impl.persistent.PersistentDataSerializerAbstractTest;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.test.TestUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.ocap.net.OcapLocator;

/**
 * Tests SerializationMgr.
 * 
 * @author Aaron Kamienski
 */
public class SerializationMgrTest extends PersistentDataSerializerAbstractTest
{
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(SerializationMgr.class);
    }

    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(SerializationMgr.class);
    }

    public SerializationMgrTest(String name)
    {
        super(name);
    }

    /**
     * Tests that baseDir/prefix is defined as expected.
     * 
     * BaseDir is expected to be defined: - as
     * ($OCAP.persistent.dvr|/syscwd)/recdbser
     * 
     * Prefix is expected to be defined: - $org.cablelabs.ocap.dvr.serial.prefix
     * - else empty
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testBaseDir_noPrefix() throws Exception
    {
        deleteUniqueId();

        File rootDir = new File(MPEEnv.getEnv("OCAP.persistent.dvr", SerializationMgr.DEFAULT_DIR));
        baseDir = new File(rootDir, SerializationMgr.DEFAULT_SUBDIR).getAbsoluteFile();
        prefix = "";
        MPEEnv.setEnv(SerializationMgr.BASEDIR_PROP, rootDir.getPath());
        MPEEnv.removeEnvOverride(SerializationMgr.PREFIX_PROP);
        pds = sm = new SerializationMgr();
        testFilenames();
    }

    /**
     * Tests that baseDir/prefix is defined as expected.
     * 
     * BaseDir is expected to be defined: - as
     * ($OCAP.persistent.dvr|/syscwd)/recdbser
     * 
     * Prefix is expected to be defined: - $org.cablelabs.ocap.dvr.serial.prefix
     * - else empty
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testBaseDir_Prefix() throws Exception
    {
        deleteUniqueId();

        File rootDir = new File(MPEEnv.getEnv("OCAP.persistent.dvr", SerializationMgr.DEFAULT_DIR));
        baseDir = new File(rootDir, SerializationMgr.DEFAULT_SUBDIR).getAbsoluteFile();
        prefix = getName();
        MPEEnv.setEnv(SerializationMgr.BASEDIR_PROP, rootDir.getPath());
        MPEEnv.setEnv(SerializationMgr.PREFIX_PROP, prefix);
        pds = sm = new SerializationMgr();
        testFilenames();
    }

    /**
     * Tests initialization of <code>lastUniqueId</code> across multiple
     * instances.
     */
    public void testLastUniqueId_init()
    {
        // Create a SerializationMgr (done in setup)
        // Create a few instances of PersistentData
        PersistentData data[] = new PersistentData[4];
        for (int i = 0; i < data.length; ++i)
        {
            data[i] = sm.newRecord();
        }
        // Don't bother saving them (in fact, could delete them)

        // Create a new SerializationMgr
        pds = sm = (SerializationMgr) createInstance(baseDir, prefix);

        // Next instance of PersistentData should have next unique ID (as
        // opposed to 0)
        PersistentData next = sm.newRecord();
        assertTrue("Expected " + next.uniqueId + " to be greater than " + data[data.length - 1].uniqueId,
                next.uniqueId > data[data.length - 1].uniqueId);
    }

    /**
     * Tests range of lastUniqueId -- should be limited to 32-bits.
     */
    public void testLastUniqueId_range()
    {
        // Create a SerializationMgr that extends current SerializationMgr...
        // Override init of lastUniqueId so that a value of 0xFFFFFFFE is
        // returned.
        pds = sm = new SerializationMgr(baseDir, prefix, true)
        {
            protected long initLastUniqueId(File base)
            {
                return 0xFFFFFFFE;
            }
        };
        // Create a PersistentData, should be 0xFFFFFFFF
        PersistentData data1 = sm.newRecord();
        assertEquals("Expected uniqueId of 0xFFFFFFFF", 0xFFFFFFFF, data1.uniqueId);
        // Create a PersistentData, should be 0x00000000
        PersistentData data2 = sm.newRecord();
        assertEquals("Expected uniqueId of 0x0", 0x0, data2.uniqueId);
    }

    /**
     * Tests what happens when the lastUniqueId value stored in persistent
     * storage is corrupted.
     */
    public void testLastUniqueId_corrupt() throws Exception
    {
        // Create a SerializationMgr (done in setup)
        // Create a few instances of PersistentData
        RecordingInfoNode data[] = new RecordingInfoNode[4];
        for (int i = 0; i < data.length; ++i)
        {
            data[i] = sm.newRecord();
        }
        // Don't bother saving them (in fact, could delete them)

        // Corrupt the lastUniqueId file (e.g., change contents)
        long[] id = loadUniqueId();
        id[1] = id[0];
        storeUniqueId(id);

        // Create a SerializationMgr
        pds = sm = (SerializationMgr) createInstance(baseDir, prefix);
        // Create a PersistentData, uniqueId should be 1
        RecordingInfoNode data1 = sm.newRecord();
        assertEquals("Unexpected uniqueId given corrupted storage", 1, data1.uniqueId);

        // Now repeat, but save the persistent data
        for (int i = 0; i < data.length; ++i)
        {
            data[i] = sm.newRecord();
            records.addElement(data[i]);
            sm.saveRecord(data[i], 0);
        }
        id = loadUniqueId();
        id[1] = id[0];
        storeUniqueId(id);

        // At which point the lastUniqueId should be the last one as normal
        pds = sm = (SerializationMgr) createInstance(baseDir, prefix);
        // Create a PersistentData, uniqueId should be greater than last
        RecordingInfoNode data2 = sm.newRecord();
        assertTrue("Expected " + data2.uniqueId + " to be greater than " + data[data.length - 1].uniqueId,
                data2.uniqueId > data[data.length - 1].uniqueId);
    }

    private long[] loadUniqueId() throws Exception
    {
        File f = new File(baseDir, SerializationMgr.UNIQUEID);
        assertTrue("Expected " + f + " to exist", f.exists());

        DataInputStream dis = new DataInputStream(new FileInputStream(f));
        long[] data = new long[2];
        data[0] = dis.readLong();
        data[1] = dis.readLong();
        dis.close();
        return data;
    }

    private void storeUniqueId(long[] data) throws Exception
    {
        File f = new File(baseDir, SerializationMgr.UNIQUEID);

        DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
        dos.writeLong(data[0]);
        dos.writeLong(data[1]);
        dos.close();
    }

    private void deleteUniqueId()
    {
        deleteUniqueId(baseDir);
    }

    private static void deleteUniqueId(File base)
    {
        File f = new File(base, SerializationMgr.UNIQUEID);

        f.delete();
    }

    /**
     * Tests what happens if the stored lastUniqueId is invalid. (I.e., it isn't
     * really the <i>last</i> unique ID).
     */
    public void testLastUniqueId_invalid() throws Exception
    {
        // Create a SerializationMgr (done in setup)
        // Create a few instances of PersistentData, and save
        RecordingInfoNode data[] = new RecordingInfoNode[4];
        for (int i = 0; i < data.length; ++i)
        {
            data[i] = sm.newRecord();
            sm.saveRecord(data[i], 0);
        }

        // Copy contents of persistent lastUniqueId
        long[] id = loadUniqueId();

        // Create a few more instances, and save
        RecordingInfoNode data2[] = new RecordingInfoNode[4];
        for (int i = 0; i < data2.length; ++i)
        {
            data2[i] = sm.newRecord();
            sm.saveRecord(data2[i], 0);
        }

        // Replace persistent lastUniqueId with old
        storeUniqueId(id);

        // Create a SerializationMgr
        pds = sm = (SerializationMgr) createInstance(baseDir, prefix);

        // Create a new instance - uniqueId should be after others
        RecordingInfoNode next = sm.newRecord();
        assertTrue("Expected " + next.uniqueId + " to be greater than " + data2[data2.length - 1].uniqueId,
                next.uniqueId > data2[data2.length - 1].uniqueId);
    }

    protected SerializationMgr sm;

    protected void setUp() throws Exception
    {
        super.setUp();
        sm = (SerializationMgr) pds;
        baseDirProp = MPEEnv.getEnv(SerializationMgr.BASEDIR_PROP);
        prefixProp = MPEEnv.getEnv(SerializationMgr.PREFIX_PROP);
    }

    protected void tearDown() throws Exception
    {
        if (baseDirProp != null)
            MPEEnv.setEnv(SerializationMgr.BASEDIR_PROP, baseDirProp);
        else
            MPEEnv.removeEnvOverride(SerializationMgr.BASEDIR_PROP);
        if (prefixProp != null)
            MPEEnv.setEnv(SerializationMgr.PREFIX_PROP, prefixProp);
        else
            MPEEnv.removeEnvOverride(SerializationMgr.PREFIX_PROP);

        deleteUniqueId();

        super.tearDown();
    }

    protected String baseDirProp;

    protected String prefixProp;

    protected PersistentDataSerializer createInstance(File base, String pfx)
    {
        return new SerializationMgr(base, pfx, true);
    }

    protected PersistentData createData(long i)
    {
        int idx = (int) i;
        RecordingInfo2 info = new SegmentedLeaf(i); // This is what is used
                                                    // internally by
                                                    // SerializationMgr

        info.setAppId(new AppID(idx * 10 + 1, idx * 10 + 1));
        info.setExpirationDate(new Date(System.currentTimeMillis()));
        info.setOrganization("org");
        info.setServiceLocators(new OcapLocator[0]);
        info.setLongServiceName("service");
        info.setBitRate((byte) 1);

        RecordedSegmentInfo rsi = new RecordedSegmentInfo();
        rsi.setNativeRecordingName("id");

        info.addRecordedSegmentInfo(rsi);

        return info;
    }

    protected void modifyData(PersistentData data) throws Exception
    {
        RecordingInfo2 info = (RecordingInfo2) data;

        info.setExpirationDate(new Date(info.getExpirationDate().getTime() + 1));
        info.setOrganization(info.getOrganization() + "_org");
        OcapLocator[] old = info.getServiceLocators();
        info.setServiceLocators(new OcapLocator[old.length + 1]);
        if (old.length > 0) System.arraycopy(old, 0, info.getServiceLocators(), 1, old.length);
        info.getServiceLocators()[0] = new OcapLocator(
                (info.getAppId().getAID() + info.getServiceLocators().length) & 0xFFFF);

        RecordedSegmentInfo rsi = new RecordedSegmentInfo();

        rsi.setNativeRecordingName(info.getFirstRecordedSegment().getNativeRecordingName() + "_id");

        info.addRecordedSegmentInfo(rsi);
    }

    protected boolean isEquals(PersistentData d0, PersistentData d1)
    {
        return d0.equals(d1);
    }

    protected void doSave(PersistentDataSerializer serializer, PersistentData data) throws IOException
    {
        ((SerializationMgr) serializer).saveRecord((RecordingInfo2) data, 0);
    }

    protected void doDelete(PersistentDataSerializer serializer, PersistentData data)
    {
        ((SerializationMgr) serializer).deleteRecord((RecordingInfo2) data);
    }

    protected Vector doLoad(PersistentDataSerializer serializer)
    {
        return ((SerializationMgr) serializer).loadRecords();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SerializationMgrTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            String prefix;

            File root = new File(MPEEnv.getEnv(SerializationMgr.BASEDIR_PROP, MPEEnv.getEnv("dvb.persistent.root",
                    "/syscwd")), "rdbtest");

            File baseDir = new File(root, SerializationMgr.DEFAULT_SUBDIR);

            public Object createImplObject()
            {
                prefix = MPEEnv.setEnv(SerializationMgr.PREFIX_PROP, "sermgrtest");
                MPEEnv.setEnv(SerializationMgr.BASEDIR_PROP, root.getAbsolutePath());
                return SerializationMgr.getInstance();
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
                MPEEnv.setEnv(SerializationMgr.PREFIX_PROP, (prefix == null) ? "" : prefix);
                deleteUniqueId(baseDir);
            }
        };
        InterfaceTestSuite ifts = RecordingDBManagerTest.isuite();
        ifts.addFactory(factory);
        suite.addTest(ifts);

        return suite;
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(SerializationMgrTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new SerializationMgrTest(tests[i]));
            return suite;
        }
    }

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
}
