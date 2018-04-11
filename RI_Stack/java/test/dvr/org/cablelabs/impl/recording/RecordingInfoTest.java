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

package org.cablelabs.impl.recording;

import org.cablelabs.impl.persistent.PersistentData;
import org.cablelabs.impl.persistent.PersistentDataAbstractTest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Hashtable;

import javax.media.Time;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;

import org.ocap.dvr.OcapRecordingProperties;

/**
 * Tests RecordingInfo
 * 
 * @author Aaron Kamienski
 */
public class RecordingInfoTest extends PersistentDataAbstractTest
{
    static class TestAppData implements java.io.Serializable
    {
        private int m_cnt = 0;

        public int getData()
        {
            return m_cnt;
        }

        TestAppData(int cnt)
        {
            m_cnt = cnt;
        }

        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            return m_cnt == ((TestAppData) obj).getData();
        }

        public int hashCode()
        {
            int hashValue = 3;
            int code = (m_cnt + 11) ^ hashValue;
            // System.out.println("Hashcode for = " +m_cnt + " is " +code);
            return code;
        }
    }

    public void testAppData() throws Exception // 1
    {
        TestAppData d1 = new TestAppData(100);
        TestAppData d2 = new TestAppData(100);
        TestAppData d3 = new TestAppData(3);

        assertTrue("testAppData", d1.equals(d2));

        assertFalse("testAppData", d1.equals(d3));
    }

    /**
     * Tests constructor.
     */
    public void testConstructor() // 2
    {
        RecordingInfo info = new RecordingInfo(20);
        assertEquals("Expected uniqueId to be set", 20, info.uniqueId);
    }

    public static void assertNotEquals(String msg, Object o1, Object o2)
    {
        assertFalse(msg, o1.equals(o2));
    }

    public static void assertNotEquals(String msg, int o1, int o2)
    {
        assertFalse(msg, o1 == o2);
    }

    public static void assertNotEquals(String msg, long o1, long o2)
    {
        assertFalse(msg, o1 == o2);
    }

    /**
     * Tests equals() and hashcode().
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testEquals() throws Exception // 3
    {
        RecordingInfo info1 = new RecordingInfo(1);
        RecordingInfo info2 = new RecordingInfo(1);
        RecordingInfo info3 = new RecordingInfo(2);

        assertEquals("Unexpected equals() result - same instance", info1, info1);
        assertEquals("Unexpected equals() result - same uniqueId", info1, info2);
        assertEquals("Unexpected hashCode() result - same uniqueId", info1, info2);
        assertNotEquals("Unexpected equals() result - different uniqueId", info1, info3);
        assertNotEquals("Unexpected hashCode() result - different uniqueId", info1.hashCode(), info3.hashCode());

        // appid
        info1.setAppId(new AppID(10, 10));
        info2.setAppId(new AppID(10, 10));
        assertEquals("Unexpected equals() result - same appID", info1, info2);
        assertEquals("Unexpected hashCode() result - same appID", info1.hashCode(), info2.hashCode());
        info2.setAppId(new AppID(10, 11));
        assertNotEquals("Unexpected equals() result - different appID", info1, info2);
        info2.setAppId(null);
        assertNotEquals("Unexpected equals() result - different appID", info1, info2);
        info2.setAppId(info1.getAppId());

        // expirationDate
        info1.setExpirationDate(new Date());
        info2.setExpirationDate(new Date(info1.getExpirationDate().getTime()));
        assertEquals("Expected dates to be equal", info1.getExpirationDate(), info2.getExpirationDate());
        assertEquals("Unexpected equals() result - same expirationDate", info1, info2);
        assertEquals("Unexpected hashCode() result - same expirationDate", info1.hashCode(), info2.hashCode());
        info2.setExpirationDate(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
        assertNotEquals("Unexpected equals() result - different expirationDate", info1, info2);
        info2.setExpirationDate(null);
        assertNotEquals("Unexpected equals() result - different expirationDate", info1, info2);
        info2.setExpirationDate(info1.getExpirationDate());

        // fap
        info1.setFap(new ExtendedFileAccessPermissions(true, false, false, true, true, true, null, new int[] { 1, 2 }));
        info2.setFap(new ExtendedFileAccessPermissions(true, false, false, true, true, true, null, new int[] { 1, 2 }));
        assertEquals("Unexpected equals() result - same fap", info1, info2);
        assertEquals("Unexpected hashCode() result - same fap", info1.hashCode(), info2.hashCode());
        info2.setFap(new ExtendedFileAccessPermissions(true, false, false, true, true, true, new int[0],
                new int[] { 5 }));
        assertNotEquals("Unexpected equals() result - different fap", info1, info2);
        info2.setFap(null);
        assertNotEquals("Unexpected equals() result - different fap", info1, info2);
        info2.setFap(info1.getFap());

        // priority
        info1.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
        info2.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
        assertEquals("Unexpected equals() result - same priority", info1, info2);
        assertEquals("Unexpected hashCode() result - same priority", info1.hashCode(), info2.hashCode());
        info2.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
        assertNotEquals("Unexpected equals() result - different priority", info1, info2);
        info2.setPriority(info1.getPriority());

        // bitrate
        info1.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        info2.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        assertEquals("Unexpected equals() result - same bitRate", info1, info2);
        assertEquals("Unexpected hashCode() result - same bitRate", info1.hashCode(), info2.hashCode());
        info2.setBitRate(OcapRecordingProperties.LOW_BIT_RATE);
        assertNotEquals("Unexpected equals() result - different bitRate", info1, info2);
        info2.setBitRate(info1.getBitRate());

        // duration
        info1.setDuration(60 * 1000 * 1000);
        info2.setDuration(60 * 1000 * 1000);
        assertEquals("Unexpected equals() result - same duration", info1, info2);
        assertEquals("Unexpected hashCode() result - same duration", info1.hashCode(), info2.hashCode());
        info2.setDuration(60 * 1000 * 1000 + 1);
        assertNotEquals("Unexpected equals() result - different duration", info1, info2);
        info2.setDuration(info1.getDuration());

        // startTime
        info1.setStartTime(19);
        info2.setStartTime(19);
        assertEquals("Unexpected equals() result - same startTime", info1, info2);
        assertEquals("Unexpected hashCode() result - same startTime", info1.hashCode(), info2.hashCode());
        info2.setStartTime(20);
        assertNotEquals("Unexpected equals() result - different startTime", info1, info2);
        info2.setStartTime(info1.getStartTime());

        // actualStartTime
        info1.setActualStartTime(19);
        info2.setActualStartTime(19);
        assertEquals("Unexpected equals() result - same actualStartTime", info1, info2);
        assertEquals("Unexpected hashCode() result - same actualStartTime", info1.hashCode(), info2.hashCode());
        info2.setActualStartTime(20);
        assertNotEquals("Unexpected equals() result - different actualStartTime", info1, info2);
        info2.setActualStartTime(info1.getActualStartTime());

        // state
        info1.setState(1);
        info2.setState(1);
        assertEquals("Unexpected equals() result - same state", info1, info2);
        assertEquals("Unexpected hashCode() result - same state", info1.hashCode(), info2.hashCode());
        info2.setState(2);
        assertNotEquals("Unexpected equals() result - different state", info1, info2);
        info2.setState(info1.getState());

        // serviceLocator
        info1.setServiceLocator(new OcapLocator[] { new OcapLocator("ocap://0x457"), null, new OcapLocator(4) });

        info2.setServiceLocator(new OcapLocator[] { new OcapLocator("ocap://0x457"), null, new OcapLocator(4) });

        assertEquals("Unexpected equals() result - same serviceLocator", info1, info2);
        assertEquals("Unexpected hashCode() result - same serviceLocator", info1.hashCode(), info2.hashCode());
        info2.getServiceLocator()[1] = info2.getServiceLocator()[2];
        assertNotEquals("Unexpected equals() result - different serviceLocator", info1, info2);
        info2.setServiceLocator(new OcapLocator[0]);
        assertNotEquals("Unexpected equals() result - different serviceLocator", info1, info2);
        info2.setServiceLocator(null);
        assertNotEquals("Unexpected equals() result - different serviceLocator", info1, info2);
        info2.setServiceLocator(info1.getServiceLocator());

        // serviceName
        info1.setServiceName("hbo");
        info2.setServiceName("hbo");
        assertEquals("Unexpected equals() result - same serviceName", info1, info2);
        assertEquals("Unexpected hashCode() result - same serviceName", info1.hashCode(), info2.hashCode());
        info2.setServiceName("max");
        assertNotEquals("Unexpected equals() result - different serviceName", info1, info2);
        info2.setServiceName(null);
        assertNotEquals("Unexpected equals() result - different serviceName", info1, info2);
        info2.setServiceName(info1.getServiceName());

        // recordingId
        info1.setRecordingId("blahblah");
        info2.setRecordingId("blahblah");
        assertEquals("Unexpected equals() result - same recordingId", info1, info2);
        assertEquals("Unexpected hashCode() result - same recordingId", info1.hashCode(), info2.hashCode());
        info2.setRecordingId("blah");
        assertNotEquals("Unexpected equals() result - different recordingId", info1, info2);
        info2.setRecordingId(null);
        assertNotEquals("Unexpected equals() result - different recordingId", info1, info2);
        info2.setRecordingId(info1.getRecordingId());

        // organization
        info1.setOrganization("hbo");
        info2.setOrganization("hbo");
        assertEquals("Unexpected equals() result - same organization", info1, info2);
        assertEquals("Unexpected hashCode() result - same organization", info1.hashCode(), info2.hashCode());
        info2.setOrganization("max");
        assertNotEquals("Unexpected equals() result - different organization", info1, info2);
        info2.setOrganization(null);
        assertNotEquals("Unexpected equals() result - different organization", info1, info2);
        info2.setOrganization(info1.getOrganization());

        // appDataTable
        info1.setAppDataTable(new Hashtable(2));
        info2.setAppDataTable(new Hashtable(2));

        Hashtable dataMap1 = new Hashtable(2);
        Hashtable dataMap2 = new Hashtable(2);

        info1.getAppDataTable().put("applicationKey", dataMap1);
        info2.getAppDataTable().put("applicationKey", dataMap2);

        dataMap1.put("appData", new TestAppData(1));
        dataMap2.put("appData", new TestAppData(1));
        assertEquals("appData", info1.getAppDataTable(), info2.getAppDataTable());
        info1.setAppDataTable(null);
        info2.setAppDataTable(null);

        // deletion details
        /*
         * long time = System.currentTimeMillis(); info1.deletionReason =
         * DeletionDetails.EXPIRED; info1.deletionTime = time;
         * info2.deletionReason = DeletionDetails.EXPIRED; info2.deletionTime =
         * time + 1; assertEquals("Unexpected equals() result - same deletion
         * details", info1, info2);
         * 
         * info2.deletionReason = DeletionDetails.USER_DELETED;
         * assertNotEquals("Unexpected equals() result - different deletion
         * details", info1, info2);
         */

        // media times
        info1.setMediaTime((new javax.media.Time(2)).getNanoseconds());
        info2.setMediaTime(new javax.media.Time(2).getNanoseconds());

        assertEquals("BAD", info1.getMediaTime(), info2.getMediaTime());

        assertEquals("Unexpected equals() result - same mediaTime", info1.getMediaTime(), info2.getMediaTime());

        assertEquals("Unexpected equals() result - same mediaTime", info1, info2);

        info2.setMediaTime(new Time(3).getNanoseconds());
        assertNotEquals("Unexpected equals() result - different mediaTimes", info1, info2);
    }

    /**
     * Tests hashCode(). Mostly tested in testEquals().
     */
    public void testHashcode() // 4
    {
        RecordingInfo info1 = new RecordingInfo(1);
        RecordingInfo info2 = new RecordingInfo(1);

        assertEquals("Expected same hashCode for equivalent records", info1.hashCode(), info2.hashCode());
    }

    /**
     * Tests toString().
     */
    public void testToString() throws Exception // 5
    {
        RecordingInfo info;
        for (int i = 0; i < 10; ++i)
        {
            info = new RecordingInfo(i * 10 + i);
            if (0 == (i % 3))
            {
                info.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
                info.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
            }
            else if (1 == (i % 3))
            {
                info.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
                info.setBitRate(OcapRecordingProperties.LOW_BIT_RATE);
            }
            else if (2 == (i % 3))
            {
                info.setPriority(OcapRecordingProperties.TEST_RECORDING);
                info.setBitRate(OcapRecordingProperties.MEDIUM_BIT_RATE);
            }
            info.setStartTime(System.currentTimeMillis());
            info.setActualStartTime(System.currentTimeMillis());
            info.setExpirationDate(((i & 4) == 0) ? null : (new Date()));
            info.setState(i);
            info.setAppId((i == 0) ? null : (new AppID(i + 1, i + 1)));
            info.setOrganization((i == 2) ? null : "cafe");
            info.setFap(((i & 1) != 0) ? null : (new ExtendedFileAccessPermissions(i != 0, i != 1, i != 2, i != 3,
                    i != 4, i != 5, (i != 1) ? null : (new int[i]), (i != 3) ? null : (new int[] { i }))));
            if ((i & 1) == 0)
                info.setServiceLocator(null);
            else
            {
                info.setServiceLocator(new OcapLocator[i]);
                for (int j = 0; j < i; ++j)
                {
                    info.getServiceLocator()[j] = ((j & 2) == 0) ? null : (new OcapLocator(0x20000000 + i, j * 2, -1));
                }
            }
            info.setServiceName(((i & 4) != 0) ? null : ("svc" + i));
            info.setRecordingId(((i & 8) != 0) ? null : ("id" + i));

            // Just verify that a string is returned, w/out exceptions
            try
            {
                String str = info.toString();

                assertNotNull("Expect toString() to return non-null", str);
                // Following is somewhat arbitrary
                // But figure there should at least be the name and info for
                // each field
                assertTrue("Expect toString() to return string with more chars",
                        "RecordingInfo".length() + 15 < str.length());
            }
            catch (Exception e)
            {
                fail("Unexpected exception thrown by toString() " + e);
            }
        }
    }

    /**
     * Creates an instance of RecordingInfo. Used by
     * PersistentDataTest.testSerialization().
     */
    protected PersistentData createInstance(int i) throws Exception
    {
        RecordingInfo info = new RecordingInfo(i * 10 + i);

        //
        // info.deletionReason = ((i & 4)!=0) ? DeletionDetails.EXPIRED :
        // DeletionDetails.USER_DELETED;
        // info.deletionTime = ((i & 4)!=0) ? System.currentTimeMillis() : 100;

        info.setMediaTime(new javax.media.Time(1).getNanoseconds());

        // Each RecordingInfo has it's own hash table.
        // Each hashtable contains the same key mapping
        // to a Serializable object that overrides equals.
        info.setAppDataTable(new Hashtable());
        for (int j = 0; j < 2; j++)
        {
            Hashtable ht = new Hashtable();
            info.getAppDataTable().put("app_" + j, ht);
            for (int k = 0; k < 2; k++)
            {
                ht.put("one_" + k, new AppDataContainer(new TestAppData(k)));
            }
        }

        if (0 == (i % 3))
        {
            info.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
            info.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        }
        else if (1 == (i % 3))
        {
            info.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
            info.setBitRate(OcapRecordingProperties.LOW_BIT_RATE);
        }
        else if (2 == (i % 3))
        {
            info.setPriority(OcapRecordingProperties.TEST_RECORDING);
            info.setBitRate(OcapRecordingProperties.MEDIUM_BIT_RATE);
        }
        info.setExpirationDate(((i & 4) == 0) ? null : new Date());
        info.setStartTime(System.currentTimeMillis());
        info.setActualStartTime(System.currentTimeMillis());
        info.setState(i);
        info.setAppId((i == 0) ? null : (new AppID(i + 1, i + 1)));
        info.setOrganization((i == 2) ? null : "beef");
        info.setFap(((i & 1) == 0) ? null : (new ExtendedFileAccessPermissions(i == 0, i == 1, i == 2, i == 3, i == 4,
                i == 5, (i == 1) ? null : (new int[i]), (i == 3) ? null : (new int[] { i }))));
        if ((i & 1) != 0)
            info.setServiceLocator(null);
        else
        {
            info.setServiceLocator(new OcapLocator[i]);
            for (int j = 0; j < i; ++j)
            {
                info.getServiceLocator()[j] = ((j & 2) != 0) ? null : (new OcapLocator(0x20000000 + i * j, i, -1));
            }
        }
        info.setServiceName(((i & 4) == 0) ? null : ("svc" + i));
        info.setRecordingId(((i & 8) == 0) ? null : ("id" + i));

        return info;
    }

    protected boolean isEquals(PersistentData d0, PersistentData d1)
    {
        if (d0 instanceof RecordingInfo && d1 instanceof RecordingInfo)
            return d0.equals(d1);
        else
            return false;
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
        TestSuite suite = new TestSuite(RecordingInfoTest.class);
        return suite;
    }

    public RecordingInfoTest(String name)
    {
        super(name);
    }

}
