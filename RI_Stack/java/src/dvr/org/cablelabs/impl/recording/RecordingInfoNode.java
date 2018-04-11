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

/*
 * Created on Jul 14, 2005
 */
package org.cablelabs.impl.recording;

import org.cablelabs.impl.persistent.PersistentData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.dvb.application.AppID;

import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * This is the base class for the {@link RecordingInfo} and
 * {@link RecordingInfoTree} classes; it contains data that is common to both.
 * These classes are used to maintain a central store of recording-related
 * information that can be serialized. Serialization need not be the method used
 * to store <code>RecordingInfo</code> objects in persistent storage, but it
 * <i>is</i> supported if necessary.
 * 
 * <p>
 * Rules for Modifying the org.cablelabs.impl.recording classes:
 * 
 * <ul>
 * <li>Never refactor the object in any way that will change the class
 * hierarchy.
 * 
 * <li>Never remove a field.
 * 
 * <li>Never change a field from one type to another.
 * 
 * <li>When adding fields, allow for a default value when reading an older
 * version of the class.
 * 
 * <li>When adding a field, if the field is not serializable by default then
 * mark it as transient and add a serialization mechanism to the readObject and
 * writeObject methods.
 * 
 * <li>There is a {@link #serialVersionUID suid} variable that should not be
 * changed unless the developer is wanting to explicilty change the backward
 * compatibilty to a newer version than is currently allowed.
 * </ul>
 * 
 * @see org.ocap.shared.dvr.LeafRecordingRequest
 * @see org.ocap.shared.dvr.ParentRecordingRequest
 * @see org.cablelabs.impl.manager.RecordingDBManager
 * 
 * @author Aaron Kamienski
 */
public abstract class RecordingInfoNode extends PersistentData implements Serializable
{
    private static final Logger log = Logger.getLogger(RecordingInfoNode.class.getName());

    private static final long serialVersionUID = -4024309277598002813L;

    /**
     * Creates an instance of RecordingInfoNode.
     * 
     * @param uniqueId
     */
    public RecordingInfoNode(long uniqueId)
    {
        super(uniqueId);
    }

    /**
     * Creates an instance of RecordingInfoNode.
     * 
     * @param uniqueId
     */
    public RecordingInfoNode(long uniqueId, final RecordingInfoNode rinfo)
    {
        super(uniqueId);

        this.organization = rinfo.organization;
        this.appId = rinfo.appId;
        this.state = rinfo.state;
        this.priority = rinfo.priority;
        this.bitRate = rinfo.bitRate;
        this.retentionPriority = rinfo.retentionPriority;

        if (rinfo.expirationDate != null)
        {
            this.expirationDate = new Date(rinfo.expirationDate.getTime());
        }

        if (rinfo.fap != null)
        {
            this.fap = new ExtendedFileAccessPermissions(rinfo.fap.hasReadWorldAccessRight(),
                    rinfo.fap.hasWriteWorldAccessRight(), rinfo.fap.hasReadOrganisationAccessRight(),
                    rinfo.fap.hasWriteOrganisationAccessRight(), rinfo.fap.hasReadApplicationAccessRight(),
                    rinfo.fap.hasWriteApplicationAccessRight(), rinfo.fap.getReadAccessOrganizationIds(),
                    rinfo.fap.getWriteAccessOrganizationIds());
        }

        //
        // Need to deep-copy the contents of the rinfo.usedAppDataBytes
        //

        // this.usedAppDataBytes is already blank (via instance initializer)

        if (rinfo.usedAppDataBytes != null)
        {
            Enumeration appEnum = rinfo.usedAppDataBytes.keys();

            while (appEnum.hasMoreElements())
            {
                Object appIDKey = appEnum.nextElement();
                Object object = rinfo.usedAppDataBytes.get(appIDKey);

                // objects of the usedAppDataBytes table are just Ints
                // so this is a simple copy

                this.usedAppDataBytes.put(appIDKey, object);
            }
        } // END if (rinfo.usedAppDataBytes != null)

        //
        // Need to deep-copy the contents of the rinfo.usedAppDataBytes
        //

        // this.appDataTable is already blank (via instance initializer)

        if (rinfo.appDataTable != null)
        {
            Enumeration appEnum = rinfo.appDataTable.keys();

            while (appEnum.hasMoreElements())
            {
                Object appIDKey = appEnum.nextElement();
                Hashtable appSpecDataTable = (Hashtable) (rinfo.appDataTable.get(appIDKey));

                Hashtable newAppDataTable = new Hashtable();

                this.appDataTable.put(appIDKey, newAppDataTable);

                // Now copy the app data for this application
                Enumeration dataEnum = appSpecDataTable.keys();

                while (dataEnum.hasMoreElements())
                {
                    String dataKey = (String) dataEnum.nextElement();
                    AppDataContainer appData = (AppDataContainer) appSpecDataTable.get(dataKey);
                    newAppDataTable.put(dataKey, new AppDataContainer(appData));
                } // END while (dataEnum.hasMoreElements())
            } // END while (appEnum.hasMoreElements())
        } // END if (rinfo.appDataTable != null)
    } // END RecordingInfoNode.RecordingInfoNode(long uniqueId, final
      // RecordingInfoNode rinfo)

    /**
     * The application which <i>owns</i> this recording.
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getAppID
     */
    private transient AppID appId;

    private transient MediaStorageVolumeReference destMSV = null;

    /**
     * The current state of this recording.
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getState
     */
    private int state;

    /**
     * This maps AppID {@link AppID#toString strings} to app-specific
     * <code>Hashtables</code>. The app-specific <code>Hashtables</code> in turn
     * map <code>String</code> keys to application-specific data. The
     * application-specific data is to be wrapped within an instance of
     * {@link AppDataContainer}.
     * <p>
     * Previous revisions of this code did not wrap the application-specific
     * <code>Serializable</code> in this manner. The {@link #readObject}
     * implementation ensures that previous versions of stored data are updated
     * to the current version by wrapping everything within an instance of
     * <code>AppDataContainer</code>.
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#addAppData(String,
     *      java.io.Serializable)
     */
    private Hashtable appDataTable = new Hashtable();

    /**
     * This is used to keep track of the total size of all application data for
     * each application. This maps AppID {@link AppID#toString strings} to
     * <code>Integer</code>s containing the summed size of all
     * application-specific datas.
     */
    private Hashtable usedAppDataBytes = new Hashtable();

    /**
     * The expiration date for this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getExpirationDate
     * @since 1
     */
    private Date expirationDate;

    /**
     * The desired recording bitRate for this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getRequestedBitRate
     * @since 1
     */
    private byte bitRate;

    /**
     * The file access permissions associated with this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getFileAccessPermissions
     * @since
     */
    private transient ExtendedFileAccessPermissions fap;

    /**
     * The priority flag of this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getPriorityFlag
     * @since
     */
    private byte priority;

    /**
     * The retention priority field of this recording.
     */
    private int retentionPriority;

    /**
     * The owning organization for this recording.
     * 
     * @see org.ocap.dvr.RecordingManager#record
     * @since
     */
    private String organization;

    private static final int WORLD_R = 0x20;

    private static final int WORLD_W = 0x10;

    private static final int ORG_R = 0x08;

    private static final int ORG_W = 0x04;

    private static final int APP_R = 0x02;

    private static final int APP_W = 0x01;

    /**
     * Gets the value of organization
     * 
     * @return the value of organization
     */
    public String getOrganization()
    {
        return this.organization;
    }

    /**
     * Sets the value of organization
     * 
     * @param argOrganization
     *            Value to assign to this.organization
     */
    public void setOrganization(String argOrganization)
    {
        this.organization = argOrganization;
    }

    // end new fields and methods

    /**
     * Gets the value of appId
     * 
     * @return the value of appId
     */
    public AppID getAppId()
    {
        return this.appId;
    }

    /**
     * Sets the value of appId
     * 
     * @param argAppId
     *            Value to assign to this.appId
     */
    public void setAppId(AppID argAppId)
    {
        this.appId = argAppId;
    }

    /**
     * Gets the value of state
     * 
     * @return the value of state
     */
    public int getState()
    {
        return this.state;
    }

    /**
     * Sets the value of state
     * 
     * @param argState
     *            Value to assign to this.state
     */
    public void setState(int argState)
    {
        this.state = argState;
    }

    /**
     * Gets the value of appDataTable
     * 
     * @return the value of appDataTable
     */
    public Hashtable getAppDataTable()
    {
        return this.appDataTable;
    }

    /**
     * Sets the value of appDataTable
     * 
     * @param argAppDataTable
     *            Value to assign to this.appDataTable
     */
    public void setAppDataTable(Hashtable argAppDataTable)
    {
        this.appDataTable = argAppDataTable;
    }

    /**
     * Gets the value of usedAppDataBytes
     * 
     * @return the value of usedAppDataBytes
     */
    public Hashtable getUsedAppDataBytes()
    {
        return this.usedAppDataBytes;
    }

    /**
     * Sets the value of usedAppDataBytes
     * 
     * @param argUsedAppDataBytes
     *            Value to assign to this.usedAppDataBytes
     */
    public void setUsedAppDataBytes(Hashtable argUsedAppDataBytes)
    {
        this.usedAppDataBytes = argUsedAppDataBytes;
    }

    /**
     * Gets the value of expirationDate
     * 
     * @return the value of expirationDate
     */
    public Date getExpirationDate()
    {
        return this.expirationDate;
    }

    /**
     * Sets the value of expirationDate
     * 
     * @param argExpirationDate
     *            Value to assign to this.expirationDate
     */
    public void setExpirationDate(Date argExpirationDate)
    {
        this.expirationDate = argExpirationDate;
    }

    /**
     * Gets the value of fap
     * 
     * @return the value of fap
     */
    public ExtendedFileAccessPermissions getFap()
    {
        return this.fap;
    }

    /**
     * Sets the value of fap
     * 
     * @param argFap
     *            Value to assign to this.fap
     */
    public void setFap(ExtendedFileAccessPermissions argFap)
    {
        this.fap = argFap;
    }

    /**
     * Gets the value of priority
     * 
     * @return the value of priority
     */
    public byte getPriority()
    {
        return this.priority;
    }

    /**
     * Sets the value of priority
     * 
     * @param argPriority
     *            Value to assign to this.priority
     */
    public void setPriority(byte argPriority)
    {
        this.priority = argPriority;
    }

    /**
     * Gets the value of retention priority
     * 
     * @return the value of retention priority
     */
    public int getRetentionPriority()
    {
        return this.retentionPriority;
    }

    /**
     * Sets the value of retention priority
     * 
     * @param argPriority
     *            Value to assign to this.retentionPriority
     */
    public void setRetentionPriority(int argRetentionPriority)
    {
        this.retentionPriority = argRetentionPriority;
    }

    /**
     * Gets the value of bitRate
     * 
     * @return the value of bitRate
     */
    public byte getBitRate()
    {
        return this.bitRate;
    }

    /**
     * Sets the value of bitRate
     * 
     * @param argBitRate
     *            Value to assign to this.bitRate
     */
    public void setBitRate(byte argBitRate)
    {
        this.bitRate = argBitRate;
    }

    /**
     * Gets the value of the unique RecordingRequest ID, per ECN 829. Not to be
     * confused with the RecordingID:String used to identify native recordings.
     * Downcasting should not result in a loss of data because the range of
     * unique ID is managed.
     * 
     * @return the value of the unique id.
     */
    public int getUniqueIDInt()
    {
        return (int) super.uniqueId;
    }

    /**
     * Overrides {@link Object#equals}. {@link #uniqueId} should be used to 
     * determine if two records refer to the same recording.
     * 
     * @return <code>true</code> if all fields match, <code>false</code>
     *         otherwise
     */
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof RecordingInfoNode)) return false;

        RecordingInfoNode other = (RecordingInfoNode) obj;

        return (uniqueId == other.uniqueId);
    }

    /**
     * Overrides {@link Object#hashCode}. This is implemented simply because
     * <code>equals()</code> is.
     * 
     * @return a function of the uniqueId
     */
    public int hashCode()
    {
        return (int) (uniqueId & 0xFFFFFFFF) ^ (int) (uniqueId >> 32);
    }

    /**
     * Overrides super implementation.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer("RecordingInfoNode:[uid 0x");

        sb.append(Long.toHexString(uniqueId)).append(",appId ").append(appId).append(",state ").append(state).append(
                ",pri ").append(priority).append(",br ").append(bitRate).append(",MSV ").append(destMSV).append(
                ",org '").append((organization == null) ? "null" : organization).append("',expr ").append(
                this.expirationDate).append(",rpri ").append(this.retentionPriority).append(",fap ").append(
                toString(this.fap)).append(",appdata [");

        // Go through the fun of dumping the app data...
        if ((this.appDataTable == null) || (this.appDataTable.size() == 0))
        {
            sb.append("none");
        }
        else
        {
            Enumeration appEnum = this.appDataTable.keys();

            while (appEnum.hasMoreElements())
            {
                Object appIDKey = appEnum.nextElement();

                sb.append("appId ").append(appIDKey);
                sb.append(":[size ").append(this.usedAppDataBytes.get(appIDKey));
                sb.append(",vals [");

                Hashtable appSpecDataTable = (Hashtable) (this.appDataTable.get(appIDKey));

                // Now walk the key/object pairs
                Enumeration dataEnum = appSpecDataTable.keys();

                while (dataEnum.hasMoreElements())
                {
                    String dataKey = (String) dataEnum.nextElement();
                    AppDataContainer appData = (AppDataContainer) appSpecDataTable.get(dataKey);

                    sb.append('\'').append(dataKey).append("'=").append(appData);
                    if (dataEnum.hasMoreElements())
                    {
                        sb.append(',');
                    }
                } // END while (dataEnum.hasMoreElements())
                sb.append("]]");
            } // END while (appEnum.hasMoreElements())
        }

        sb.append("]]");

        return sb.toString();
    } // END toString()

    /**
     * @see #toString()
     */
    private String toString(ExtendedFileAccessPermissions fap)
    {
        if (fap == null) return "{null}";

        int perm = ((fap.hasReadWorldAccessRight() ? WORLD_R : 0) | (fap.hasWriteWorldAccessRight() ? WORLD_W : 0)
                | (fap.hasReadOrganisationAccessRight() ? ORG_R : 0)
                | (fap.hasWriteOrganisationAccessRight() ? ORG_W : 0)
                | (fap.hasReadApplicationAccessRight() ? APP_R : 0) | (fap.hasWriteApplicationAccessRight() ? APP_W : 0));

        StringBuffer sb = new StringBuffer("{perms ");

        sb.append(Integer.toHexString(perm));
        sb.append(",raoids ").append(toString(fap.getReadAccessOrganizationIds()));
        sb.append(",waoids ").append(toString(fap.getWriteAccessOrganizationIds()));
        sb.append('}');

        return sb.toString();
    }

    /**
     * @see #toString(ExtendedFileAccessPermissions fap)
     */
    private StringBuffer toString(int[] oids)
    {
        if ((oids == null) || (oids.length == 0)) return new StringBuffer("[none]");

        StringBuffer sb = new StringBuffer('[');

        for (int i = 0; i < oids.length; ++i)
        {
            sb.append(oids[i]).append(',');
        }
        sb.append(']');
        return sb;
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
        out.writeBoolean(appId != null);
        if (appId != null)
        {
            out.writeInt(appId.getOID());
            out.writeInt(appId.getAID());
        }

        // ExtendedFileAccessPermissions
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
        }
        // MSV
        if (destMSV == null || destMSV.getVolumeName() == null)
        {
            out.writeBoolean(false);
        }
        else
        {
            out.writeBoolean(true);
            int orgId = destMSV.getAppID().getOID();
            int appId = destMSV.getAppID().getAID();
            String volumeName = destMSV.getVolumeName();
            String deviceName = destMSV.getDeviceName();
            out.writeInt(orgId);
            out.writeInt(appId);
            out.writeUTF(volumeName);
            out.writeUTF(deviceName);
        }
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

            appId = new AppID(oid, aid);
        }

        // If the bitrate is zero,
        // do not attempt to read the next
        // fields because the stream has not yet
        // been updated.
        if (bitRate != 0)
        {
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

                fap = new ExtendedFileAccessPermissions((hash & WORLD_R) != 0, (hash & WORLD_W) != 0,
                        (hash & ORG_R) != 0, (hash & ORG_W) != 0, (hash & APP_R) != 0, (hash & APP_W) != 0, readOid,
                        writeOid);
            }
        }

        try
        {
            if (!in.readBoolean())
            {
                destMSV = new MediaStorageVolumeReference();
            }
            else
            {
                int orgId = in.readInt();
                int appId = in.readInt();
                AppID appID = new AppID(orgId, appId);
                String volumeName = in.readUTF();
                String deviceName = in.readUTF();
                destMSV = new MediaStorageVolumeReference(appID, volumeName, deviceName);
                destMSV.updateMSV();
            }
        }
        catch (Exception e)
        {
            destMSV = new MediaStorageVolumeReference();
        }
        // iterate over the appDataTable to ensure that everything is an
        // instance of AppDataContainer
        updateAppDataContainers();
    }

    public void setDestination(MediaStorageVolume volume)
    {
        destMSV = new MediaStorageVolumeReference(volume);
    }

    public MediaStorageVolume getDestination()
    {
        return destMSV.getMSV();
    }

    public MediaStorageVolumeReference getMSVReference()
    {
        return destMSV;
    }

    /**
     * Iterates over the {@link #appDataTable}, ensuring that everything is an
     * instance of <code>AppDataContainer</code>. If not, then the data is
     * wrapped in an instance of <code>AppDataContainer</code>. If that fails,
     * then the data is removed altogether.
     * <p>
     * After this operation completes, the <code>appDataTable</code> will
     * contain <i>appIdString:Hashtable</i> pairs where the component
     * <code>Hashtable</code>'s contain <i>key:AppDataContainer</i> pairs.
     */
    private void updateAppDataContainers()
    {
        if (appDataTable == null) return;

        for (Enumeration e = appDataTable.keys(); e.hasMoreElements();)
        {
            updateAppDataContainers(appDataTable.get(e.nextElement()));
        }
    }

    /**
     * Iterates over the application-specific data contained within the given
     * <code>Hashtable</code> and ensures that everything is wrapped within an
     * instance of <code>AppDataContainer</code>. If not, then the data is
     * wrapped in an instance of <code>AppDataContainer</code>. If that fails,
     * then the data is removed altogether.
     * <p>
     * If <i>obj</i> is not a <code>Hashtable</code>, then this method returns
     * and does nothing.
     * 
     * @param obj
     *            the hashtable containing <i>key:appData</i> pairs
     */
    private void updateAppDataContainers(Object obj)
    {
        if (obj == null || !(obj instanceof Hashtable)) return;
        Hashtable h = (Hashtable) obj;

        // Determine which entries need to be updated
        // Note: this two step operation is necessary to avoid concurrent
        // modification errors
        Vector toFix = new Vector();
        for (Enumeration e = h.keys(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            Object data = h.get(key);

            if (!(data instanceof AppDataContainer)) toFix.addElement(key);
        }

        // Update those entries
        for (Enumeration e = toFix.elements(); e.hasMoreElements();)
        {
            Object key = e.nextElement();
            Object data = h.get(key);

            try
            {
                // Replace appData with an AppDataContainer
                AppDataContainer appData = new AppDataContainer((Serializable) data);
                h.put(key, appData);
            }
            catch (Exception ex)
            {
                // Remove the entry altogether
                h.remove(key);
            }
        }
    }

}
