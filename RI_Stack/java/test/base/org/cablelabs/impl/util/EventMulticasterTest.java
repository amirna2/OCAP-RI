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

package org.cablelabs.impl.util;

import junit.framework.*;
import org.cablelabs.test.*;
import java.util.*;
import org.havi.ui.*;
import org.dvb.application.*;
import org.dvb.event.*;

/**
 * Tests EventMulticaster.
 * 
 * @author Aaron Kamienski
 */
public class EventMulticasterTest extends HEventMulticasterTest
{
    private static final Class[] classes = { org.dvb.application.AppStateChangeEventListener.class,
            org.dvb.application.AppsDatabaseEventListener.class, org.dvb.event.UserEventListener.class, };

    /**
     * Tests for correct ancestry. Calls super to test for HEventMulticaster
     * ancestry.
     */
    public void testAncestry()
    {
        super.testAncestry();

        TestUtils.testExtends(EventMulticaster.class, HEventMulticaster.class);
        for (int i = 0; i < classes.length; ++i)
            TestUtils.testImplements(EventMulticaster.class, classes[i]);
    }

    /**
     * There are no public constructors. There is one protected constructor:
     * <ul>
     * <li>EventMulticaster(EventListener a, EventListener b)
     * </ul>
     */
    public void testConstructors()
    {
        super.testConstructors();

        TestUtils.testNoPublicConstructors(EventMulticaster.class);

        EventListener a = new EventListener()
        {
        };
        EventListener b = new EventListener()
        {
        };
        checkConstructor("EventMulticaster(EventListener, EventListener)", new EventMulticaster(a, b), a, b);
    }

    protected Adapter createAdapter()
    {
        return new Adapter2();
    }

    protected Class testedClass()
    {
        return EventMulticaster.class;
    }

    /**
     * Calls the appropriate method on the multicaster.
     */
    protected void doit(EventListener el, int what)
    {
        switch (what)
        {
            default:
                super.doit(el, what);
            case STATECHANGE:
                ((AppStateChangeEventListener) el).stateChange(null);
                break;
            case USEREVENTRECEIVED:
                ((UserEventListener) el).userEventReceived(null);
                break;
            case DBNEW:
                ((AppsDatabaseEventListener) el).newDatabase(null);
                break;
            case DBADDED:
                ((AppsDatabaseEventListener) el).entryAdded(null);
                break;
            case DBREMOVED:
                ((AppsDatabaseEventListener) el).entryRemoved(null);
                break;
            case DBCHANGED:
                ((AppsDatabaseEventListener) el).entryChanged(null);
                break;
        }
    }

    public static final int STATECHANGE = 100;

    public static final int USEREVENTRECEIVED = 101;

    public static final int DBNEW = 102, DBADDED = 103, DBREMOVED = 104, DBCHANGED = 105;

    public static TestSuite suite()
    {
        TestSuite suite = HEventMulticasterTest.suite();

        suite.addTest(new EventMulticasterTest("stateChange", STATECHANGE, AppStateChangeEventListener.class));
        suite.addTest(new EventMulticasterTest("userEventReceived", USEREVENTRECEIVED, UserEventListener.class));
        suite.addTest(new EventMulticasterTest("newDatabase", DBNEW, AppsDatabaseEventListener.class));
        suite.addTest(new EventMulticasterTest("entryAdded", DBADDED, AppsDatabaseEventListener.class));
        suite.addTest(new EventMulticasterTest("entryRemoved", DBREMOVED, AppsDatabaseEventListener.class));
        suite.addTest(new EventMulticasterTest("entryChanged", DBCHANGED, AppsDatabaseEventListener.class));

        return suite;
    }

    /**
     * Adapter class used in tests.
     */
    protected static class Adapter2 extends Adapter implements AppStateChangeEventListener, AppsDatabaseEventListener,
            UserEventListener
    {
        public Adapter2()
        {
            super();
            called = new int[106];
        }

        public void stateChange(AppStateChangeEvent evt)
        {
            ++called[STATECHANGE];
        }

        public void userEventReceived(UserEvent e)
        {
            ++called[USEREVENTRECEIVED];
        }

        public void newDatabase(AppsDatabaseEvent evt)
        {
            ++called[DBNEW];
        }

        public void entryAdded(AppsDatabaseEvent evt)
        {
            ++called[DBADDED];
        }

        public void entryRemoved(AppsDatabaseEvent evt)
        {
            ++called[DBREMOVED];
        }

        public void entryChanged(AppsDatabaseEvent evt)
        {
            ++called[DBCHANGED];
        }
    }

    /**
     * Standard constructor.
     */
    public EventMulticasterTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public EventMulticasterTest(String s, int index, Class listenerClass)
    {
        super(s);
        lookup = index;
        this.listenerClass = listenerClass;
    }

    /**
     * Standalone runner. This one is never called. Subclasses should duplicate
     * this one EXACTLY.
     */
    public static void main(String args[])
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
