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

package javax.microedition.xlet.ixc;

import java.lang.reflect.Method;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.util.Hashtable;
import javax.microedition.xlet.XletContext;

import org.cablelabs.impl.manager.ixc.IxcRegistryFactory;

//import com.sun.xlet.ixc.IxcRegistryImpl;
//import com.sun.xlet.mvmixc.MvmIxcRegistryImpl;

/**
 * <code>IXCRegistry</code> is the bootstrap mechanism for obtaining references
 * to remote objects residing in other Xlets executing on the same machine, but
 * in separate classloaders.
 * 
 * <p>
 * Instances of <code>IXCRegistry</code> are never accessible via
 * <code>java.rmi.Naming</code> or <code>java.rmi.registry.LocateRegistry</code>
 * if RMI functionality is implemented.
 * 
 * @see java.rmi.Registry
 */

public abstract class IxcRegistry implements Registry
{
    /**
     * Creates the IxcRegistry instance.
     */
    protected IxcRegistry()
    {
    }

    static boolean isPlatformChecked = false;

    static Method getRegistryImplMethod = null;

    static String svmIxcRegistryName = "com.sun.xlet.ixc.IxcRegistryImpl";

    static String mvmIxcRegistryName = "com.sun.xlet.mvmixc.MvmIxcRegistryImpl";

    /**
     * Returns the Inter-Xlet Communication registry.
     */
    public static IxcRegistry getRegistry(XletContext context)
    {

        if (context == null) throw new NullPointerException("XletContext is null");

        // Try to return a registry from the IxcRegistryFactory
        IxcRegistry reg = IxcRegistryFactory.getRegistry(context);
        if (reg != null) return reg;

        if (getRegistryImplMethod == null)
        {
            Class ixcRegistryImplClass = null;
            try
            {
                ixcRegistryImplClass = Class.forName(svmIxcRegistryName);
            }
            catch (Exception e)
            { // Not found, let's try MVM.
            }

            if (ixcRegistryImplClass == null)
            {
                try
                {
                    ixcRegistryImplClass = Class.forName(mvmIxcRegistryName);
                }
                catch (Exception e)
                { // Problem. ixcRegistryImplClass remains null.
                }
            }

            if (ixcRegistryImplClass == null)
            {
                System.out.println("Fatal error in starting IXC: ");
                System.out.println("Neither " + svmIxcRegistryName + " or " + mvmIxcRegistryName + " is found.");

                return null;
            }

            try
            {
                getRegistryImplMethod = ixcRegistryImplClass.getMethod("getIxcRegistryImpl",
                        new Class[] { javax.microedition.xlet.XletContext.class });
            }
            catch (NoSuchMethodException nsme)
            {
                nsme.printStackTrace();
            }
            catch (SecurityException se)
            {
                se.printStackTrace();
            }
        }

        try
        {
            if (getRegistryImplMethod != null)
            {
                return (IxcRegistry) getRegistryImplMethod.invoke(null, new Object[] { context });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns a reference, a stub, for the remote object associated with the
     * specified <code>name</code>.
     * 
     * @param name
     *            a URL-formatted name for the remote object
     * @return a reference for a remote object
     * @exception NotBoundException
     *                if name is not currently bound
     * @exception StubException
     *                If a stub could not be generated
     */

    public abstract Remote lookup(String name) throws StubException, NotBoundException;

    /**
     * Binds the specified <code>name</code> to a remote object.
     * 
     * @param name
     *            a URL-formatted name for the remote object
     * @param obj
     *            a reference for the remote object (usually a stub)
     * @exception AlreadyBoundException
     *                if name is already bound
     * @exception MalformedURLException
     *                if the name is not an appropriately formatted URL
     * @exception RemoteException
     *                if registry could not be contacted
     */
    public abstract void bind(String name, Remote obj) throws StubException, AlreadyBoundException;

    /**
     * Destroys the binding for the specified name that is associated with a
     * remote object.
     * 
     * @param name
     *            a URL-formatted name associated with a remote object
     * @exception NotBoundException
     *                if name is not currently bound
     */
    public abstract void unbind(String name) throws NotBoundException, AccessException;

    /**
     * Rebinds the specified name to a new remote object. Any existing binding
     * for the name is replaced.
     * 
     * @param name
     *            a URL-formatted name associated with the remote object
     * @param obj
     *            new remote object to associate with the name
     * @exception MalformedURLException
     *                if the name is not an appropriately formatted URL
     * @exception RemoteException
     *                if registry could not be contacted
     * @exception AccessException
     *                if this operation is not permitted (if originating from a
     *                non-local host, for example)
     */
    public abstract void rebind(String name, Remote obj) throws StubException, AccessException;

    /**
     * Returns an array of the names bound in the registry. The names are
     * URL-formatted strings. The array contains a snapshot of the names present
     * in the registry at the time of the call.
     * 
     * @return an array of names (in the appropriate URL format) bound in the
     *         registry
     * @exception RemoteException
     *                if registry could not be contacted
     * @exception AccessException
     *                if this operation is not permitted (if originating from a
     *                non-local host, for example)
     */
    public abstract String[] list();

    /**
     * Removes the bindings for all remote objects currently exported by the
     * calling Xlet.
     */
    public abstract void unbindAll();

}
