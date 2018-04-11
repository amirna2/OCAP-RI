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

import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import java.util.*;
import junit.framework.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.dvb.application.*;
import org.ocap.net.OcapLocator;
import org.ocap.storage.*;
import org.ocap.dvr.OcapRecordingProperties;

/**
 * Tests RecordingDBManager. This is an interface testcase!
 * 
 * @author Aaron Kamienski
 */
public class RecordingDBManagerTest extends ManagerTest
{
    /**
     * Tests newRecord.
     */
    public void testNewRecord()
    {
        Hashtable hash = new Hashtable();
        RecordingInfo2 last = null;
        for (int i = 0; i < 10; ++i)
        {
            RecordingInfo2 info = dbmgr.newRecord();

            assertNotNull("should not return null", info);
            assertTrue("should not return same as last call", last != info);
            last = info;

            Long lng = new Long(info.uniqueId);
            assertTrue("uniqueId's aren't unique", hash.get(lng) == null);
            hash.put(lng, lng);
        }
    }

    /**
     * Tests saveRecord().
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testSaveRecord() throws Exception
    {
        RecordingInfo2 record = dbmgr.newRecord();
        records.addElement(record);

        // Initialize record
        record.setAppId(new AppID(1000, 1000));
        record.setExpirationDate(new Date());
        record.setFap(new ExtendedFileAccessPermissions(true, false, false, true, true, true, new int[] { 25, 32 },
                new int[] { 33, 35 }));

        record.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
        record.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        record.setRequestedDuration(60 * 1000 * 1000);
        record.setRequestedStartTime(System.currentTimeMillis());
        record.setState(3);
        record.setServiceLocators(null);

        RecordedSegmentInfo rsi = new RecordedSegmentInfo(null, "id", System.currentTimeMillis(), 0);

        record.addRecordedSegmentInfo(rsi);

        // Save record
        dbmgr.saveRecord(record, 0);

        // Now loadRecords
        Vector v = dbmgr.loadRecords();
        assertEquals("Expected only one record", 1, v.size());
        RecordingInfo2 record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record", record, record2);

        // Modify record
        record.setServiceLocators(new OcapLocator[1]);
        record.getServiceLocators()[0] = new OcapLocator("ocap://0x457");
        record.setLongServiceName("blah");
        record.getFirstRecordedSegment().setNativeRecordingName("xyzabced");

        // Save record
        dbmgr.saveRecord(record, 0);

        // Now loadRecords
        v = dbmgr.loadRecords();
        assertEquals("Expected only one record (modified)", 1, v.size());
        record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record (modified)", record, record2);

        // Modify record again
        record.setExpirationDate(new Date());
        record.getFap().setPermissions(true, true, false, false, true, true, new int[] { 10 }, null);
        record.setPriority(OcapRecordingProperties.TEST_RECORDING);
        record.setLongServiceName("");
        record.getFirstRecordedSegment().setNativeRecordingName(null);

        // Save record
        dbmgr.saveRecord(record, 0);

        // Now loadRecords
        v = dbmgr.loadRecords();
        assertEquals("Expected only one record (modified2)", 1, v.size());
        record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record (modified2)", record, record2);
    }

    /**
     * Tests saveRecord() w/ update flags.
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testUpdateRecord() throws Exception
    {
        RecordingInfo2 record = dbmgr.newRecord();
        records.addElement(record);

        // Initialize record
        record.setAppId(new AppID(1000, 1000));
        record.setExpirationDate(new Date());
        record.setFap(new ExtendedFileAccessPermissions(true, false, false, true, true, true, new int[] { 25, 32 },
                new int[] { 33, 35 }));

        record.setPriority(OcapRecordingProperties.RECORD_IF_NO_CONFLICTS);
        record.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        record.setRequestedDuration(60 * 1000 * 1000);
        record.setRequestedStartTime(System.currentTimeMillis());
        record.setState(3);
        record.setServiceLocators(null);
        record.setLongServiceName(null);
        RecordedSegmentInfo rsi = new RecordedSegmentInfo();
        rsi.setActualStartTime(System.currentTimeMillis());
        rsi.setNativeRecordingName(null);
        record.addRecordedSegmentInfo(rsi);

        // Save record
        dbmgr.saveRecord(record, RecordingDBManager.ALL);

        // Now loadRecords
        Vector v = dbmgr.loadRecords();
        assertEquals("Expected only one record", 1, v.size());
        RecordingInfo2 record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record", record, record2);

        // Modify record
        record.setServiceLocators(new OcapLocator[1]);
        record.getServiceLocators()[0] = new OcapLocator("ocap://0x457");
        record.setLongServiceName("blah");

        rsi = new RecordedSegmentInfo();
        rsi.setActualStartTime(System.currentTimeMillis());
        rsi.setNativeRecordingName("xyzabced");
        record.addRecordedSegmentInfo(rsi);

        // Save record
        dbmgr.saveRecord(record, RecordingDBManager.SERVICE_LOCATOR | RecordingDBManager.SERVICE_NAME
                | RecordingDBManager.RECORDING_ID);

        // Now loadRecords
        v = dbmgr.loadRecords();
        assertEquals("Expected only one record (modified)", 1, v.size());
        record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record (modified)", record, record2);

        // Modify record again
        record.setExpirationDate(new Date());
        record.getFap().setPermissions(true, true, false, false, true, true, new int[] { 10 }, null);
        record.setPriority(OcapRecordingProperties.TEST_RECORDING);
        record.setLongServiceName("");

        record.getFirstRecordedSegment().setNativeRecordingName(null);

        // Save record
        dbmgr.saveRecord(record, RecordingDBManager.EXPIRATION_DATE | RecordingDBManager.FAP
                | RecordingDBManager.PRIORITY | RecordingDBManager.SERVICE_NAME | RecordingDBManager.RECORDING_ID);

        // Now loadRecords
        v = dbmgr.loadRecords();
        assertEquals("Expected only one record (modified2)", 1, v.size());
        record2 = (RecordingInfo2) v.elementAt(0);

        // Validate against previous record
        assertEquals("Expected saved record to match loaded record (modified2)", record, record2);
    }

    /**
     * Tests deleteRecord().
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testDeleteRecord() throws Exception
    {
        RecordingInfo2[] r = new RecordingInfo2[5];

        for (int i = 0; i < r.length; ++i)
        {
            r[i] = dbmgr.newRecord();
            records.addElement(r[i]);
            dbmgr.saveRecord(r[i], 0);
        }

        Vector v = dbmgr.loadRecords();
        assertEquals("Unexpected number of records", r.length, v.size());
        for (int i = 0; i < r.length; ++i)
        {
            int idx = v.indexOf(r[i]);
            assertTrue("Expected record to be loaded", -1 != idx);
            v.removeElementAt(idx);
        }

        for (int i = 0; i < r.length; ++i)
        {
            dbmgr.deleteRecord(r[i]);

            v = dbmgr.loadRecords();
            assertEquals("Unexpected number of records after delete - " + i, r.length - i - 1, v.size());
            assertTrue("Did not expect record to be loaded", -1 == v.indexOf(r[i]));
        }
    }

    /**
     * Tests loadRecords().
     * 
     * @todo reenable once 5553 is fixed
     */
    public void testLoadRecords() throws Exception
    {
        for (int i = 0; i < 4; ++i)
        {
            // Examine twice, to make sure return equivalent
            for (int j = 0; j < 2; ++j)
            {
                Vector v = dbmgr.loadRecords();
                assertNotNull("Expected non-null to be returned", v);
                assertEquals("Unexpected size()", records.size(), v.size());

                for (int k = 0; k < records.size(); ++k)
                {
                    int idx = v.indexOf(records.elementAt(k));

                    assertTrue("Did not find expected element in loaded array - " + k, idx != -1);
                    v.removeElementAt(idx);
                }
            }

            // Add record for next loop...
            RecordingInfo2 r = dbmgr.newRecord();
            records.addElement(r);
            dbmgr.saveRecord(r, 0);
        }
    }

    /* Boilerplate */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(RecordingDBManagerTest.class);
        suite.setName(RecordingDBManager.class.getName());
        return suite;
    }

    public RecordingDBManagerTest(String name, ImplFactory f)
    {
        super(name, RecordingDBManager.class, f);
    }

    protected RecordingDBManager createRecordingDBManager()
    {
        return (RecordingDBManager) createManager();
    }

    private RecordingDBManager dbmgr;

    private Vector records = new Vector();

    protected void setUp() throws Exception
    {
        super.setUp();
        dbmgr = (RecordingDBManager) mgr;
    }

    protected void tearDown() throws Exception
    {
        for (Enumeration e = records.elements(); e.hasMoreElements();)
        {
            dbmgr.deleteRecord((RecordingInfo2) e.nextElement());
        }
        dbmgr = null;
        super.tearDown();
    }

    public static interface DBFactory extends ManagerTest.ManagerFactory
    {
        public void tearDown() throws Exception;
    }
}
