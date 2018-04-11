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

package org.cablelabs.test;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Vector;

public class ProxySecurityManager extends SecurityManager
{
    private Vector stack = new Vector();

    public static void install() throws SecurityException
    {
        SecurityManager sm;
        if ((sm = System.getSecurityManager()) == null || !(sm instanceof ProxySecurityManager))
        {
            System.setSecurityManager(new ProxySecurityManager());
        }
    }

    public static void push(SecurityManager toPush)
    {
        SecurityManager sm = System.getSecurityManager();
        ((ProxySecurityManager) sm).pushSecurityManager(toPush);
    }

    public static void pop()
    {
        SecurityManager sm = System.getSecurityManager();
        ((ProxySecurityManager) sm).popSecurityManager();
    }

    public void pushSecurityManager(SecurityManager sm)
    {
        stack.insertElementAt(sm, 0);
    }

    public void popSecurityManager()
    {
        stack.removeElementAt(0);
    }

    private SecurityManager sm()
    {
        try
        {
            return (SecurityManager) stack.elementAt(0);
        }
        catch (Exception e)
        {
            return NULL;
        }
    }

    public void checkCreateClassLoader() throws SecurityException
    {
        sm().checkCreateClassLoader();
    }

    public void checkAccess(Thread g) throws SecurityException
    {
        sm().checkAccess(g);
    }

    public void checkAccess(ThreadGroup g) throws SecurityException
    {
        sm().checkAccess(g);
    }

    public void checkExit(int status) throws SecurityException
    {
        sm().checkExit(status);
    }

    public void checkExec(String cmd) throws SecurityException
    {
        sm().checkExec(cmd);
    }

    public void checkLink(String lib) throws SecurityException
    {
        sm().checkLink(lib);
    }

    public void checkRead(FileDescriptor fd) throws SecurityException
    {
        sm().checkRead(fd);
    }

    public void checkRead(String file) throws SecurityException
    {
        sm().checkRead(file);
    }

    public void checkRead(String file, Object context) throws SecurityException
    {
        sm().checkRead(file, context);
    }

    public void checkWrite(FileDescriptor fd) throws SecurityException
    {
        sm().checkWrite(fd);
    }

    public void checkWrite(String file) throws SecurityException
    {
        sm().checkWrite(file);
    }

    public void checkDelete(String file) throws SecurityException
    {
        sm().checkDelete(file);
    }

    public void checkConnect(String host, int port) throws SecurityException
    {
        sm().checkConnect(host, port);
    }

    public void checkConnect(String host, int port, Object context) throws SecurityException
    {
        sm().checkConnect(host, port, context);
    }

    public void checkListen(int port) throws SecurityException
    {
        sm().checkListen(port);
    }

    public void checkAccept(String host, int port) throws SecurityException
    {
        sm().checkAccept(host, port);
    }

    public void checkMulticast(InetAddress maddr) throws SecurityException
    {
        sm().checkMulticast(maddr);
    }

    public void checkMulticast(InetAddress maddr, byte ttl) throws SecurityException
    {
        sm().checkMulticast(maddr, ttl);
    }

    public void checkPropertiesAccess() throws SecurityException
    {
        sm().checkPropertiesAccess();
    }

    public void checkPropertyAccess(String key) throws SecurityException
    {
        sm().checkPropertyAccess(key);
    }

    /*
     * public void checkPropertyAccess(String key, String def) throws
     * SecurityException { sm().checkPropertyAccess(key, def); }
     */
    public boolean checkTopLevelWindow(Object window) throws SecurityException
    {
        return sm().checkTopLevelWindow(window);
    }

    public void checkPrintJobAccess() throws SecurityException
    {
        sm().checkPrintJobAccess();
    }

    public void checkSystemClipboardAccess() throws SecurityException
    {
        sm().checkSystemClipboardAccess();
    }

    public void checkAwtEventQueueAccess() throws SecurityException
    {
        sm().checkAwtEventQueueAccess();
    }

    public void checkPackageAccess(String pkg) throws SecurityException
    {
        sm().checkPackageAccess(pkg);
    }

    public void checkPackageDefinition(String pkg) throws SecurityException
    {
        sm().checkPackageDefinition(pkg);
    }

    public void checkSetFactory() throws SecurityException
    {
        sm().checkSetFactory();
    }

    public void checkMemberAccess(Class clazz, int which) throws SecurityException
    {
        sm().checkMemberAccess(clazz, which);
    }

    public void checkSecurityAccess(String provider) throws SecurityException
    {
        sm().checkSecurityAccess(provider);
    }

    public void checkPermission(Permission p) throws SecurityException
    {
        sm().checkPermission(p);
    }

    public static class NullSecurityManager extends SecurityManager
    {
        public void checkCreateClassLoader()
        {
        }

        public void checkAccess(Thread g)
        {
        }

        public void checkAccess(ThreadGroup g)
        {
        }

        public void checkExit(int status)
        {
        }

        public void checkExec(String cmd)
        {
        }

        public void checkLink(String lib)
        {
        }

        public void checkRead(FileDescriptor fd)
        {
        }

        public void checkRead(String file)
        {
        }

        public void checkRead(String file, Object context)
        {
        }

        public void checkWrite(FileDescriptor fd)
        {
        }

        public void checkWrite(String file)
        {
        }

        public void checkDelete(String file)
        {
        }

        public void checkConnect(String host, int port)
        {
        }

        public void checkConnect(String host, int port, Object context)
        {
        }

        public void checkListen(int port)
        {
        }

        public void checkAccept(String host, int port)
        {
        }

        public void checkMulticast(InetAddress maddr)
        {
        }

        public void checkMulticast(InetAddress maddr, byte ttl)
        {
        }

        public void checkPropertiesAccess()
        {
        }

        public void checkPropertyAccess(String key)
        {
        }

        // public void checkPropertyAccess(String key, String def) { }
        public boolean checkTopLevelWindow(Object window)
        {
            return true;
        }

        public void checkPrintJobAccess()
        {
        }

        public void checkSystemClipboardAccess()
        {
        }

        public void checkAwtEventQueueAccess()
        {
        }

        public void checkPackageAccess(String pkg)
        {
        }

        public void checkPackageDefinition(String pkg)
        {
        }

        public void checkSetFactory()
        {
        }

        public void checkMemberAccess(Class clazz, int which)
        {
        }

        public void checkSecurityAccess(String provider)
        {
        }

        public void checkPermission(Permission p)
        {
        }
    };

    //
    // Security manager that only accepts a single permission name
    //
    public static class DummySecurityManager extends NullSecurityManager
    {
        private String testPermissionName;

        public DummySecurityManager(String name)
        {
            testPermissionName = name;
        }

        public void checkPermission(Permission p)
        {
            if (testPermissionName.equals(p.getName()))
            {
                throw new SecurityException();
            }
            else
            {
                throw new IllegalArgumentException("Unexpected Security Check " + p.getName());
            }

        }
    }

    private static final SecurityManager NULL = new NullSecurityManager();
}
