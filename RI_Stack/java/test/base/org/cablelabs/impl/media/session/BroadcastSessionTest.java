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
package org.cablelabs.impl.media.session;

import javax.media.Time;
import javax.tv.service.Service;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.media.CannedVideoDevice;
import org.cablelabs.impl.media.JMFTests;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.davic.net.Locator;
import org.dvb.spi.selection.SelectionSession;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * BroadcastSessionTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class BroadcastSessionTest extends TestCase
{

    private BroadcastSession session;

    private BroadcastSession sessionWithSS;

    private CannedSelectionSession ss;

    private CannedSessionListener listener;

    private CannedSIDatabase sidb;

    private CannedNetworkInterface cni;

    public BroadcastSessionTest()
    {
        super("BroadcastSessionTest");
    }

    public BroadcastSessionTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(BroadcastSessionTest.class);
        return suite;
    }

    ServiceDetailsExt details;

    ServiceComponentExt[] components;

    public void setUp() throws Exception
    {
        super.setUp();

        JMFTests.setUpJMF();

        Object sync = new Object();
        cni = (CannedNetworkInterface) JMFTests.nim.getNetworkInterfaces()[0];
        listener = new CannedSessionListener();
        sidb = JMFTests.getCannedSIDB();
        Service svc = sidb.jmfService1;
        components = new ServiceComponentExt[] { sidb.jmfServiceComponent1V, sidb.jmfServiceComponent1A1 };
        CannedVideoDevice vd = new CannedVideoDevice(1);

        details = getServiceDetails(svc);
        session = new BroadcastSession(sync, listener, details, vd, cni, (short)0, false, 0.0F, (byte) 0);

        ss = new CannedSelectionSession();
        sessionWithSS = new BroadcastSession(sync, listener, details, vd, cni, (short)0, false, 0.0F, (byte) 0);

        session.present(details, null);
    }

    private ServiceDetailsExt getServiceDetails(Service svc) throws Exception
    {
        return (ServiceDetailsExt) ((ServiceExt) svc).getDetails();
    }

    public void tearDown() throws Exception
    {
        session.stop(false);

        cni.cannedSetCurrentSelectionSession(null);
        cni = null;

        JMFTests.tearDownJMF();

        super.tearDown();
    }

    // Test section

    public void testSetVideoDevice()
    {
        // call to make sure it doesn't throw an exception
        CannedVideoDevice cvd = new CannedVideoDevice(2);
        session.setVideoDevice(cvd);
    }

    public void testStartAndStop()
    {
        // We're already started...
        // Call to make sure it doesn't throw an exception
        // This test doesn't have much use if the Session wasn't started
        session.stop(false);
    }

    public void testSetAndGetMediaTime() throws Exception
    {
        Time t = new Time(1000L);
        Time newT = session.setMediaTime(t);
        assertNull("Time should be null", newT);

        newT = session.getMediaTime();
        assertNull("Time should be null", newT);

        cni.cannedSetCurrentSelectionSession(ss);
        sessionWithSS.present(details, null);
        t = new Time(1000000000L);
        newT = sessionWithSS.setMediaTime(t);
        assertEquals("Time does not match", t.getNanoseconds(), newT.getNanoseconds());
        assertEquals("Millis is incorrect", 1000L, ss.pos);
        sessionWithSS.stop(false);
    }

    public void testSetAndGetRate() throws Exception
    {
        float newRate = session.setRate(3.0f);
        assertEquals("Rate does not match", 1.0f, newRate, 0.0001f);

        newRate = session.getRate();
        assertEquals("Rate does not match", 1.0f, newRate, 0.0001f);

        cni.cannedSetCurrentSelectionSession(ss);
        sessionWithSS.present(details, null);
        newRate = sessionWithSS.setRate(5.0f);
        assertEquals("Rate does not match", 5.0f, newRate, 0.0001f);
        assertEquals("Rate does not match", 5.0f, ss.rate, 0.0001f);
        sessionWithSS.stop(false);
    }

    public void testFreezeAndResume() throws Exception
    {
        session.freeze();

        session.resume();
    }

    // Test support section

    private class CannedSessionListener implements SessionListener
    {

        Session ssn;

        int code;

        public void handleSessionEvent(Session s, int ec, int data1, int data2)
        {
            this.ssn = s;
            this.code = ec;
        }

    }

    private class CannedSelectionSession implements SelectionSession
    {

        long pos;

        float rate;

        public void destroy()
        {

        }

        public Locator select()
        {
            return null;
        }

        public void selectionReady()
        {

        }

        public long setPosition(long position)
        {
            pos = position;
            return pos;
        }

        public float setRate(float newRate)
        {
            rate = newRate;
            return rate;
        }

    }

}
