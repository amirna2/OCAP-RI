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
package org.cablelabs.impl.media.source;

import javax.media.Time;
import javax.tv.locator.Locator;

import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.CannedDVRAPI;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.protocol.recording.DataSource;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * RecordingDataSourceImplTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class RecordingDataSourceImplTest extends TestCase
{

    private DataSource rds;

    private CannedOcapRecordedServiceExt ors;

    private CannedRecordedServiceMediaLocator rsml;

    private MediaAPIManager oldAPI;

    private DVRAPIManager oldDVR;

    /**
     * 
     */
    public RecordingDataSourceImplTest()
    {
        super();
        // TODO (Josh) Implement
    }

    /**
     * @param name
     */
    public RecordingDataSourceImplTest(String name)
    {
        super(name);
        // TODO (Josh) Implement
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(RecordingDataSourceImplTest.class);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(RecordingDataSourceImplTest.class);
        suite.setName("RecordingDataSourceImplTest");
        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();

        oldAPI = (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);
        CannedMediaAPI cma = (CannedMediaAPI) CannedMediaAPI.getInstance();
        ManagerManagerTest.updateManager(MediaAPIManager.class, CannedMediaAPI.class, true, cma);

        oldDVR = (DVRAPIManager) ManagerManager.getInstance(DVRAPIManager.class);
        CannedDVRAPI cda = (CannedDVRAPI) CannedDVRAPI.getInstance();
        ManagerManagerTest.updateManager(DVRAPIManager.class, cda.getClass(), true, cda);

        ors = new CannedOcapRecordedServiceExt();
        ors.setMediaTime(new Time(0L));
        rsml = new CannedRecordedServiceMediaLocator(ors);
        rds = new DataSource();
        rds.setService(ors);
        // CannedRecordingPlayer player = new CannedRecordingPlayer(null);
        // player.setSource(rds);
        // // MAS - commented out because it was removed for segmented recorded
        // services
        // rds.setPlayer(player);
    }

    public void tearDown() throws Exception
    {
        rds = null;
        ors = null;
        rsml = null;

        ManagerManagerTest.updateManager(MediaAPIManager.class, oldAPI.getClass(), true, oldAPI);
        ManagerManagerTest.updateManager(DVRAPIManager.class, oldDVR.getClass(), true, oldDVR);

        super.tearDown();
    }

    // Test section

    public void testGetContentType()
    {
        assertEquals("Returned content type is incorrect", "ocap.recording", rds.getContentType());
    }

    // public void testGetRecordedService()
    // {
    // assertNull("Returned Service should be null", rds.getRecordedService());
    // rds.setLocator(rsml);
    // assertSame("Returned Service does not match expected value",
    // ors, rds.getRecordedService());
    // }

    // public void testSetLocator()
    // {
    // rds.setLocator(rsml);
    // assertSame("Returned Service does not match expected value",
    // ors, rds.getRecordedService());
    // }

    public void testGetDuration() throws Exception
    {
        CannedRecordingRequest tempRR = (CannedRecordingRequest) ors.getRecordingRequest();

        // Put the RecordingRequest back in and we'll start with
        // recordingInProgress
        // returning false
        ors.cannedSetRecordingRequest(tempRR);
        tempRR.cannedSetState(LeafRecordingRequest.COMPLETED_STATE);
        assertEquals("Returned duration is incorrect", new Time(60000000000L).getNanoseconds(), rds.getDuration()
                .getNanoseconds());

        // Now for a recording in progress...
        tempRR.cannedSetState(LeafRecordingRequest.IN_PROGRESS_STATE);

        // Okay, now a ServiceContextRecordingSpec
        tempRR.cannedSetRecordingSpec(new ServiceContextRecordingSpec(null, null, 60000, null)); // The
                                                                                                 // only
                                                                                                 // thing
                                                                                                 // we
                                                                                                 // care
                                                                                                 // about
                                                                                                 // in
                                                                                                 // here
                                                                                                 // is
                                                                                                 // the
                                                                                                 // duration
        assertEquals("Returned duration is incorrect", new Time(60000000000L).getNanoseconds(), rds.getDuration()
                .getNanoseconds());

        // Next is a LocatorRecordingSpec
        tempRR.cannedSetRecordingSpec(new LocatorRecordingSpec(new Locator[] {}, null, 60000, null));
        assertEquals("Returned duration is incorrect", new Time(60000000000L).getNanoseconds(), rds.getDuration()
                .getNanoseconds());

        // Finally, we have the ServiceRecordingSpec
        tempRR.cannedSetRecordingSpec(new ServiceRecordingSpec(null, null, 60000, null));
        assertEquals("Returned duration is incorrect", new Time(60000000000L).getNanoseconds(), rds.getDuration()
                .getNanoseconds());
    }

    public void testRecordingInProgress()
    {
        // First we'll just call it with the service being null
        assertFalse("Returned value should be false with null Service", rds.recordingInProgress());

        // Now, set the MediaLocator and the recording state and try again
        rds.setLocator(rsml);
        ((CannedRecordingRequest) ors.getRecordingRequest()).cannedSetState(LeafRecordingRequest.IN_PROGRESS_STATE);
        assertTrue("Returned value should be true with recording in IN_PROGRESS_STATE", rds.recordingInProgress());

        // We'll set the state to another branch in the code for better coverage
        ((CannedRecordingRequest) ors.getRecordingRequest()).cannedSetState(LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE);
        assertTrue("Returned value should be true with recording in IN_PROGRESS_INSUFFICIENT_SPACE_STATE",
                rds.recordingInProgress());

        // Finally we'll put it in a non-IN_PROGRESS_STATE
        ((CannedRecordingRequest) ors.getRecordingRequest()).cannedSetState(LeafRecordingRequest.DELETED_STATE);
        assertFalse("Returned value should be true with recording in IN_PROGRESS_STATE", rds.recordingInProgress());
    }

}
