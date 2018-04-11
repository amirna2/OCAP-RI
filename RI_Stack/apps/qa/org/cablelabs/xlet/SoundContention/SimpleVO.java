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
package org.cablelabs.xlet.SoundContention;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.tv.xlet.XletContext;

/**
 * 
 * SimpleVO implements a simple Value Object. Using IXC a SimpleVO object can be
 * used by either the owner of the object or by another Xlet.
 */

public class SimpleVO implements SimpleRemote
{
    private int value = 0;

    private Vector valueChangedListenerV = new Vector();

    private SoundContention1 master;

    /**
     * 
     * @param m
     *            - The Owner of the object. Its applet context is used to bind
     *            the object.
     */

    public SimpleVO(SoundContention1 contention1)
    {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.xlet.ixcSample.SimpleRemote#setValue(int)
     */
    public void setValue(int v) throws RemoteException
    {
        value = v;

        System.out.println("Exporter.setValue " + v + " callbackSize=" + valueChangedListenerV.size());
        new Thread()
        {
            public void run()
            {
                for (int i = 0; i < valueChangedListenerV.size(); i++)
                {
                    ValueChangedListener l = (ValueChangedListener) valueChangedListenerV.elementAt(i);
                    System.out.println("SimpleVO.setValue before valueChanged");
                    try
                    {
                        l.valueChanged();
                    }
                    catch (RemoteException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println("SimpleVO.setValue after valueChanged");
                }
            }
        }.start();
    }

    /**
     * Return the value.
     * 
     * @see org.cablelabs.xlet.ixcSample.SimpleRemote#getValue()
     */
    public int getValue() throws RemoteException
    {
        return value;
    }

    /**
     * 
     * @param ctx
     */
    public void exportMe(XletContext ctx)
    {

        try
        {
            org.dvb.io.ixc.IxcRegistry.bind(ctx, "Exporter", this);
        }
        catch (AlreadyBoundException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Notify me when the value changes. Call this if SimpleVO is not a remote
     * object.
     * 
     * @param l
     * @throws RemoteException
     */
    public void addValueChangedListener(ValueChangedListener l) throws RemoteException
    {
        valueChangedListenerV.addElement(l);
    }

    /**
     * Cleanup Call this if SimpleVO is not a remote object.
     * 
     * @param l
     * @throws RemoteException
     */
    public void removeValueChangedListener(ValueChangedListener l) throws RemoteException
    {
        valueChangedListenerV.removeElement(l);
    }

    /**
     * Call if this is a remote object.
     * 
     * @see org.cablelabs.xlet.ixcSample.SimpleRemote#addValueChangedListener(java.lang.String)
     */
    public void addValueChangedListener(String name) throws RemoteException
    {
        final String fname = name;

        // TODO I should not have to start this thread.
        new Thread()
        {
            public void run()
            {
                try
                {
                    addValueChangedListener(lookupValueChangedListener(fname));
                }
                catch (RemoteException e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Call if this is a remote object.
     * 
     * @see org.cablelabs.xlet.ixcSample.SimpleRemote#removeValueChangedListener(java.lang.String)
     */
    public void removeValueChangedListener(String name) throws RemoteException
    {
        removeValueChangedListener(lookupValueChangedListener(name));
    }

    /**
     * Get a reference to the remote object.
     * 
     * @param name
     * @throws RemoteException
     */
    private ValueChangedListener lookupValueChangedListener(String name) throws RemoteException
    {
        try
        {
            ValueChangedListener l = (ValueChangedListener) org.dvb.io.ixc.IxcRegistry.lookup(master.getContext(), name);
            System.out.println("SimpleVO.lookupValueChangedListener " + l);
            return l;
        }
        catch (NotBoundException e)
        {
            e.printStackTrace();
            throw new RemoteException();
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
            throw new RemoteException();
        }
    }

    /**
     * Pass an object to a remote method.
     * 
     * @see org.cablelabs.xlet.ixcSample.SimpleRemote#test(org.cablelabs.xlet.ixcSample.TestClass)
     */
    public void test(TestSerializableClass t) throws RemoteException
    {
        System.out.println("SimpleVO.test " + t.i1 + " " + t.s1);
    }
}
