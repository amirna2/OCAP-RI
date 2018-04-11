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

package org.cablelabs.impl.manager.application;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests ExecQueue.
 * 
 * @author Aaron Kamienski
 */
public class ExecQueueTest extends TestCase
{
    /**
     * Tests post().
     */
    public void testPost()
    {
        execqueue.post(null); // non-disposing dispose...
    }

    /**
     * Tests getNext().
     */
    public void testGetNext() throws Exception
    {
        Runnable[] runs = new Runnable[10];
        for (int i = 0; i < runs.length; ++i)
        {
            runs[i] = new Runnable()
            {
                public void run()
                {
                }
            };
            execqueue.post(runs[i]);
        }

        for (int i = 0; i < runs.length; ++i)
        {
            Runnable run = execqueue.getNext();
            assertNotNull("getNext() should not return null", run);
            assertSame("getNext() should return FIFO order", runs[i], run);
        }
    }

    /**
     * Tests getNext().
     */
    public void testGetNext_blocking()
    {
        execqueue.dispose();

        final boolean[] called = { false };
        execqueue = new ExecQueue()
        {
            protected synchronized boolean waitOnQueue()
            {
                called[0] = true;
                return true;
            }
        };

        Runnable run = new Runnable()
        {
            public void run()
            {
            }
        };
        execqueue.post(run);
        Runnable run1 = execqueue.getNext();
        assertNotNull("Expected non-null returned first", run1);
        assertSame("getNext() should return posted runnable", run, run1);
        assertFalse("Did not expect any blocking to occur if non-empty", called[0]);

        Runnable run2 = execqueue.getNext();
        assertSame("Expected getNext() to return null", null, run2);
        assertTrue("Expected waitOnQueue() to be called", called[0]);
    }

    /**
     * Tests dispose().
     */
    public void testDispose()
    {
        // Post a runnable
        Runnable run = new Runnable()
        {
            public void run()
            {
            }
        };
        execqueue.post(run);

        // Dispose, the runnable should be ignored
        execqueue.dispose();

        // try to pull off the runnable, but get null instead
        Runnable run1 = execqueue.getNext();
        assertSame("Expected getNext() to return null after disposal", null, run1);
    }

    protected ExecQueue createExecQueue()
    {
        return new ExecQueue();
    }

    protected ExecQueue execqueue;

    protected void setUp() throws Exception
    {
        super.setUp();
        execqueue = createExecQueue();
    }

    protected void tearDown() throws Exception
    {
        if (execqueue != null) execqueue.dispose();
        execqueue = null;
        super.tearDown();
    }

    public ExecQueueTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ExecQueueTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ExecQueueTest(tests[i]));
            return suite;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        return new TestSuite(ExecQueueTest.class);
    }
}
