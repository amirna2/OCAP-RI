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
package org.cablelabs.impl.ocap.si;

import java.util.Date;

import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedServiceMgr;
import org.cablelabs.impl.manager.service.ServiceMgrImpl;
import org.cablelabs.impl.service.SICache;

public class TableChangeManagerTest extends InterfaceTestCase
{
    TableChangeManagerFactory factory;

    TableChangeManager tableChangeManager;

    CannedSIDatabase sidb;

    public static InterfaceTestSuite isuite(TableChangeManagerFactory factory)
    {
        return new InterfaceTestSuite(TableChangeManagerTest.class, factory);
    }

    public TableChangeManagerTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
        this.factory = (TableChangeManagerFactory) factory;
    }

    public TableChangeManagerTest(String name, ImplFactory factory)
    {
        this(name, TableChangeManager.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        factory.setUp();
        tableChangeManager = (TableChangeManager) createImplObject();
        sidb = factory.getCannedSIDatabase();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        factory.tearDown();
    }

    public void testRemoveListenerNotAdded()
    {
        CannedTableChangeListener l = new CannedTableChangeListener();
        tableChangeManager.removeChangeListener(l);
    }

    public void testAddNotifyListener()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(-1, -1, -1, listener);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    public void testAddNotifyListenerWithPriority()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListenerWithPriority(-1, -1, -1, listener, 1);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    public void testMultipleListenerWithPriority()
    {
        CannedTableChangeListener listener1 = new CannedTableChangeListener();
        tableChangeManager.addChangeListenerWithPriority(-1, -1, -1, listener1, 1);
        CannedTableChangeListener listener2 = new CannedTableChangeListener();
        tableChangeManager.addChangeListenerWithPriority(-1, -1, -1, listener2, 2);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener1.waitForEvent();
        listener2.waitForEvent();
        assertTrue(listener1.lastEvent != null);
        assertTrue(listener2.lastEvent != null);
        assertTrue(listener1.notifiedDate.getTime() >= listener2.notifiedDate.getTime());
    }

    public void testAddTwiceReceiveNotificationTwice()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(-1, -1, -1, listener);
        tableChangeManager.addChangeListener(-1, -1, -1, listener);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    public void testAddPriorityRemoveNotNotified()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListenerWithPriority(-1, -1, -1, listener, 1);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener.waitForEvent();
        listener.reset();

        tableChangeManager.removeChangeListener(listener);
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);
        listener.waitForEvent();
        assertTrue(listener.lastEvent == null);
    }

    public void testAddRemoveNotNotified()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(-1, -1, -1, listener);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);

        listener.waitForEvent();
        listener.reset();

        tableChangeManager.removeChangeListener(listener);
        tableChangeManager.notifyListeners(tableChangeManager.new Service(-1, -1, -1), evt);
        listener.waitForEvent();
        assertTrue(listener.lastEvent == null);
    }

    public void testAddNotifyListenerSpecifyService()
    {
        int sourceID = sidb.jmfLocator1.getSourceID();
        int frequency = sidb.jmfLocator1.getFrequency();
        int programNumber = sidb.jmfLocator1.getProgramNumber();

        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(sourceID, frequency, programNumber, listener);

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent1A1)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(sourceID, frequency, programNumber), evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    public void testAddNotifyListenerSpecifyDifferentService()
    {
        int sourceID = sidb.jmfLocator1.getSourceID();
        int frequency = sidb.jmfLocator1.getFrequency();
        int programNumber = sidb.jmfLocator1.getProgramNumber();

        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(sourceID, frequency, programNumber, listener);

        int sourceID2 = sidb.jmfLocator2.getSourceID();
        int frequency2 = sidb.jmfLocator2.getFrequency();
        int programNumber2 = sidb.jmfLocator2.getProgramNumber();

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, sidb.jmfServiceComponent2A2)
        {
        };
        tableChangeManager.notifyListeners(tableChangeManager.new Service(sourceID2, frequency2, programNumber2), evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent == null);
    }

    static abstract class TableChangeManagerFactory implements ImplFactory
    {
        private ServiceManager oldSM;

        private CannedServiceMgr csm;

        public CannedSIDatabase getCannedSIDatabase()
        {
            return (CannedSIDatabase) csm.getSIDatabase();
        }

        public SICache getSICache()
        {
            return (SICache) csm.getSICache();
        }

        public void setUp()
        {
            oldSM = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            csm = (CannedServiceMgr) CannedServiceMgr.getInstance();
            ManagerManagerTest.updateManager(ServiceManager.class, CannedServiceMgr.class, true, csm);

        }

        public void tearDown()
        {
            ManagerManagerTest.updateManager(ServiceManager.class, ServiceMgrImpl.class, true, oldSM);
            if (csm != null)
            {
                csm.destroy();
            }
            csm = null;
            oldSM = null;
        }

        public abstract Object createImplObject() throws Exception;
    }

    static class CannedTableChangeListener implements TableChangeListener
    {
        SIChangeEvent lastEvent;

        Object waitObject = new Object();

        Date notifiedDate = null;

        public void reset()
        {
            lastEvent = null;
            notifiedDate = null;
        }

        public void waitForEvent()
        {
            synchronized (waitObject)
            {
                try
                {
                    if (lastEvent == null)
                    {
                        waitObject.wait(3000);
                    }
                }
                catch (InterruptedException exc)
                {

                }
            }
        }

        public void notifyChange(SIChangeEvent event)
        {
            synchronized (waitObject)
            {
                lastEvent = event;
                notifiedDate = new Date();
                waitObject.notifyAll();
            }
        }

    }
}
