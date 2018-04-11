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
package org.cablelabs.impl.manager.recording;

import org.ocap.dvr.BufferingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.recording.db.SerializationMgrTest;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceMgrImpl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BufferingRequestManagerTest extends TestCase
{
    public static ServiceManager oldSM;

    public static CannedServiceMgr csm;

    protected void setUp() throws Exception
    {
        super.setUp();
        oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        ManagerManagerTest.updateManager(ServiceManager.class, ServiceMgrImpl.class, true, oldSM);
        if (csm != null)
        {
            csm.destroy();
        }
    }

    public void testCreateBufferingRequest()
    {
        BufferingRequestManager mgr = new BufferingRequestManager();
        long minDuration = 100l;
        long maxDuration = 200l;
        ExtendedFileAccessPermissions efap = null;
        CannedSIDatabase sidb = (CannedSIDatabase) csm.getSIDatabase();
        BufferingRequest req = mgr.createBufferingRequest(sidb.jmfService1, minDuration, maxDuration, efap);

        assertTrue(req.getMaxDuration() == maxDuration);
        assertTrue(req.getMinimumDuration() == minDuration);
        assertTrue(req.getService().equals(sidb.jmfService1));
    }

    public void testCreateBufferingRequestMinGreaterThanMaxDuration()
    {
        BufferingRequestManager mgr = new BufferingRequestManager();
        long minDuration = 100l;
        long maxDuration = 50l;
        ExtendedFileAccessPermissions efap = null;
        CannedSIDatabase sidb = (CannedSIDatabase) csm.getSIDatabase();
        try
        {
            BufferingRequest req = mgr.createBufferingRequest(sidb.jmfService1, minDuration, maxDuration, efap);
            fail("Expected an exception");
        }
        catch (Exception e)
        {
            // expected outcome
        }

    }

    public void testGetBufferingRequestsEmpty()
    {
        BufferingRequestManager mgr = new BufferingRequestManager();
        BufferingRequest[] reqArr = mgr.getBufferingRequests();
        assertTrue(reqArr != null);
        assertTrue(reqArr.length == 0);
    }

    public void testAddBufferingRequest()
    {
        BufferingRequestManager mgr = new BufferingRequestManager();
        BufferingRequest req = createBufferingRequest(mgr);
        mgr.addActiveBufferingRequest(req);
        BufferingRequest[] reqArr = mgr.getBufferingRequests();
        assertTrue(reqArr.length == 1);
        assertTrue(reqArr[0].equals(req));
    }

    public void testAddMultipleBufferingRequests()
    {
        BufferingRequestManager mgr = new BufferingRequestManager();
        BufferingRequest req1 = createBufferingRequest(mgr);
        BufferingRequest req2 = createBufferingRequest(mgr);
        mgr.addActiveBufferingRequest(req1);
        mgr.addActiveBufferingRequest(req2);
        BufferingRequest[] reqArr = mgr.getBufferingRequests();

        boolean req1Found = false, req2Found = false;
        for (int i = 0; i < reqArr.length; i++)
        {
            if (req1.equals(reqArr[i]))
            {
                req1Found = true;
            }
            else if (req2.equals(reqArr[i]))
            {
                req2Found = true;
            }
        }
        assertTrue(req1Found);
        assertTrue(req2Found);
    }

    private BufferingRequest createBufferingRequest(BufferingRequestManager mgr)
    {
        long minDuration = 100l;
        long maxDuration = 200l;
        ExtendedFileAccessPermissions efap = null;
        CannedSIDatabase sidb = (CannedSIDatabase) csm.getSIDatabase();
        BufferingRequest req = mgr.createBufferingRequest(sidb.jmfService1, minDuration, maxDuration, efap);
        return req;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BufferingRequestManagerTest.class);
        return suite;
    }

}
