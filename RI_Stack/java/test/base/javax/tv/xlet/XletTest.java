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

package javax.tv.xlet;

import junit.framework.*;
import java.lang.reflect.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests the XletStateChangeException class.
 */
public class XletTest extends InterfaceTestCase
{
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(XletTest.class);
        suite.setName(Xlet.class.getName());
        return suite;
    }

    public XletTest(String name, ImplFactory f)
    {
        super(name, Class.class, f);
    }

    protected Xlet createXlet() throws Exception
    {
        return (Xlet) getXletClass().newInstance();
    }

    protected Class getXletClass() throws Exception
    {
        return (Class) createImplObject();
    }

    protected Xlet xlet;

    protected Context ctx;

    private class Context implements XletContext
    {
        public boolean destroyed;

        public boolean paused;

        public boolean resume;

        public void notifyDestroyed()
        {
            destroyed = true;
        }

        public void notifyPaused()
        {
            paused = true;
        }

        public Object getXletProperty(String key)
        {
            return null;
        }

        public void resumeRequest()
        {
            resume = true;
        }
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        xlet = createXlet();
        ctx = new Context();
    }

    protected void tearDown() throws Exception
    {
        xlet.destroyXlet(true);
        xlet = null;
        ctx = null;
        super.tearDown();
    }

    public void testConstructor() throws Exception
    {
        Class cl = xlet.getClass();
        Constructor c = cl.getConstructor(new Class[0]);
        assertNotNull("Should have a default constructor", c);
        assertTrue("Default constructor should be public", 0 != (c.getModifiers() & Modifier.PUBLIC));

        // Constructor should do no real work. How do we test that?
    }

    public void testInitXlet() throws Exception
    {
        xlet.initXlet(ctx);
    }

    public void testStartXlet() throws Exception
    {
        xlet.initXlet(ctx);
        xlet.startXlet();
    }

    public void testPauseXlet() throws Exception
    {
        xlet.initXlet(ctx);
        xlet.startXlet();
        xlet.pauseXlet();
    }

    /**
     * Tests startXlet resumes a paused xlet.
     */
    public void testResumeXlet() throws Exception
    {
        xlet.initXlet(ctx);
        xlet.startXlet();
        xlet.pauseXlet();
        xlet.startXlet();
    }

    /**
     * Tests destroyXlet unconditionally.
     */
    public void testDestroyXletUncond() throws Exception
    {
        xlet.initXlet(ctx);
        xlet.startXlet();

        xlet.destroyXlet(true);
    }

    /**
     * Tests destroyXlet. Verifies that all Threads started by the xlet are
     * stopped.
     */
    public void testDestroyXlet() throws Exception
    {
        ThreadGroup tg = new ThreadGroup("XletTest");
        Thread t = new Thread(tg, new Runnable()
        {
            public void run()
            {
                try
                {
                    xlet.initXlet(ctx);
                }
                catch (Exception e)
                {
                }
            }
        });
        t.start();
        t.join();

        t = new Thread(tg, new Runnable()
        {
            public void run()
            {
                try
                {
                    xlet.destroyXlet(true);
                }
                catch (Exception e)
                {
                }
            }
        });
        t.start();
        t.join();

        assertEquals("No threads should exist for the Xlet's ThreadGroup", 0, tg.activeCount());
        assertEquals("No threadGroupss should exist for the Xlet's ThreadGroup", 0, tg.activeGroupCount());
        tg.destroy();
    }

    public static Test suite()
    {
        InterfaceTestSuite suite = XletTest.isuite();
        suite.addFactory(new ImplFactory()
        {
            public Object createImplObject()
            {
                return org.cablelabs.impl.manager.application.XletAppTest.DummyJTVXlet.class;
            }
        });
        return suite;
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

}
