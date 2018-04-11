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

package org.cablelabs.impl.manager.appstorage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.dvb.application.AppID;

/**
 * Combines an <code>AppID</code> and an <code>int</code> version to serve as a
 * unique key for a stored application.
 * 
 * @author Aaron Kamienski
 */
class AppKey implements Serializable
{
    private static final long serialVersionUID = -9085625202244170971L;

    /**
     * Creates an instance of AppKey.
     * 
     * @param id
     *            <code>AppID</code> for the app in storage
     * @param version
     *            version of the app in storage
     */
    AppKey(AppID id, long version)
    {
        this.id = id;
        this.version = version;
    }

    /**
     * The <code>AppID</code> for a stored application.
     */
    transient AppID id;

    /**
     * The version of a stored application.
     */
    final long version;

    /**
     * Overrides {@link Object#equals(java.lang.Object)}.
     * 
     * @return <code>true</code> if <i>obj</i> is equivalent to this
     */
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != AppKey.class) return false;

        return version == ((AppKey) obj).version && id.equals(((AppKey) obj).id);
    }

    /**
     * Overrides {@link Object#hashCode()}.
     */
    public int hashCode()
    {
        return ((int)(version & 0xFFFFFFFF)) ^ id.hashCode();
    }

    /**
     * Overrides {@link java.lang.Object#toString()}.
     * 
     * @return a string representation of this key
     */
    public String toString()
    {
        return id + ":" + version;
    }

    /**
     * Handles serialization of non-serializable or transient fields. E.g.,
     * <code>AppID</code>.
     * 
     * @param out
     *            the stream to write to
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();

        out.writeInt(id.getOID());
        out.writeInt(id.getAID());
    }

    /**
     * Handles de-serialization of non-serializable or transient fields. E.g.,
     * <code>AppID</code>.
     * 
     * @param in
     *            the stream to read from
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        int oid = in.readInt();
        int aid = in.readInt();

        id = new AppID(oid, aid);
    }
}
