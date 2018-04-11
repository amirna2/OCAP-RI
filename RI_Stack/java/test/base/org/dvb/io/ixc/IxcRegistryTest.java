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

package org.dvb.io.ixc;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.tv.xlet.XletContext;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IxcRegistryTest extends TestCase
{

    public void test_bind() throws Exception
    {
        XletContext xc = new TestXletContext("1", "1001");
        Remote obj = new RemoteObj();
        try
        {
            try
            {
                IxcRegistry.bind(xc, "test", obj);
            }
            catch (AlreadyBoundException e)
            {
                fail("Didn't expect AlreadyBoundException to be thrown!");
            }

            // try to bind the same object again using the same name
            try
            {
                IxcRegistry.bind(xc, "test", obj);
                fail("Expected AlreadyBoundException to be thrown");
            }
            catch (AlreadyBoundException e)
            {
                // pass
            }

            // try to bind the same object again using a different name
            try
            {
                IxcRegistry.bind(xc, "test2", obj);
            }
            catch (AlreadyBoundException e)
            {
                fail("Did not expect AlreadyBoundException to be thrown");
            }
        }
        finally
        {
            try
            {
                IxcRegistry.unbind(xc, "test");
            }
            catch (Exception e)
            {
                // do nothing
            }

            try
            {
                IxcRegistry.unbind(xc, "test2");
            }
            catch (Exception e)
            {
                // do nothing
            }
        }
    }

    public void test_unbind() throws Exception
    {
        XletContext xc = new TestXletContext("1", "1001");
        try
        {
            // try to unbind something we know isn't bound
            try
            {
                IxcRegistry.unbind(xc, "notbound");
                fail("Expected NotBoundException to be thrown");
            }
            catch (NotBoundException e)
            {
                // pass
            }

            Remote obj = new RemoteObj();
            IxcRegistry.bind(xc, "bind", obj);

            // should be able to unbind bound object
            try
            {
                IxcRegistry.unbind(xc, "bind");
            }
            catch (NotBoundException e)
            {
                fail("Did not expect NotBoundException to be thrown!");
            }
        }
        finally
        {
            try
            {
                IxcRegistry.unbind(xc, "bind");
            }
            catch (NotBoundException e)
            {
                // do nothing
            }
        }
    }

    public void test_rebind() throws Exception
    {
        XletContext xc = new TestXletContext("1", "1001");
        Remote obj = new RemoteObj();
        Remote obj2 = new RemoteObj();
        try
        {
            try
            {
                IxcRegistry.bind(xc, "test", obj);
            }
            catch (AlreadyBoundException e)
            {
                fail("Didn't expect exception to be thrown!" + e);
            }

            // try to bind the same object again using a different object
            IxcRegistry.rebind(xc, "test", obj2);
            // lookup the object and verify the second object is returned
            Remote found = IxcRegistry.lookup(xc, "/1/1001/test");

            assertNotNull("Expected to get an object back from lookup", found);
            // assertEquals("Expected to get the object used with rebind()",
            // obj2, found);
        }
        finally
        {
            try
            {
                IxcRegistry.unbind(xc, "test");
            }
            catch (Exception e)
            {
                // do nothing
            }
        }
    }

    public void test_list() throws Exception
    {
        XletContext xc = new TestXletContext("1", "1001");
        Remote obj = new RemoteObj();
        Remote obj2 = new RemoteObj();

        try
        {
            // verify that no objects are available
            String[] list = IxcRegistry.list(xc);
            assertEquals("Expected no objects to be available", 0, list.length);

            String prefix = "/1/1001/";
            String[] names = new String[] { prefix + "object", prefix + "object2" };
            IxcRegistry.bind(xc, names[0].substring(prefix.length()), obj);
            IxcRegistry.bind(xc, names[1].substring(prefix.length()), obj2);

            String array[] = IxcRegistry.list(xc);
            assertEquals("Expected 2 objects to be available", 2, array.length);
            for (int i = 0; i < names.length; i++)
            {
                boolean found = false;
                for (int j = 0; j < array.length; j++)
                {
                    if (names[i].equals(array[j]))
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    fail(names[i] + " was not found in the returned list");
                }
            }
        }
        finally
        {
            try
            {
                IxcRegistry.unbind(xc, "object");
            }
            catch (NotBoundException e)
            {
                // do nothing
            }

            try
            {
                IxcRegistry.unbind(xc, "object2");
            }
            catch (NotBoundException e)
            {
                // do nothing
            }
        }
    }

    public void test_lookup() throws Exception
    {
        XletContext xc = new TestXletContext("1", "1001");
        Remote remObj = new RemoteObj();

        // verify that an IllegalArgumentException is thrown if the path is not
        // formatted correctly
        try
        {
            IxcRegistry.lookup(xc, "/");
            fail("Expected IllegalArgumentException to be thrown for illegal path");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        try
        {
            IxcRegistry.lookup(xc, "/1/");
            fail("Expected IllegalArgumentException to be thrown for illegal path");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        try
        {
            IxcRegistry.lookup(xc, "1/2/name/");
            fail("Expected IllegalArgumentException to be thrown for illegal path");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        try
        {
            IxcRegistry.lookup(xc, "1/2//name");
            fail("Expected IllegalArgumentException to be thrown for illegal path");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        try
        {
            IxcRegistry.lookup(xc, "/1//name/");
            fail("Expected IllegalArgumentException to be thrown for illegal path");
        }
        catch (IllegalArgumentException e)
        {
            // pass
        }

        // verify NotBoundException is thrown if the path is not bound
        try
        {
            IxcRegistry.lookup(xc, "/1/2/name");
            fail("Expected NotBoundException to be thrown for unbound path");
        }
        catch (NotBoundException e)
        {
            // pass
        }

        try
        {
            // bind an object
            IxcRegistry.bind(xc, "name", remObj);
            // verify that we can lookup an object even if the path is not in
            // simplified form
            Remote found = IxcRegistry.lookup(xc, "/01/1001/name");

            assertNotNull("Expected to get an object back", found);

            /**
             * Test failure conditions
             * 
             * verify RemoteExcpetion is thrown if a stub cannot be generated
             */

            String key = "/1/1001/name";
            // each method must declare java.rmi.RemoteException in its throws
            // clause
            IxcRegistry.rebind(xc, "name", new RemoteClass()
            {
                public void method1(int[] array)
                {
                }
            });

            try
            {
                IxcRegistry.lookup(xc, key);
                fail("Expected RemoteException to be thrown!");
            }
            catch (RemoteException e)
            {
                // pass
            }

            // remote object passed by remote reference as an argument or return
            // value must be
            // declared as an interface that extends java.rmi.Remote, and not as
            // an application class that
            // implements this remote interface.
            IxcRegistry.rebind(xc, "name", new RemoteClass4()
            {
                public void method(RemoteObj arg)
                {
                }
            });

            try
            {
                IxcRegistry.lookup(xc, key);
                fail("Expected RemoteException to be thrown!");
            }
            catch (RemoteException e)
            {
                // pass
            }

            // type of each method argument must either be a remote interface, a
            // class or interface
            // that implements java.io.Serializable, or a primitive type.
            IxcRegistry.rebind(xc, "name", new RemoteClass3()
            {
                public void method(Object arg)
                {

                }
            });

            try
            {
                IxcRegistry.lookup(xc, key);
                fail("Expected RemoteException to be thrown!");
            }
            catch (RemoteException e)
            {
                // pass
            }

            // Each return value must either be a remote interface, a class or
            // interface that implements
            // java.io.Serializable, a primitive type, or void. */
            IxcRegistry.rebind(xc, "name", new RemoteClass2()
            {
                public Object method()
                {
                    return new Object();
                }
            });

            try
            {
                IxcRegistry.lookup(xc, key);
                fail("Expected RemoteException to be thrown!");
            }
            catch (RemoteException e)
            {
                // pass
            }

            /**
             * Test success conditions
             */

            remObj = null;
            // method declared to correctly throw RemoteException
            IxcRegistry.rebind(xc, "name", new RemoteClass10()
            {
                public void method1(int[] array)
                {
                }
            });

            try
            {
                remObj = IxcRegistry.lookup(xc, key);
            }
            catch (RemoteException e)
            {
                fail("Did not expect RemoteException to be thrown!");
            }
            assertNotNull("Expected to get a reference back", remObj);

            remObj = null;
            // method returns a Remote, Serializable, or primitive
            IxcRegistry.rebind(xc, "name", new RemoteClass11()
            {
                public Integer method()
                {
                    return new Integer(1);
                }
            });

            try
            {
                remObj = IxcRegistry.lookup(xc, key);
            }
            catch (RemoteException e)
            {
                fail("Did not expect RemoteException to be thrown!");
            }
            assertNotNull("Expected to get a reference back", remObj);

            remObj = null;
            // method with an argument that is Remote, serializable, or
            // primitive
            IxcRegistry.rebind(xc, "name", new RemoteClass12()
            {
                public void method(Integer arg)
                {
                }
            });

            try
            {
                remObj = IxcRegistry.lookup(xc, key);
            }
            catch (RemoteException e)
            {
                fail("Did not expect RemoteException to be thrown!");
            }
            assertNotNull("Expected to get a reference back", remObj);

            remObj = null;
            // method with an argument that is a Remote interface
            IxcRegistry.rebind(xc, "name", new RemoteClass13()
            {
                public void method(RemoteClass3 arg)
                {
                }
            });

            try
            {
                remObj = IxcRegistry.lookup(xc, key);
            }
            catch (RemoteException e)
            {
                fail("Did not expect RemoteException to be thrown!");
            }
            assertNotNull("Expected to get a reference back", remObj);

        }
        finally
        {
            try
            {
                IxcRegistry.unbind(xc, "name");
            }
            catch (NotBoundException e)
            {
                // do nothing
            }
        }
    }

    class RemoteObj implements Remote
    {

    }

    // class with a method that does not throw a RemoteException
    interface RemoteClass extends Remote
    {
        void method1(int[] array);
    }

    // class with a method that does not return a Remote, Serializable, or
    // primitive
    interface RemoteClass2 extends Remote
    {
        Object method() throws RemoteException;
    }

    // class with a method that has an argument that is not a Remote interface,
    // Serializable, or primitive
    interface RemoteClass3 extends Remote
    {
        void method(Object arg) throws RemoteException;
    }

    // class with an arguement that is not declared to be a remote interface,
    // but an application class
    interface RemoteClass4 extends Remote
    {
        void method(RemoteObj arg) throws RemoteException;
    }

    // method correctly throws RemoteException
    interface RemoteClass10 extends Remote
    {
        void method1(int[] array) throws RemoteException;
    }

    // class with a method that returns a Remote, Serializable, or primitive
    interface RemoteClass11 extends Remote
    {
        Integer method() throws RemoteException;
    }

    // class with a method that has an argument that is a Remote interface,
    // Serializable, or primitive
    interface RemoteClass12 extends Remote
    {
        void method(Integer arg) throws RemoteException;
    }

    // class with an arguement that is a remote interface
    interface RemoteClass13 extends Remote
    {
        void method(RemoteClass3 arg) throws RemoteException;
    }

    public class TestXletContext implements XletContext
    {

        String orgId;

        String appId;

        public TestXletContext(String orgId, String appId)
        {
            this.orgId = orgId;
            this.appId = appId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.xlet.XletContext#getXletProperty(java.lang.String)
         */
        public Object getXletProperty(String key)
        {
            if ("dvb.org.id".equals(key))
            {
                return orgId;
            }
            else if ("dvb.app.id".equals(key))
            {
                return appId;
            }

            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.xlet.XletContext#notifyDestroyed()
         */
        public void notifyDestroyed()
        {
            // Auto-generated method stub
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.xlet.XletContext#notifyPaused()
         */
        public void notifyPaused()
        {
            // Auto-generated method stub
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.tv.xlet.XletContext#resumeRequest()
         */
        public void resumeRequest()
        {
            // Auto-generated method stub
        }
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(IxcRegistryTest.class);
        return suite;
    }

    static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(IxcRegistryTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new IxcRegistryTest(tests[i]));
            return suite;
        }
    }

    public IxcRegistryTest(String name)
    {
        super(name);
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
