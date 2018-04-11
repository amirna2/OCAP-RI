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

package org.cablelabs.impl.manager.timer;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.JavaVersion;
import org.cablelabs.impl.util.TaskQueue;
import org.cablelabs.test.TestUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import javax.tv.util.TVTimer;

import junit.framework.TestCase;

/**
 * Abstract base class for testing TimerMgr(s).
 * 
 * @author Aaron Kamienski
 */
public abstract class TimerMgrTest extends TestCase
{
    /**
     * Ensure no public constructors (only for TimerMgr base class).
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(TimerMgr.class);
    }

    /**
     * Ensure that getInstance() is implemented to return TimerMgr
     * implementation.
     */
    public void testGetInstance()
    {
        TimerMgr mgr = null;
        try
        {
            mgr = (TimerMgr) TimerMgr.getInstance();
            assertNotNull("getInstance should return non-null", mgr);

            // The class name is dependent upon JavaVersion information
            String expectedName = null;
            if (JavaVersion.PJAVA_12)
                expectedName = "org.cablelabs.impl.manager.timer.TimerMgrPJava";
            else if (JavaVersion.PBP_10)
                expectedName = "org.cablelabs.impl.manager.timer.TimerMgrJava2";
            else if (JavaVersion.JAVA_2)
                expectedName = "org.cablelabs.impl.manager.timer.TimerMgrJava2";
            else
                fail("Don't know what should be returned");
            assertEquals("Expected implementation to be returned based on JavaVersion", expectedName, mgr.getClass()
                    .getName());
        }
        finally
        {
            if (mgr != null) mgr.destroy();
        }
    }

    /**
     * Tests getTimer(). Ensure that getTimer(CallerContext) is called with the
     * correct context.
     */
    public abstract void testGetTimer();

    /**
     * Tests getTimer(CallerContext).
     */
    public void testGetTimerWithContext()
    {
        // Check for null
        try
        {
            timermgr.getTimer(null);
            fail("Expected NullPointerException for null context");
        }
        catch (NullPointerException e)
        {
        }

        // Try with a range of CallerContexts
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext contexts[] = { ccm.getSystemContext(), ccm.getCurrentContext(), new CallerContext()
        {
            private Hashtable callbackData = new Hashtable();

            public void addCallbackData(CallbackData data, Object key)
            {
                callbackData.put(key, data);
            }

            public void removeCallbackData(Object key)
            {
                callbackData.remove(key);
            }

            public CallbackData getCallbackData(Object key)
            {
                return (CallbackData) callbackData.get(key);
            }

            public void runInContext(Runnable run) throws SecurityException, IllegalStateException
            {
                throw new RuntimeException("Should not be used");
            }

            public void runInContextSync(Runnable run) throws SecurityException, IllegalStateException,
                    InvocationTargetException
            {
                throw new RuntimeException("Should not be used");
            }

            public void runInContextAsync(Runnable run)
            {
                throw new RuntimeException("Should not be used");
            }

            public boolean isAlive()
            {
                return true;
            }

            public void checkAlive()
            {
            }

            public boolean isActive()
            {
                return true;
            }

            public Object get(Object key)
            {
                throw new UnsupportedOperationException();
            }

            public TaskQueue createTaskQueue()
            {
                throw new UnsupportedOperationException();
            }

            void dispose() throws Exception
            {
            }

            public void runInContextAWT(Runnable run) throws SecurityException,
                    IllegalStateException
            {
            }
        }, };
        TVTimer timers[] = new TVTimer[contexts.length * 2];

        // Initially get timers
        for (int i = 0; i < contexts.length; ++i)
        {
            timers[i] = timermgr.getTimer(contexts[i]);
            assertNotNull("Expected getTimer(CallerContext) to return a timer", timers[i]);
        }
        // Check for duplicates
        for (int i = 0; i < contexts.length; ++i)
        {
            for (int j = i; j < contexts.length; ++j)
            {
                if (contexts[i] == contexts[j])
                    assertSame("Expected same timer to be returned for a callerContext", timers[i], timers[j]);
                else
                    assertNotSame("Expected different timers to be returned for " + "different callerContexts",
                            timers[i], timers[j]);
            }
        }
        // Verify no duplicates for second run
        // Check for expected duplicates
        for (int i = 0; i < contexts.length; ++i)
        {
            int j = i + contexts.length;
            timers[j] = timermgr.getTimer(contexts[i]);
            assertNotNull("Expected getTimer(CallerContext) to return a timer", timers[j]);
            assertSame("Expected same timer to be returned for a callerContext", timers[i], timers[j]);
        }
    }

    /**
     * Tests createTimer().
     */
    public abstract void testCreateTimer();

    /**
     * Tests disposeTimer().
     */
    public abstract void testDisposeTimer();

    protected abstract TimerMgr createTimerMgr();

    protected TimerMgr timermgr;

    public void setUp() throws Exception
    {
        super.setUp();
        timermgr = createTimerMgr();
    }

    public void tearDown() throws Exception
    {
        if (timermgr != null) timermgr.destroy();
        timermgr = null;
        super.tearDown();
    }

    public TimerMgrTest(String name)
    {
        super(name);
    }

    /*
     * public static void main(String[] args) { try {
     * junit.textui.TestRunner.run(suite(args)); } catch(Exception e) {
     * e.printStackTrace(); } System.exit(0); }
     * 
     * public static Test suite(String[] tests) { if (tests == null ||
     * tests.length == 0) return suite(); else { TestSuite suite = new
     * TestSuite(TimerMgrTest.class.getName()); for(int i = 0; i < tests.length;
     * ++i) suite.addTest(new TimerMgrTest(tests[i])); return suite; } }
     * 
     * public static Test suite() { TestSuite suite = new
     * TestSuite(TimerMgrTest.class); return suite; }
     */
}
