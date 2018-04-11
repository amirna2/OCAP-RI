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

package org.dvb.application;

import junit.framework.*;

/**
 * Tests AppStateChangeEvent.
 * 
 * @author Aaron Kamienski
 */
public class AppStateChangeEventTest extends TestCase
{
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
        TestSuite suite = new TestSuite(AppStateChangeEventTest.class);
        return suite;
    }

    public AppStateChangeEventTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testGetSource() throws Exception
    {
        AppID id1 = new AppID(1000, 2000);
        AppID id2 = new AppID(2000, 1000);
        AppProxy app1 = new EmptyProxy();
        AppProxy app2 = new EmptyProxy();
        AppStateChangeEvent e1 = new AppStateChangeEvent(new AppID(1, 2), AppProxy.STARTED, AppProxy.STARTED, app1,
                true);
        AppStateChangeEvent e2 = new AppStateChangeEvent(new AppID(1, 2), AppProxy.STARTED, AppProxy.STARTED, app2,
                true);

        assertSame("The appID should be the one the AppStateChangEvent was created with", e1.getSource(), app1);
        assertSame("The appID should be the one the AppStateChangEvent was created with", e2.getSource(), app2);
    }

    /**
     * Tests getAppID().
     */
    public void testGetAppID() throws Exception
    {
        AppID id1 = new AppID(1000, 2000);
        AppID id2 = new AppID(2000, 1000);
        AppStateChangeEvent e1 = new AppStateChangeEvent(id1, AppProxy.STARTED, AppProxy.STARTED, new EmptyProxy(),
                true);
        AppStateChangeEvent e2 = new AppStateChangeEvent(id2, AppProxy.STARTED, AppProxy.STARTED, new EmptyProxy(),
                true);

        assertEquals("The appID should be the one the AppStateChangEvent was created with", e1.getAppID(), id1);
        assertEquals("The appID should be the one the AppStateChangEvent was created with", e2.getAppID(), id2);
    }

    private static final int[] states = { DVBJProxy.NOT_LOADED, DVBJProxy.LOADED, DVBJProxy.PAUSED, DVBJProxy.STARTED,
            DVBJProxy.DESTROYED, DVBHTMLProxy.LOADING, DVBHTMLProxy.KILLED, };

    /**
     * Tests getFromState().
     */
    public void testGetFromState() throws Exception
    {
        AppID id = new AppID(1, 2);
        AppProxy app = new EmptyProxy();
        for (int i = 0; i < states.length; ++i)
        {
            AppStateChangeEvent e1 = new AppStateChangeEvent(id, states[i], AppProxy.NOT_LOADED, app, false);

            assertEquals("The fromState should be the one the AppStateChangeEvent was created with", states[i],
                    e1.getFromState());
        }
    }

    /**
     * Tests getToState().
     */
    public void testGetToState() throws Exception
    {
        AppID id = new AppID(1, 2);
        AppProxy app = new EmptyProxy();
        for (int i = 0; i < states.length; ++i)
        {
            AppStateChangeEvent e1 = new AppStateChangeEvent(id, AppProxy.NOT_LOADED, states[i], app, false);

            assertEquals("The toState should be the one the AppStateChangeEvent was created with", states[i],
                    e1.getToState());
        }
    }

    /**
     * Tests hasFailed().
     */
    public void testHasFailed() throws Exception
    {
        AppProxy app = new EmptyProxy();
        AppID id = new AppID(2397978, 323);
        for (int i = 0; i < 2; ++i)
        {
            AppStateChangeEvent e1 = new AppStateChangeEvent(id, AppProxy.NOT_LOADED, AppProxy.STARTED, app, i == 0);
            assertEquals("The failed state should be the one the appStateChangeEvent was created with", i == 0,
                    e1.hasFailed());
        }
    }

    static class EmptyProxy implements DVBJProxy
    {
        public int getState()
        {
            return STARTED;
        }

        public void load()
        {
        }

        public void init()
        {
        }

        public void start()
        {
        }

        public void start(String[] str)
        {
        }

        public void pause()
        {
        }

        public void resume()
        {
        }

        public void stop(boolean kill)
        {
        }

        public void addAppStateChangeEventListener(AppStateChangeEventListener l)
        {
        }

        public void removeAppStateChangeEventListener(AppStateChangeEventListener l)
        {
        }
    }
}
