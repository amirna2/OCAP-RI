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

package org.cablelabs.impl.manager;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import javax.tv.xlet.XletContext;

public interface IxcManager extends Manager
{
    /**
     * Returns a remote object previously exported by an Xlet that has not been
     * destroyed. The identification of a remote object is given using a syntax
     * indicating the organisation ID and application ID:
     * 
     * <br>
     * /organisation_id/application_id/name <br>
     * organisation_id = the organisation ID of the Xlet, as signalled in the
     * application_identifier record. <br>
     * application_id = the application ID of the Xlet, as signalled in the
     * application_identifier record. <br>
     * name = the name under which the remote object was exported.
     * 
     * <p>
     * The organisation ID and the application ID shall each be encoded as a
     * hexadecimal string, as would be accepted by
     * java.lang.Integer.parseInt(String s, 16). If the caller is not authorized
     * to import a given object due to the security policy, then this API will
     * behave as though the object had not been exported, that is, a
     * NotBoundException shall be thrown.
     * 
     * @param xc
     *            The context of the current Xlet (that is, the Xlet importing
     *            the object).
     * @param path
     *            A file pathname-like string identifying the Xlet and the name
     *            of the object to be imported.
     * 
     * @return A remote object
     * 
     * @exception NotBoundException
     *                If the path is not currently bound.
     * @exception RemoteException
     *                If a remote stub class cannot be generated for the object
     *                being imported.
     * @exception java.lang.IllegalArgumentException
     *                If the path is not formatted in the syntax given above.
     * @exception NullPointerException
     *                if path is null
     **/
    public Remote lookup(XletContext xc, String path) throws NotBoundException, RemoteException;

    /**
     * Exports an object under a given name in the namespace of an Xlet. The
     * name can be any valid non-null String. No hierarchical namespace exists,
     * e.g. the names "foo" and "bar/../foo" are distinct. If the exporting xlet
     * has been destroyed, this method may fail silently.
     * 
     * @param xc
     *            The context of the Xlet exporting the object.
     * @param name
     *            The name identifying the object.
     * @param obj
     *            The object being exported
     * 
     * @exception AlreadyBoundException
     *                if this Xlet has previously exported an object under the
     *                given name.
     * 
     * @exception NullPointerException
     *                if xc, name or obj is null
     **/
    public void bind(XletContext xc, String name, Remote obj) throws AlreadyBoundException;

    /**
     * Unbind the name.
     * 
     * @param xc
     *            The context of the Xlet that exported the object to be
     *            unbound.
     * @param name
     *            The name identifying the object.
     * 
     * @exception NotBoundException
     *                if this is not currently any object exported by this Xlet
     *                under the given name.
     * 
     * @exception NullPointerException
     *                if xc or name is null
     **/
    public void unbind(XletContext xc, String name) throws NotBoundException;

    /**
     * Rebind the name to a new object in the context of an Xlet; replaces any
     * existing binding. The name can be any valid non-null String. No
     * hierarchical namespace exists, e.g. the names "foo" and "bar/../foo" are
     * distinct. If the exporting xlet has been destroyed, this method may fail
     * silently.
     * 
     * @param xc
     *            The context of the Xlet that exported the object.
     * @param name
     *            The name identifying the object.
     * @param obj
     *            The object being exported
     * 
     * @exception NullPointerException
     *                if xc, name or obj is null
     **/
    public void rebind(XletContext xc, String name, Remote obj);

    /**
     * Returns an array of string path objects available in the registry. The
     * array contains a snapshot of the names present in the registry that the
     * current Xlet would be allowed to import using IxcRegistry.lookup.
     * 
     * @param xc
     *            The context of the current Xlet.
     * 
     * @return A non-null array of strings containing a snapshot of the path
     *         names of all objects available to the caller in this registry.
     * 
     * @see IxcManager#lookup(javax.tv.xlet.XletContext,String)
     **/
    public String[] list(XletContext xc);

    /**
     * Creates a stub class instance that can be used for IXC communication or
     * where objects are passed by remote reference.
     * 
     * @param obj
     *            <code>Remote</code> object from which the stub class should be
     *            generated
     * @param source
     *            <code>CallerContext</code> of the source Xlet.
     * @param target
     *            <code>ClassLoader</code> of the target Xlet.
     * @return a stub class instance that implements all <code>Remote</code>
     *         methods.
     * @throws RemoteException
     *             if the stub class cannot be generated because one of the
     *             rules in MHP section 11.7.3.1.1 are violated.
     */
    public Remote createRemoteReference(Remote obj, CallerContext source, ClassLoader target) throws RemoteException;
}
