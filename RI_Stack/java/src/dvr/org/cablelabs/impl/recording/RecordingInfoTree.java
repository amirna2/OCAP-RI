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

package org.cablelabs.impl.recording;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;

import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * A central store of recording information that can be serialized.
 * Serialization need not be the method used to store <code>RecordingInfo</code>
 * objects in persistent storage, but it <i>is</i> supported if necessary.
 * <p>
 * 
 * @see org.ocap.dvr.recording.ParentRecordingRequest
 * @see org.cablelabs.impl.recording.RecordingInfo
 * @see org.cablelabs.impl.manager.RecordingDBManager
 * 
 * @author Aaron Kamienski
 */
public class RecordingInfoTree extends RecordingInfoNode implements Serializable
{

    private static final long serialVersionUID = 7711551182641653515L;

    private static final int WORLD_R = 0x20;

    private static final int WORLD_W = 0x10;

    private static final int ORG_R = 0x08;

    private static final int ORG_W = 0x04;

    private static final int APP_R = 0x02;

    private static final int APP_W = 0x01;

    /**
     * A <code>Vector</code> of child <code>RecordingInfoNode</code>s.
     * <p>
     * Note that this field is serialized (it is <code>transient</code> and
     * otherwise not serialized by a <code>writeObject()</code>
     * implementation</code>). It is considered the responsibility of the
     * {@link org.cablelabs.impl.manager.RecordingDBManager} to save and restore
     * the tree hiearchy. This is because we don't want to serialize a
     * <code>Vector</code> of <code>RecordingInfoNode</code>s directly, as
     * de-serialization may produce multiple copies.
     */
    public transient Vector children;

    private transient Serializable appPrivateData = null;

    public void setAppPrivateData(Serializable apd)
    {
        appPrivateData = apd;
    }

    public Serializable getAppPrivateData()
    {
        return appPrivateData;
    }

    public Vector getChildren()
    {
        return this.children;
    }

    public void setChildren(Vector argChildren)
    {
        this.children = argChildren;
    }

    /**
     * Overrides super implementation.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (obj == null || (getClass() != obj.getClass())) return false;
        if (!super.equals(obj)) return false;

        RecordingInfoTree o = (RecordingInfoTree) obj;

        return (children == null) ? (o.children == null) : children.equals(o.children);
    }

    /**
     * Overrides super implementation.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("RecordingInfoTree@");
        sb.append(System.identityHashCode(this))
                .append('[')
                .append(super.toString())
                .append(',')
                .append(children)
                .append(']');
        return sb.toString();
    }

    /**
     * Creates an instance of RecordingInfoLeaf.
     * 
     * @param uniqueId
     */
    public RecordingInfoTree(long uniqueId)
    {
        super(uniqueId);
    }

    /**
     * 
     * Default values are set by the implementation. If the were not set then
     * this routine will execute.
     * 
     */
    private void updateSuperClass()
    {
        setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
        setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
        setRetentionPriority(OcapRecordingProperties.DELETE_AT_EXPIRATION);
        setFap(null);
        setOrganization(null);
        // default storage volume.
    }

    /**
     * Provide for de-serialization of non-serializable objects contained
     * within. This takes care of reading <i>transient</i> fields from the given
     * <code>ObjectInputStream</code>.
     * <p>
     * This method is implemented for serialization only, not for general usage.
     * <p>
     * This method doesn' de-serialize the {@link #appDataTable} but it does
     * ensure that <i>older</i> revisions are sufficiently upgraded.
     * 
     * @param in
     * 
     * @see #appId
     * @see #appDataTable
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // Read default fields
        in.defaultReadObject();

        // Read in non-seriallizable/transient fields

        // AppID
        if (in.readBoolean())
        {
            int oid = in.readInt();
            int aid = in.readInt();

            setAppId(new AppID(oid, aid));
        }

        // ExtendedFileAccessPermissions
        if (in.readBoolean())
        {
            int hash = in.readInt();
            int length = in.readInt();
            int[] readOid = (length == -1) ? null : (new int[length]);
            for (int i = 0; i < length; ++i)
                readOid[i] = in.readInt();
            length = in.readInt();
            int[] writeOid = (length == -1) ? null : (new int[length]);
            for (int i = 0; i < length; ++i)
                writeOid[i] = in.readInt();

            setFap(new ExtendedFileAccessPermissions((hash & WORLD_R) != 0, (hash & WORLD_W) != 0, (hash & ORG_R) != 0,
                    (hash & ORG_W) != 0, (hash & APP_R) != 0, (hash & APP_W) != 0, readOid, writeOid));
        }
        setAppPrivateData((Serializable) in.readObject());

    }

    /**
     * Provide for serialization of non-serializable objects contained within.
     * This takes care of writing <i>transient</i> fields to the given
     * <code>ObjectOutputStream</code>.
     * <p>
     * This method is implemented for serialization only, not for general usage.
     * 
     * @param out
     * 
     * @see #appId
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Write default fields
        out.defaultWriteObject();

        // Write out non-serializable/transient fields

        // AppID
        org.dvb.application.AppID appId = getAppId();
        out.writeBoolean(appId != null);
        if (appId != null)
        {
            out.writeInt(appId.getOID());
            out.writeInt(appId.getAID());
        }

        // ExtendedFileAccessPermissions
        org.ocap.storage.ExtendedFileAccessPermissions fap = getFap();
        out.writeBoolean(fap != null);
        if (fap != null)
        {
            int hash = (fap.hasReadWorldAccessRight() ? WORLD_R : 0) | (fap.hasWriteWorldAccessRight() ? WORLD_W : 0)
                    | (fap.hasReadOrganisationAccessRight() ? ORG_R : 0)
                    | (fap.hasWriteOrganisationAccessRight() ? ORG_W : 0)
                    | (fap.hasReadApplicationAccessRight() ? APP_R : 0)
                    | (fap.hasWriteApplicationAccessRight() ? APP_W : 0);
            out.writeInt(hash);

            int[] readOid = fap.getReadAccessOrganizationIds();
            if (readOid == null)
                out.writeInt(-1);
            else
            {
                out.writeInt(readOid.length);
                for (int i = 0; i < readOid.length; ++i)
                {
                    out.writeInt(readOid[i]);
                }
            }

            int[] writeOid = fap.getWriteAccessOrganizationIds();
            if (writeOid == null)
                out.writeInt(-1);
            else
            {
                out.writeInt(writeOid.length);
                for (int i = 0; i < writeOid.length; ++i)
                {
                    out.writeInt(writeOid[i]);
                }
            }

            out.writeObject(getAppPrivateData());
        }
    }

}
