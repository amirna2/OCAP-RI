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
package org.cablelabs.impl.util;

import junit.framework.TestCase;

import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.service.ServiceDetailsExt;

public class PidMapTableTest extends TestCase
{
    CannedServiceMgr csm;

    CannedSIDatabase sidb;

    PidMapEntry entry0;

    PidMapEntry entry1;

    PidMapEntry entry2;

    PidMapEntry entry3;

    PidMapEntry entry4;

    public void setUp() throws Exception
    {
        super.setUp();
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        sidb = (CannedSIDatabase) csm.getSIDatabase();

        entry0 = new PidMapEntry((short) 0, (short) 0, 0, (short) 0, 0, sidb.jmfServiceComponent1A1);
        entry1 = new PidMapEntry((short) 1, (short) 1, 1, (short) 1, 1, sidb.jmfServiceComponent1A1);
        entry2 = new PidMapEntry((short) 2, (short) 2, 2, (short) 2, 2, sidb.jmfServiceComponent1A1);
        entry3 = new PidMapEntry((short) 3, (short) 3, 3, (short) 3, 3, sidb.jmfServiceComponent1A1);
        entry4 = new PidMapEntry((short) 4, (short) 4, 4, (short) 4, 4, sidb.jmfServiceComponent1A1);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        csm.destroy();
    }

    public void testConstructorGetSize()
    {
        PidMapTable table = new PidMapTable(4);
        assertTrue(table.getSize() == 4);
    }

    public void testGetSetServiceDetails()
    {
        PidMapTable table = new PidMapTable(4);
        ServiceDetailsExt sd = sidb.jmfServiceDetails2;
        table.setServiceDetails(sd);
        assertTrue(sd.equals(table.getServiceDetails()));
    }

    public void testAddGetEntryAtIndex()
    {
        PidMapTable table = new PidMapTable(4);
        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);

        PidMapEntry testEntry = table.getEntryAtIndex(2);
        assertTrue(testEntry.equals(entry2));

        assertTrue(table.getEntryAtIndex(8) == null);
    }

    public void testFindEntryBySourcePID()
    {
        PidMapTable table = new PidMapTable(5);
        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);
        table.addEntryAtIndex(4, entry4);

        PidMapEntry testEntry = table.findEntryBySourcePID(entry3.getSourcePID());
        assertTrue(testEntry.equals(entry3));

        testEntry = table.findEntryBySourcePID(-1);
        assertTrue(testEntry == null);
    }

    public void testFindEntryByRecordedPID()
    {
        PidMapTable table = new PidMapTable(5);
        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);
        table.addEntryAtIndex(4, entry4);

        PidMapEntry testEntry = table.findEntryByRecordedPID(entry3.getSourcePID());
        assertTrue(testEntry.equals(entry3));

        testEntry = table.findEntryByRecordedPID(-1);
        assertTrue(testEntry == null);
    }

    public void testFindEntryBySourcePIDSourceStreamType()
    {
        PidMapTable table = new PidMapTable(5);
        entry4.setSourcePID(entry3.getSourcePID());

        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);
        table.addEntryAtIndex(4, entry4);

        PidMapEntry testEntry = table.findEntryBySourcePIDSourceStreamType(entry3.getSourcePID(),
                entry3.getSourceElementaryStreamType());
        assertTrue(testEntry.equals(entry3));

        testEntry = table.findEntryBySourcePIDSourceStreamType(-1, (short) -1);
        assertTrue(testEntry == null);
    }

    public void testFindEntryByRecordedPIDRecordedStreamType()
    {
        PidMapTable table = new PidMapTable(5);
        entry4.setSourcePID(entry3.getSourcePID());

        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);
        table.addEntryAtIndex(4, entry4);

        PidMapEntry testEntry = table.findEntryByRecordedPIDRecordedStreamType(entry3.getRecordedPID(),
                entry3.getRecordedElementaryStreamType());
        assertTrue(testEntry.equals(entry3));

        testEntry = table.findEntryByRecordedPIDRecordedStreamType(-1, (short) -1);
        assertTrue(testEntry == null);
    }

    public void testEntry()
    {
        PidMapTable table = new PidMapTable(4);

        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);

        assertTrue(table.findEntry(entry1));
        assertTrue(table.findEntry(entry0));
        assertTrue(table.findEntry(entry3));
        assertFalse(table.findEntry(entry4));
    }

    public void testFindEntryByServiceComponent()
    {
        PidMapTable table = new PidMapTable(5);

        table.addEntryAtIndex(0, entry0);
        table.addEntryAtIndex(1, entry1);
        table.addEntryAtIndex(2, entry2);
        table.addEntryAtIndex(3, entry3);
        table.addEntryAtIndex(4, entry4);
        PidMapEntry testEntry = table.findEntryByServiceComponent(entry0.getServiceComponentReference());
        assertTrue(testEntry.equals(entry0));

        testEntry = table.findEntryByServiceComponent(sidb.serviceComponent105);
        assertTrue(testEntry == null);
    }
}
