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
package org.cablelabs.impl.media.player;

import javax.media.IncompatibleSourceException;
import javax.media.NotRealizedError;
import javax.media.Time;
import javax.media.protocol.DataSource;

import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.davic.media.MediaLocator;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.media.source.CannedDataSource;
import org.cablelabs.impl.media.source.CannedOcapServiceDataSource;

public class BroadcastServiceMediaHandlerTest extends CannedBaseMediaPlayerTest
{
    BroadcastServiceMediaHandler mediaHandler;

    CannedControllerListener listener;

    public BroadcastServiceMediaHandlerTest(String name)
    {
        super(name);
    }

    public BroadcastServiceMediaHandlerTest()
    {
        super();
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(BroadcastServiceMediaHandlerTest.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        mediaHandler = new BroadcastServiceMediaHandler(cc, new Object(), new ResourceUsageImpl(cc));
        listener = new CannedControllerListener(1);
        mediaHandler.addControllerListener(listener);
    }

    public void tearDown() throws Exception
    {
        mediaHandler.close();
        mediaHandler.removeControllerListener(listener);
        mediaHandler = null;
        listener = null;
        super.tearDown();
    }

    public void testGetLock()
    {
        Object syncObject = mediaHandler.getLock();
        assertTrue(syncObject != null);
    }

    public void testGetIsComponentPlayer()
    {
        boolean value = mediaHandler.isComponentPlayer();
        assertTrue(!value);
    }

    public void testSetGetSource() throws Exception
    {
        DataSource ds = new CannedOcapServiceDataSource();
        ds.setLocator(new MediaLocator(new OcapLocator(0x1)));

        //
        // first, just set the source and get it back
        //
        mediaHandler.setSource(ds);
        DataSource returnedDs = mediaHandler.getSource();
        assertTrue(ds.equals(returnedDs));

        //
        // next, try to send in a non
        // 
        DataSource errords = new CannedDataSource();
        try
        {
            mediaHandler.setSource(errords);
            fail("Setting a incompatible data source succeeded.");
        }
        catch (IncompatibleSourceException exc)
        {
            // expected outcome, should not have changed the source
            assertTrue(ds.equals(mediaHandler.getSource()));

        }

    }

    public void testDoSetMediaTimeBeforeRealized()
    {
        try
        {
            mediaHandler.setMediaTime(new Time(0l));
            fail("setMediaTime call succeeded in an unrealized state");
        }
        catch (NotRealizedError err)
        {
            // this is the expected behavior
        }
    }

}
