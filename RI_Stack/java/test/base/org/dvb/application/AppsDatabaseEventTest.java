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
 * Tests AppsDatabaseEvent.
 * 
 * @author Aaron Kamienski
 */
public class AppsDatabaseEventTest extends TestCase
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
        TestSuite suite = new TestSuite(AppsDatabaseEventTest.class);
        return suite;
    }

    public AppsDatabaseEventTest(String name)
    {
        super(name);
    }

    /**
     * Tests getSource().
     */
    public void testGetSource() throws Exception
    {
        Object source[] = { new Object(), new Object(), new Object(), new Object(), };
        AppsDatabaseEvent e[] = new AppsDatabaseEvent[source.length];

        for (int i = 0; i < source.length; ++i)
        {
            e[i] = new AppsDatabaseEvent(AppsDatabaseEvent.APP_DELETED, new AppID(1, 2), source[i]);
        }
        for (int i = 0; i < source.length; ++i)
        {
            assertSame("The Source set on construction should be returned", source[i], e[i].getSource());
        }
    }

    /**
     * Tests getAppID().
     */
    public void testGetAppID() throws Exception
    {
        AppID ids[] = { new AppID(100, 100), new AppID(100, 200), new AppID(200, 100), };
        AppsDatabaseEvent e[] = new AppsDatabaseEvent[ids.length];

        for (int i = 0; i < ids.length; ++i)
        {
            e[i] = new AppsDatabaseEvent(AppsDatabaseEvent.NEW_DATABASE, ids[i], new Object());
        }
        for (int i = 0; i < ids.length; ++i)
        {
            assertSame("The AppID set on construction should be returned", ids[i], e[i].getAppID());
        }
    }

    /**
     * Tests getEventId().
     */
    public void testGetEventId() throws Exception
    {
        int events[] = { AppsDatabaseEvent.NEW_DATABASE, AppsDatabaseEvent.APP_CHANGED, AppsDatabaseEvent.APP_ADDED,
                AppsDatabaseEvent.APP_DELETED, };
        AppsDatabaseEvent e[] = new AppsDatabaseEvent[events.length];

        for (int i = 0; i < events.length; ++i)
        {
            e[i] = new AppsDatabaseEvent(events[i], new AppID(100, 2), new Object());
        }
        for (int i = 0; i < events.length; ++i)
        {
            assertEquals("The EventID set on construction should be returned", events[i], e[i].getEventId());
        }
    }
}
