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

package javax.tv.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests TVTimerSpec.
 * 
 * @author Aaron Kamienski
 */
public class TVTimerSpecTest extends TestCase
{
    public void testConstructor()
    {
        assertEquals("Unexpected absolute value on construction", true, spec.isAbsolute());
        assertEquals("Unexpected repeat value on construction", false, spec.isRepeat());
        assertEquals("Unexpected regular value on construction", true, spec.isRegular());
        assertEquals("Unexpected time value on construction", 0L, spec.getTime());
    }

    public void testAbsolute()
    {
        spec.setAbsolute(false);
        assertEquals("Expected set value returned", false, spec.isAbsolute());

        spec.setAbsolute(true);
        assertEquals("Expected set value returned", true, spec.isAbsolute());

        spec.setAbsolute(false);
        assertEquals("Expected set value returned", false, spec.isAbsolute());
    }

    public void testRepeat()
    {
        spec.setRepeat(false);
        assertEquals("Expected set value returned", false, spec.isRepeat());

        spec.setRepeat(true);
        assertEquals("Expected set value returned", true, spec.isRepeat());

        spec.setRepeat(false);
        assertEquals("Expected set value returned", false, spec.isRepeat());
    }

    public void testRegular()
    {
        spec.setRegular(false);
        assertEquals("Expected set value returned", false, spec.isRegular());

        spec.setRegular(true);
        assertEquals("Expected set value returned", true, spec.isRegular());

        spec.setRegular(false);
        assertEquals("Expected set value returned", false, spec.isRegular());
    }

    public void testTime()
    {
        spec.setTime(12345L);
        assertEquals("Expected set value returned", 12345L, spec.getTime());

        spec.setTime(0L);
        assertEquals("Expected set value returned", 0L, spec.getTime());

        spec.setTime(6789L);
        assertEquals("Expected set value returned", 6789L, spec.getTime());
    }

    public void testSetAbsoluteTime()
    {
        long time[] = { 1234567890L, 0L, 1234L };
        for (int i = 0; i < time.length; ++i)
        {
            spec.setAbsoluteTime(time[i]);
            assertEquals("Expected set value returned", time[i], spec.getTime());
            assertEquals("Expected absolute to be set", true, spec.isAbsolute());
            assertEquals("Expected repeat to be clear", false, spec.isRepeat());
        }
    }

    public void testSetDelayTime()
    {
        long time[] = { 1234567890L, 0L, 1234L };
        for (int i = 0; i < time.length; ++i)
        {
            spec.setDelayTime(time[i]);
            assertEquals("Expected set value returned", time[i], spec.getTime());
            assertEquals("Expected absolute to be clear", false, spec.isAbsolute());
            assertEquals("Expected repeat to be clear", false, spec.isRepeat());
        }
    }

    public void testListeners()
    {
        class Listener implements TVTimerWentOffListener
        {
            int expected = 0;

            int events = 0;

            public void timerWentOff(TVTimerWentOffEvent evt)
            {
                ++events;
            }
        }
        Listener listeners[] = { new Listener(), new Listener(), new Listener(), null, null, null, null, new Listener() };
        listeners[3] = listeners[1];
        listeners[4] = listeners[0];
        listeners[5] = listeners[2];
        listeners[6] = listeners[1];

        // notify
        spec.notifyListeners(timer);
        for (int j = 0; j < listeners.length; ++j)
            assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                    listeners[j].events);

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            for (int j = 0; j <= i; ++j)
                listeners[j].events = 0;

            // Add listener
            listeners[i].expected++;
            spec.addTVTimerWentOffListener(listeners[i]);

            // notify
            spec.notifyListeners(timer);
            for (int j = 0; j <= i; ++j)
                assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                        listeners[j].events);
        }

        // Remove listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            for (int j = 0; j <= i; ++j)
                listeners[j].events = 0;

            // Remove listener
            listeners[i].expected--;
            spec.removeTVTimerWentOffListener(listeners[i]);

            // notify
            spec.notifyListeners(timer);
            for (int j = 0; j <= i; ++j)
                assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                        listeners[j].events);
        }

        for (int j = 0; j < listeners.length; ++j)
            listeners[j].events = 0;

        // notify
        spec.notifyListeners(timer);
        for (int j = 0; j < listeners.length; ++j)
            assertEquals("Expected listener to be called as many times as added", listeners[j].expected,
                    listeners[j].events);
    }

    public void testListenerThreadSafety()
    {
        class Listener implements TVTimerWentOffListener
        {
            public void timerWentOff(TVTimerWentOffEvent evt)
            {
                spec.removeTVTimerWentOffListener(this);
                spec.removeTVTimerWentOffListener(this);
                spec.removeTVTimerWentOffListener(this);
            }
        }
        Listener l = new Listener();

        spec.addTVTimerWentOffListener(l);
        spec.addTVTimerWentOffListener(l);
        spec.addTVTimerWentOffListener(l);

        // Just want to make sure nothing goes horribly wrong...
        // Can't really expect removal to affect whether listeners are called
        // during notification
        spec.notifyListeners(timer);
    }

    public void testNotifyListeners_exception()
    {
        class Listener implements TVTimerWentOffListener
        {
            int events = 0;

            public void timerWentOff(TVTimerWentOffEvent evt)
            {
                ++events;
            }
        }
        Listener listeners[] = { new Listener(), new Listener(), new Listener()
        {
            public void timerWentOff(TVTimerWentOffEvent evt)
            {
                super.timerWentOff(evt);
                // should be caught within notifyListeners
                throw new RuntimeException();
            }
        }, new Listener(), new Listener(), };

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
            spec.addTVTimerWentOffListener(listeners[i]);

        // notify
        try
        {
            spec.notifyListeners(timer);
        }
        catch (RuntimeException e)
        {
            fail("Expected exceptions to be caught in notifyListeners()");
        }

        // Check listener invocation
        for (int i = 0; i < listeners.length; ++i)
            assertEquals("Expected all listeners to be called - " + i, 1, listeners[i].events);

        // Now do the same testing for under-privileged caller
        // TODO: fix this test; it is incorrect as it can't account for
        // PrivilegedAction
        // May need to examine the stack.
        /*
         * ProxySecurityManager.install(); ProxySecurityManager.push(new
         * NullSecurityManager() { public void checkPermission(Permission p) {
         * if ( p instanceof AllPermission ) throw new
         * SecurityException("under-privileged"); } }); try { // notify
         * spec.notifyListeners(timer);
         * 
         * // Don't bother checking listener invocation now... assume it's okay
         * } finally { ProxySecurityManager.pop(); }
         */
    }

    private TVTimer timer;

    private TVTimerSpec spec;

    public void setUp() throws Exception
    {
        super.setUp();
        timer = TVTimer.getTimer();
        spec = new TVTimerSpec();
    }

    public void tearDown() throws Exception
    {
        timer = null;
        spec = null;
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
        TestSuite suite = new TestSuite(TVTimerSpecTest.class);
        return suite;
    }
}
