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

import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.persistent.PersistentData;

import java.io.Serializable;
import java.util.Date;

/**
 * Persistent data represents a stored application or registered API within the
 * runtime database.
 * 
 * @author Aaron Kamienski
 * 
 * @see StorageEntrySerializer
 */
class StorageEntry extends PersistentData implements Serializable
{
    /**
     * Key that can be used to lookup this object.
     * 
     * @see AppKey
     * @see ApiKey
     */
    Object key;

    /**
     * The status of the storage. If this is <code>true</code> then all files
     * have been stored and should remain stored. If this is <code>false</code>
     * then files should be deleted at the next opportunity.
     */
    boolean status = false;

    /**
     * Indicates a partial or complete storage of the file set. For
     * applications, if storage was not fully completed due to a transport
     * communication error, subsequent signalling will resume storage at the
     * point it was previously terminated
     */
    boolean complete = false;

    /**
     * The size of the files in storage. This should be used in preventing the
     * storage of too many files.
     */
    long size;

    /**
     * The priority of the files in storage.
     */
    int priority;

    /**
     * A representation of the original <i>Application Description</i> or
     * <i>Shared Classes Description</i> file used to store the files.
     */
    AppDescriptionInfo desc;

    /**
     * The storage date represented in milliseconds. This is initialized using
     * {@link System.currentTimeMillis()} when this object is created. The
     * original time is saved and restored with serialization.
     */
    final long storageTime;

    /**
     * The last modification date represented in milliseconds. This is modified
     * by {@link StorageEntrySerializer#saveEntry} prior to storing any changes.
     */
    long modificationTime;

    /**
     * Whether the contents of this file have been authenticated or not. If this is
     * <code>true</code> then all files have been authenticated, and additional is
     * not necessary. If this is <code>false</code>, then files must be
     * authenticated before use; once authenticated this should be set to
     * <code>true</code>.
     */
    boolean authenticated = false;

    /** Serialization Version Unique ID. */
    private static final long serialVersionUID = 4196095595112439677L;

    /**
     * Creates an instance of StorageEntry with the given <i>uniqueId</i>. It is
     * the responsibility of the caller to manage the uniqueIds (and make sure
     * that they are, in fact, unique).
     * 
     * @param uniqueId
     *            the unique ID to use
     */
    StorageEntry(long uniqueId, Object key)
    {
        super(uniqueId);

        this.key = key;
        this.storageTime = System.currentTimeMillis();
    }

    /**
     * Returns a <code>String</code> representation of the contents of this
     * object. This is used in the implementation of {@link #toString}.
     * 
     * @return a <code>String</code> representation of the contents of this
     *         object
     */
    protected StringBuffer toStringInner()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(Long.toHexString(uniqueId)).append(':');
        sb.append("key=").append(key).append(',');
        sb.append("valid=").append(status).append(',');
        sb.append("size=").append(size).append(',');
        sb.append("pri=").append(priority).append(',');
        sb.append("complete=").append(complete).append(',');
        sb.append("c=").append(new Date(storageTime)).append(',');
        sb.append("m=").append(new Date(modificationTime)).append(',');

        return sb;
    }

    /**
     * Overrides {@link Object#toString()}.
     * 
     * @returns a <code>String</code> representation of this object
     * @see #toStringInner
     */
    public String toString()
    {
        return super.toString() + "[" + toStringInner() + "]";
    }
}
