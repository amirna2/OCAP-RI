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
 * Created on Nov 16, 2006
 */
package org.cablelabs.impl.manager;

import org.cablelabs.impl.manager.AppDownloadManager.Callback;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadRequest;
import org.cablelabs.impl.manager.AppDownloadManager.DownloadedApp;
import org.cablelabs.impl.manager.AppStorageManagerTest.ToDelete;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.util.Enumeration;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests the {@link AppDownloadManager} interface.
 * 
 * @author Aaron Kamienski
 */
public class AppDownloadManagerTest extends ManagerTest
{

    /**
     * Tests {@link AppDownloadManager#download(AppEntry, boolean, Callback)}.
     */
    public void testDownload_NoRemoteOC()
    {
        // For now simply test things that cannot be "downloaded"
        final XAppEntry entry = new XAppEntry();
        entry.id = new AppID(0xcafebabe, 0x6456);
        entry.baseDirectory = "/";
        entry.controlCode = OcapAppAttributes.AUTOSTART;
        //entry.serviceId = 0x22222;
        entry.transportProtocols = new TransportProtocol[] { new IcTransportProtocol()
        {
            {
                protocol = 0x0101;
                label = 1;
                urls.add("http://www.cablelabs.org/");
            }
        }, new LocalTransportProtocol()
        {
            {
                protocol = 0xFFFF;
                label = 0xFF;
            }
        }, new IcTransportProtocol()
        {
            {
                protocol = 0x0101;
                label = 2;
                urls.add("http://www.nosuchurl.com/where/");
            }
        }, };

        DownloadRequest req = adm.download(entry, false, true, new DummyCallback());
        assertNull("Expected request to not be submitted w/ no OC", req);

        // with no remote OC
        entry.transportProtocols[2] = new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = entry.transportProtocols[2].label;
                componentTag = 0x10;
            }
        };
        req = adm.download(entry, false, true, new DummyCallback());
        assertNull("Expected request to not be submitted w/ non-remote OC", req);
    }

    /**
     * Tests {@link AppDownloadManager#download(AppEntry, boolean, Callback)}.
     */
    public void testDownload_NonMSO()
    {
        // For now simply test things that cannot be "downloaded"
        XAppEntry entry = new XAppEntry();
        entry.id = new AppID(0xcafebabe, 0x6456);
        entry.baseDirectory = "/";
        entry.controlCode = OcapAppAttributes.AUTOSTART;
        //entry.serviceId = 0x2000;
        entry.transportProtocols = new TransportProtocol[] { new OcTransportProtocol()
        {
            {
                protocol = 0x0001;
                label = 1;
                remoteConnection = true;
                serviceId = 0x400;
                componentTag = 0x10;
            }
        }, };

        DownloadRequest req = adm.download(entry, false, true, new DummyCallback());
        assertNull("Expected request to not be submitted w/out MSO serviceId", req);
    }

    public static class DummyCallback implements Callback
    {
        public boolean disposeImmediately;

        public int failure;

        public int success;

        public int reason;

        public String msg;

        public DownloadedApp app;

        public DummyCallback()
        {
            this(true);
        }

        public DummyCallback(boolean dispose)
        {
            disposeImmediately = dispose;
        }

        public void reset()
        {
            reset(disposeImmediately);
        }

        public void reset(boolean dispose)
        {
            disposeImmediately = dispose;
            failure = 0;
            success = 0;
            reason = 0;
            msg = null;
        }

        public synchronized void downloadFailure(int reason, String msg)
        {
            ++failure;
            this.reason = reason;
            this.msg = msg;
            notifyAll();
        }

        public synchronized void downloadSuccess(DownloadedApp app)
        {
            ++success;
            this.app = app;

            if (disposeImmediately) app.dispose();

            notifyAll();
        }

    }

    /* ====================== boilerplate =================== */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(AppDownloadManagerTest.class);
        suite.setName(AppDownloadManager.class.getName());
        return suite;
    }

    public static InterfaceTestSuite isuite(String[] tests)
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(AppDownloadManagerTest.class, tests);
        suite.setName(AppDownloadManager.class.getName());
        return suite;
    }

    public AppDownloadManagerTest(String name, ImplFactory f)
    {
        super(name, AppDownloadManager.class, f);
    }

    protected AppDownloadManager createAppDownloadManager()
    {
        return (AppDownloadManager) createManager();
    }

    private AppDownloadManager adm;

    private Vector toDelete;

    protected void setUp() throws Exception
    {
        // System.out.println(getName());

        super.setUp();
        adm = (AppDownloadManager) mgr;
        toDelete = new Vector();
    }

    protected void tearDown() throws Exception
    {
        for (Enumeration e = toDelete.elements(); e.hasMoreElements();)
            ((ToDelete) e.nextElement()).delete();
        adm = null;
        super.tearDown();
    }
}
