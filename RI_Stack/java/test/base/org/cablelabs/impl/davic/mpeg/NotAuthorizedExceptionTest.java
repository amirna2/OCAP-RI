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

/*
 * Created on Aug 1, 2006
 */
package org.cablelabs.impl.davic.mpeg;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.net.CannedNetMgr;
import org.cablelabs.impl.manager.service.CannedServiceMgr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.mpeg.ElementaryStream;
import org.davic.mpeg.NotAuthorizedInterface;
import org.davic.mpeg.Service;
import org.davic.mpeg.TransportStream;
import org.davic.net.Locator;
import org.davic.net.tuning.StreamTable;

/**
 * Tests NotAuthorizedException.
 * 
 * @author Aaron Kamienski
 */
public class NotAuthorizedExceptionTest extends TestCase
{
    private void check(String msg, NotAuthorizedException e, int type, int[][] reasons, Service service,
            ElementaryStream[] streams, String toString)
    {
        assertNotNull(msg, e);

        assertEquals(msg + " unexpected type", type, e.getType());
        for (int i = 0; i < reasons.length; ++i)
        {
            int[] r = e.getReason(i);
            assertNotNull(msg + " Expected non-null reasons[" + i + "]", r);
            assertEquals("Internal Error: expected major/minor reason[" + i + "]", 2, reasons[i].length);
            assertEquals(msg + " Expected major/minor reason[" + i + "]", 2, r.length);
            assertEquals(msg + " Unexpected major reason[" + i + "]", reasons[i][0], r[0]);
            assertEquals(msg + " Unexpected minor reason[" + i + "]", reasons[i][1], r[1]);
        }
        try
        {
            e.getReason(reasons.length);
            fail("Expected IndexOutOfBoundsException for index=" + reasons.length);
        }
        catch (IndexOutOfBoundsException iooe)
        { /* expected */
        }
        assertEquals(msg + " unexpected service", service, e.getService());
        if (streams == null)
            assertNull(msg + " expected null elementaryStreams", e.getElementaryStreams());
        else
        {
            ElementaryStream[] elemStreams = e.getElementaryStreams();
            assertNotNull(msg + " expected non-null elementaryStreams", elemStreams);
            assertEquals(msg + " unexpected number of elementaryStreams", streams.length, elemStreams.length);
            for (int i = 0; i < streams.length; ++i)
                assertEquals(msg + " unexpected elementaryStream[" + i + "]", streams[i], elemStreams[i]);
        }
        if (toString == null)
            assertNull(msg + " expected null message", e.getMessage());
        else
        {
            String message = e.getMessage();
            assertNotNull(msg + " expected non-null message", message);

            // Expect the message to contain the given string
            assertTrue(msg + " expected \"" + message + "\" to contain \"" + toString + "\"",
                    message.indexOf(toString) >= 0);
        }
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.NotAuthorizedException(int,
     * int)'
     */
    public void testNotAuthorizedExceptionIntInt()
    {
        int[][] reasons = { { NotAuthorizedInterface.NOT_POSSIBLE, NotAuthorizedInterface.NO_ENTITLEMENT } };
        NotAuthorizedException e = new NotAuthorizedException(reasons[0][0], reasons[0][1]);
        check("NotAuthorizedException(II)", e, 0, reasons, null, null, null);
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.NotAuthorizedException(Service,
     * int[])'
     */
    public void testNotAuthorizedExceptionServiceIntArray()
    {
        Service service = svc;
        int[][] reasons = { { NotAuthorizedInterface.NOT_POSSIBLE, NotAuthorizedInterface.MATURITY_RATING } };
        NotAuthorizedException e = new NotAuthorizedException(service, reasons[0]);
        check("NotAuthorizedException(I[])", e, NotAuthorizedInterface.SERVICE, reasons, service, null,
                service.toString());

        // e.setElementaryStreams() should be ignored
        e.setElementaryStreams(new ElementaryStream[] { es[0] });
        check("setElementaryStreams() should be ignored", e, NotAuthorizedInterface.SERVICE, reasons, service, null,
                service.toString());
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.NotAuthorizedException(ElementaryStream[],
     * int[][])'
     */
    public void testNotAuthorizedExceptionElementaryStreamArrayIntArrayArray()
    {
        ElementaryStream[] streams = es;
        int[][] reasons = genReasons(es);
        NotAuthorizedException e = new NotAuthorizedException(es, reasons);
        check("NotAuthorizedException(ES[]I[][]", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null, es,
                es[0].toString());

        // e.setElementaryStreams() should be ignored
        e.setService(svc);
        check("setService() should be ignored", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null, es,
                es[0].toString());
    }

    private int[][] genReasons(ElementaryStream[] streams)
    {
        int[][] reasons = new int[streams.length][2];

        for (int i = 0; i < streams.length; ++i)
        {
            if ((i & 1) == 0)
            {
                reasons[i][0] = NotAuthorizedInterface.NOT_POSSIBLE;
                if ((i & 2) == 0)
                    reasons[i][1] = NotAuthorizedInterface.GEOGRAPHICAL_BLACKOUT;
                else
                    reasons[i][1] = NotAuthorizedInterface.NO_ENTITLEMENT;
            }
            else
            {
                reasons[i][0] = NotAuthorizedInterface.POSSIBLE_UNDER_CONDITIONS;
                if ((i & 2) == 0)
                    reasons[i][1] = NotAuthorizedInterface.COMMERCIAL_DIALOG;
                else
                    reasons[i][1] = NotAuthorizedInterface.MATURITY_RATING_DIALOG;
            }
        }
        return reasons;
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.setElementaryStreams(ElementaryStream[])'
     */
    public void testSetElementaryStreamsElementaryStreamArray()
    {
        ElementaryStream[] streams = { es[0] };
        int[][] reasons = { { NotAuthorizedInterface.NOT_POSSIBLE, NotAuthorizedInterface.OTHER } };
        NotAuthorizedException e = new NotAuthorizedException(reasons[0][0], reasons[0][1]);
        check("NotAuthorizedException(II)", e, 0, reasons, null, null, null);

        e.setElementaryStreams(streams);
        check("setElementaryStreams(es[])", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null, streams,
                streams[0].toString());

        // setService should be ignored
        e.setService(svc);
        check("setService() ignored", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null, streams,
                streams[0].toString());
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.setElementaryStreams(TransportStream,
     * int)'
     */
    public void testSetElementaryStreamsTransportStreamInt()
    {
        ElementaryStream[] streams = { es[es.length - 1] };
        int pid = streams[0].getPID();
        int[][] reasons = { { NotAuthorizedInterface.POSSIBLE_UNDER_CONDITIONS, NotAuthorizedInterface.OTHER } };
        NotAuthorizedException e = new NotAuthorizedException(reasons[0][0], reasons[0][1]);
        check("NotAuthorizedException(II)", e, 0, reasons, null, null, null);

        e.setElementaryStreams(ts, pid);
        check("setElementaryStreams(TransportStream,pid)", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null,
                streams, streams[0].toString());

        // setService should be ignored
        e.setService(svc);
        check("setService() ignored", e, NotAuthorizedInterface.ELEMENTARY_STREAM, reasons, null, streams,
                streams[0].toString());
    }

    /*
     * Test method for
     * 'org.cablelabs.impl.davic.mpeg.NotAuthorizedException.setService(Service)'
     */
    public void testSetService()
    {
        int[][] reasons = { { NotAuthorizedInterface.POSSIBLE_UNDER_CONDITIONS, NotAuthorizedInterface.OTHER } };
        NotAuthorizedException e = new NotAuthorizedException(reasons[0][0], reasons[0][1]);
        check("NotAuthorizedException(II)", e, 0, reasons, null, null, null);

        Service service = svc;
        e.setService(service);
        check("setService(Service)", e, NotAuthorizedInterface.SERVICE, reasons, service, null, service.toString());

        // setElementaryStreams should be ignored
        e.setElementaryStreams(new ElementaryStream[] { es[0] });
        check("setElementaryStreams() ignored", e, NotAuthorizedInterface.SERVICE, reasons, service, null,
                service.toString());
    }

    protected TransportStream ts;

    protected Service svc;

    protected ElementaryStream[] es;

    private NetManager oldNetMgr;

    private ServiceManager oldSvcMgr;

    private NetManager netMgr;

    private ServiceManager svcMgr;

    private void setUpSIDB() throws Exception
    {
        // Switch to using our canned manager(s)
        svcMgr = (ServiceManager) CannedServiceMgr.getInstance();
        oldSvcMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, svcMgr);
        netMgr = (NetManager) CannedNetMgr.getInstance();
        oldNetMgr = (NetManager) ManagerManager.getInstance(NetManager.class);
        ManagerManagerTest.updateManager(NetManager.class, CannedNetMgr.class, false, netMgr);
    }

    private void tearDownSIDB() throws Exception
    {
        // Replace the canned manager(s) originally setup by the stack
        svcMgr.destroy();
        svcMgr = null;
        netMgr.destroy();
        netMgr = null;

        ManagerManagerTest.updateManager(NetManager.class, oldNetMgr != null ? oldNetMgr.getClass() : null, false,
                oldNetMgr);
        ManagerManagerTest.updateManager(ServiceManager.class, oldSvcMgr != null ? oldSvcMgr.getClass() : null, false,
                oldSvcMgr);
        ts = null;
        svc = null;
        es = null;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        setUpSIDB();

        // Get a transport stream with multiple services and multiple pids
        Locator[] locs = StreamTable.listTransportStreams();
        boolean done = false;

        // Block until SI is available, if necessary
        final long blockTime = 30000;
        if (locs == null || locs.length == 0)
        {
            long stopTime = System.currentTimeMillis() + blockTime;
            do
            {
                Thread.sleep(500);
                locs = StreamTable.listTransportStreams();
            }
            while ((locs == null || locs.length == 0) && System.currentTimeMillis() < stopTime);
        }

        ALL: for (int i = 0; i < locs.length; ++i)
        {
            TransportStream[] tsArray = StreamTable.getTransportStreams(locs[i]);
            if (tsArray == null) continue;
            for (int j = 0; j < tsArray.length; ++j)
            {
                Service[] svcArray = tsArray[j].retrieveServices();
                if (svcArray == null) continue;

                for (int k = 0; k < svcArray.length; ++k)
                {
                    ElementaryStream[] esArray = svcArray[k].retrieveElementaryStreams();
                    if (esArray != null && esArray.length > 1)
                    {
                        ts = tsArray[j];
                        svc = svcArray[k];
                        es = esArray;
                        done = true;
                        break ALL;
                    }
                }
            }
        }
        assertTrue("Internal error - could not find testable elementary streams", done);
    }

    protected void tearDown() throws Exception
    {
        tearDownSIDB();
        super.tearDown();
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
        TestSuite suite = new TestSuite(NotAuthorizedExceptionTest.class);
        return suite;
    }

}
