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

package org.cablelabs.impl.manager.ixc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * Implements the Invocation handler interface for the IXC implementation
 */
public class ProxyHandler implements InvocationHandler
{
    public ProxyHandler(Remote real, CallerContext cc)
    {
        this.real = real;
        this.cc = cc;
        // register a CallbackData object to clear the Remote object when the
        // CallerContext cc is destroyed
        cc.addCallbackData(new Callback(), this); // Axiom cl 52026
    }

    /**
     * Processes the method invocation and returns the appropriate return value.
     * Called by a <code>Proxy</code> instance.
     * 
     * @param proxy
     *            the Proxy instance the method is called on
     * @param method
     *            the Method instance representing the called method
     * @param arguments
     *            array of Objects containing the values of the arguments passed
     *            in the method invocation
     * 
     * @return Object instance containing the return value of the method.
     * @throws Throwable
     * 
     * @see java.lang.reflect.InvocationHandler
     */
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable
    {
        final Object[] val = { null };
        Object[] values = { null };
        final Throwable[] exception = { null };
        final Method m;
        final Object[] finalArgs;
        Object[] args = null;
        Class[] methodArgClasses = null;
        Class serverClass;

        // check if the exporting application is still around
        if (null == real)
        {
            throw new RemoteException("exporting application destroyed.");
        }

        // get the classloader used by the server object
        ClassLoader serverClassLoader = real.getClass().getClassLoader();

        // need to check the parameters to make sure everything is correct.
        // serializable objects should be serialized
        // remote objects need to have a proxy generated.
        if (null != arguments)
        {
            args = convertObjects(arguments, serverClassLoader, cc);

            // Convert each non-primitive method argument class to a
            // server-side class
            Class[] argsClasses = method.getParameterTypes();
            methodArgClasses = new Class[argsClasses.length];
            for (int i = 0; i < argsClasses.length; ++i)
            {
                Class clazz = argsClasses[i];
                if (clazz.isPrimitive() || (clazz.isArray() && clazz.getComponentType().isPrimitive()))
                {
                    methodArgClasses[i] = argsClasses[i];
                }
                else
                {
                    try
                    {
                        if (clazz.isArray())
                        {
                            Class arrayClass = clazz;
                            Class componentClass;
                            // Search for the component type of this array
                            // class. We loop here
                            // because this may be a multi-dimensional array.
                            while (true)
                            {
                                componentClass = arrayClass.getComponentType();
                                if (!componentClass.isArray()) break;

                                arrayClass = componentClass;
                            }

                            // Load the component type class and then place the
                            // loaded argument
                            // class in our array of method arguments
                            serverClassLoader.loadClass(componentClass.getName());
                            methodArgClasses[i] = Class.forName(argsClasses[i].getName(), true, serverClassLoader);
                        }
                        else
                        {
                            methodArgClasses[i] = serverClassLoader.loadClass(clazz.getName());
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new RemoteException("Class not found -- " + clazz.getName());
                    }
                }
            }
        }

        // get the class defining this method
        Class methodClass = method.getDeclaringClass();
        // load server definition of method class
        try
        {
            serverClass = serverClassLoader.loadClass(methodClass.getName());
        }
        catch (ClassNotFoundException e)
        {
            throw new RemoteException("Class not found");
        }

        try
        {
            m = serverClass.getMethod(method.getName(), methodArgClasses);
        }
        catch (NoSuchMethodException e)
        {
            throw new RemoteException("target method not found");
        }

        finalArgs = args;
        // invoke the method in the correct context
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                try
                {
                    val[0] = m.invoke(real, finalArgs);
                }
                catch (InvocationTargetException e)
                {
                    exception[0] = e.getTargetException();
                }
                catch (Throwable e)
                {
                    exception[0] = e;
                }
            }
        });

        // check for exceptions
        if (null != exception[0])
        {
            try
            {
                byte[] array;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);

                out.writeObject(exception[0]);
                out.close();

                array = bos.toByteArray();
                ByteArrayInputStream bis = new ByteArrayInputStream(array);
                IxcObjectInputStream in = new IxcObjectInputStream(bis, proxy.getClass().getClassLoader());

                exception[0] = (Throwable) in.readObject();
            }
            catch (IOException e)
            {
                throw new RemoteException("exception serialization failed");
            }

            throw exception[0];
        }
        // return type should be correct for the client
        if (null != val[0])
        {
            values = convertObjects(val, proxy.getClass().getClassLoader(), ccm.getCurrentContext());
        }

        return values[0];
    }

    /**
     * Returns a reference to the <code>Remote</code> object instance associated
     * with this <code>ProxyHandler</code>.
     * 
     * @return Remote instance associated with this <code>ProxyHandler</code>.
     */
    public Remote getRemoteObject()
    {
        return real;
    }

    /**
     * Returns a reference to the <code>CallerContext</code> instance assoicated
     * with this <code>ProxyHandler</code>.
     * 
     * @return <code>CallerContext</code> instance associated with this
     *         <code>ProxyHandler</code>.
     */
    public CallerContext getContext()
    {
        return cc;
    }

    /**
     * Copies or generates proxies for objects using the specified classloader.
     * 
     * @param objects
     *            array of object that need to be copied or have proxies
     *            generated
     * @param cl
     *            ClassLoader instance to use in generating proxies or copying
     *            objects
     * @return array of objects
     * 
     * @throws RemoteException
     */
    private Object[] convertObjects(Object[] objects, ClassLoader cl, CallerContext targetContext)
            throws RemoteException
    {
        // do some checking of the parameters?
        Object[] objs = new Object[objects.length];
        for (int i = 0; i < objects.length; i++)
        {
            // all primitive objects are wrapped in the appropriate wrapper
            // class so
            // no checks for primitive types are made. Those classes are
            // serializable.
            if (objects[i] == null)
            {
                objs[i] = null;
            }
            else if (objects[i] instanceof Remote)
            {
                // generate a proxy for this remote argument
                Class[] classes;
                ProxyHandler handler;
                Class[] interfaces;

                if (Proxy.isProxyClass(objects[i].getClass()))
                {
                    Remote object;
                    CallerContext ctx;
                    // TODO: could we just get the invocation handler and create
                    // a new proxy
                    // with that?
                    InvocationHandler ih = Proxy.getInvocationHandler(objects[i]);
                    if (ih instanceof ProxyHandler)
                    {
                        ProxyHandler ph = (ProxyHandler) ih;
                        object = ph.getRemoteObject();
                        ctx = ph.getContext();

                        // is this application the one that originated the
                        // Remote object
                        if (ctx == targetContext)
                        {
                            // yes, then the original object should be used
                            objs[i] = object;
                            continue;
                        }

                        // create a new proxy handler for this object
                        handler = new ProxyHandler(object, ctx);
                        interfaces = IxcManagerImpl.getRemoteClasses(object.getClass());
                    }
                    else
                    {
                        throw new RemoteException("Proxy is the incorrect type");
                    }
                }
                else
                {
                    // create a new proxy handler for this object
                    handler = new ProxyHandler((Remote) objects[i], ccm.getCurrentContext());
                    interfaces = IxcManagerImpl.getRemoteClasses(objects[i].getClass());
                }

                classes = new Class[interfaces.length];
                for (int j = 0; j < interfaces.length; j++)
                {
                    try
                    {
                        classes[j] = cl.loadClass(interfaces[j].getName());
                    }
                    catch (ClassNotFoundException e)
                    {
                        throw new RemoteException("argument class " + j + " not found");
                    }
                }
                objs[i] = (Remote) Proxy.newProxyInstance(cl, classes, handler);
            }
            else if (objects[i] instanceof Serializable)
            {
                // do some serialization
                try
                {
                    byte[] array;
                    Object obj = objects[i];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bos);

                    out.writeObject(obj);
                    out.close();

                    array = bos.toByteArray();
                    ByteArrayInputStream bis = new ByteArrayInputStream(array);
                    IxcObjectInputStream in = new IxcObjectInputStream(bis, cl);

                    objs[i] = in.readObject();
                }
                catch (IOException e)
                {
                    throw new RemoteException("error serializing argument" + i);
                }
                catch (ClassNotFoundException e)
                {
                    throw new RemoteException("class of argument " + i + " not found");
                }
            }
            else
            {
                throw new RemoteException("argument " + i + " is an incorrect type");
            }
        }
        return objs;
    }

    /**
     * clears the reference to the Remote object when the exporting context is
     * destroyed.
     */
    private class Callback implements CallbackData
    {
        public void destroy(CallerContext cc)
        {
            real = null;
        }

        public void active(CallerContext cc)
        {
        }

        public void pause(CallerContext cc)
        {
        }

    }

    /**
     * Subclass of ObjectInputStream that resolves classes using the ClassLoader
     * object passed in to the constructor.
     */
    private class IxcObjectInputStream extends ObjectInputStream
    {
        IxcObjectInputStream(InputStream ois, ClassLoader classLoader) throws IOException
        {
            super(ois);
            this.classLoader = classLoader;
        }

        protected Class resolveClass(ObjectStreamClass os) throws IOException, ClassNotFoundException
        {
            return Class.forName(os.getName(), false, classLoader);
        }

        /**
         * usually the server object classloader
         */
        private ClassLoader classLoader;
    }

    /**
     * reference to the exported object
     */
    private Remote real;

    /**
     * exported object CallerContext
     */
    private CallerContext cc;

    /**
     * reference to the CallerContextManager
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

}
