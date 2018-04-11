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

import javax.tv.locator.Locator;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.transport.TransportStream;

import org.ocap.si.PATProgram;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.ocap.si.TableChangeManagerTest.CannedTableChangeListener;
import org.cablelabs.impl.ocap.si.TableChangeManagerTest.TableChangeManagerFactory;
import org.cablelabs.impl.service.ProgramAssociationTableHandle;

public class PATTableChangeManagerTest extends TestCase
{
    Factory factory;

    PATTableChangeManager tableChangeManager;

    CannedSIDatabase sidb;

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(PATTableChangeManagerTest.class);
        suite.addTest(TableChangeManagerTest.isuite(new Factory()));
        return suite;
    }

    public PATTableChangeManagerTest()
    {
        super();
    }

    public PATTableChangeManagerTest(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        factory = new Factory();
        super.setUp();
        factory.setUp();
        tableChangeManager = (PATTableChangeManager) factory.createImplObject();
        sidb = factory.getCannedSIDatabase();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        factory.tearDown();
    }

    public void testAddNotifyListenerNegativeFrequency()
    {
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(-1, -1, -1, listener);
        CannedProgramAssociationTableExt siElement = new CannedProgramAssociationTableExt();
        siElement.frequency = -1;

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, siElement)
        {
        };
        tableChangeManager.notifyChange(evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    public void testAddNotifyListenerSourceID()
    {
        int sourceID = sidb.jmfLocator1.getSourceID();
        CannedTableChangeListener listener = new CannedTableChangeListener();
        tableChangeManager.addChangeListener(sourceID, -1, -1, listener);
        CannedProgramAssociationTableExt siElement = new CannedProgramAssociationTableExt();
        siElement.sourceIDs = new int[] { sourceID };

        SIChangeEvent evt = new SIChangeEvent(this, SIChangeType.ADD, siElement)
        {
        };
        tableChangeManager.notifyChange(evt);

        listener.waitForEvent();
        assertTrue(listener.lastEvent != null);
    }

    private static class Factory extends TableChangeManagerFactory
    {

        public Object createImplObject() throws Exception
        {
            return new PATTableChangeManager(getSICache());
        }

    }

    private static class CannedProgramAssociationTableExt implements ProgramAssociationTableExt
    {
        int frequency;

        int[] sourceIDs = new int[0];

        PATProgram[] programs = new PATProgram[0];

        public int getFrequency()
        {
            return frequency;
        }

        public ProgramAssociationTableHandle getPATHandle()
        {
            return null;
        }

        public int[] getSourceIDs()
        {
            return sourceIDs;
        }

        public TransportStream getTransportStream()
        {
            return null;
        }

        public void setLocator(Locator locator)
        {
        }

        public PATProgram[] getPrograms()
        {
            return programs;
        }

        public short getTableId()
        {
            return 0;
        }

        public Locator getLocator()
        {
            return null;
        }

        public ServiceInformationType getServiceInformationType()
        {
            return null;
        }

        public Date getUpdateTime()
        {
            return null;
        }

        public int getTransportStreamId()
        {
            // TODO Auto-generated method stub
            return 0;
        }

    }
}
