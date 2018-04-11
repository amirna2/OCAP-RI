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
import java.util.Date;

import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.DeliverySystemType;

import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;


/**
 * A central store of recording information that can be serialized.
 * Serialization need not be the method used to store <code>RecordingInfo</code>
 * objects in persistent storage, but it <i>is</i> supported if necessary.
 * 
 * -- IMPORTANT -- You must manually keep this data in sync with
 * DVRUpgradeRecordingInfo, DVRUpgradeManager and
 * RecordingManagerImpl.addOrphanedRecording().
 * 
 * @see org.ocap.dvr.recording.LeafRecordingRequest
 * @see org.cablelabs.impl.recording.RecordingInfoTree
 * @see org.cablelabs.impl.manager.RecordingDBManager
 * 
 * @author Aaron Kamienski
 */
public class RecordingInfo extends RecordingInfoNode implements Serializable
{
    private static final long serialVersionUID = -5682869065502527169L;

    /**
     * The expiration date for this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getExpirationDate
     */
    private Date expirationDate;

    /**
     * The owning organization for this recording.
     * 
     * @see org.ocap.dvr.RecordingManager#record
     */
    private String organization;

    /**
     * The file access permissions associated with this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getFileAccessPermissions
     */
    private transient ExtendedFileAccessPermissions fap;

    /**
     * The priority flag of this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getPriorityFlag
     */
    private byte priority;

    /**
     * The retention priority field of this recording.
     * 
     */
    private int retentionPriority;

    /**
     * The desired recording bitRate for this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getRequestedBitRate
     */
    private byte bitRate;

    /**
     * The desired recording duration.
     * 
     * @see org.ocap.dvr.RecordingSession#getRequestedDuration
     */
    private long duration;

    /**
     * The scheduled recording start time.
     * 
     * @see org.ocap.dvr.RecordingSession#getStartTime
     */
    private long startTime;

    // ServiceDescription fields.

    /**
     * The <code>String</code> that is the service description for this
     * RecordingInfo. Used to satisfy the implementation of ServiceDescription.
     */
    private String serviceDescription;

    /**
     * The <code>Date</code> that is the update time for this recording info.
     * Used to satisfy the implementation of ServiceDescription.
     */
    private Date serviceDescriptionUpdateTime;

    // ServiceDetails fields
    private String longName;

    // ServiceDetails.DeliverySystemType
    private transient DeliverySystemType deliverySystemType = DeliverySystemType.UNKNOWN;

    // SIElement ServiceInformationType
    private transient ServiceInformationType serviceInformationType = ServiceInformationType.UNKNOWN;

    // ServiceDetails isFree
    private boolean isFree;

    // ServiceDetails caSystemIds
    private int[] caSystemIds;

    // ServiceComponents collection for this Recording.
    // TODO: this implementation will need to change when
    // components that change during the recording are implemented.
    RecordedServiceComponentInfo[] serviceComponents = new RecordedServiceComponentInfo[0];

    /**
     * The time this recording was actually started
     * 
     * @see org.ocap.dvr.RecordingSession#getStartTime
     */
    private long actualStartTime;

    /**
     * DeletionDetails attributes
     * 
     * @see <code>DeletionDetails</code>
     */
    private long deletionTime;

    /**
     * DeletionDetails attributes
     * 
     * @see <code>DeletionDetails</code>
     */
    private int deletionReason;

    /**
     *
     */
    private long mediaTime;

    /**
     * The locator for the recorded service.
     * 
     * @see org.ocap.dvr.RecordedService#getLocator
     */
    private transient OcapLocator[] serviceLocator;

    /**
     * The name of the recorded service.
     * 
     * @see org.ocap.dvr.RecordedService#getName
     */
    private String serviceName;

    /**
     * A platform-specific unique representation of the actual recroding in
     * persistent storage.
     */
    private String recordingId;

    /**
     * The reason for this recordin's failure
     */
    private int failedExceptionReason;

    /**
     * Constructs a new instance with the given <i>uniqueId</i>. It is the
     * responsibility of the caller to manage the uniqueIds (and make sure that
     * they are, in fact, unique). This constructor should not be used directly,
     * but a factory method instead.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordingInfo</code>
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordingInfo(long uniqueId)
    {
        super(uniqueId);
    }

    /**
     * Constructs a new instance with the given <i>uniqueId</i> initialized with
     * values from <i>rinfo</i>. It is the responsibility of the caller to
     * manage the uniqueIds (and make sure that they are, in fact, unique). This
     * constructor should not be used directly, but a factory method instead.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordingInfo</code>
     * @param rinfo
     *            the <code>RecordingInfo</code> initialize from
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordingInfo(long uniqueId, final RecordingInfo rinfo)
    {
        super(uniqueId, rinfo);

        this.startTime = rinfo.startTime;
        this.duration = rinfo.duration;
        this.actualStartTime = rinfo.actualStartTime;
        this.mediaTime = rinfo.mediaTime;
        this.serviceName = rinfo.serviceName;
        this.recordingId = rinfo.recordingId;
        this.deliverySystemType = rinfo.deliverySystemType;
        this.serviceName = rinfo.serviceName;
        this.serviceInformationType = rinfo.serviceInformationType;
        this.serviceDescription = rinfo.serviceDescription;
        this.longName = rinfo.longName;
        this.isFree = rinfo.isFree;
        this.deletionTime = rinfo.deletionTime;
        this.deletionReason = rinfo.deletionReason;
        this.failedExceptionReason = rinfo.failedExceptionReason;

        if (rinfo.serviceDescriptionUpdateTime != null)
        {
            this.serviceDescriptionUpdateTime = new Date(rinfo.serviceDescriptionUpdateTime.getTime());
        }

        if (rinfo.caSystemIds != null)
        {
            this.caSystemIds = new int[rinfo.caSystemIds.length];
            System.arraycopy(rinfo.caSystemIds, 0, this.caSystemIds, 0, rinfo.caSystemIds.length);
        }

        if (rinfo.serviceLocator != null)
        { // Need to deep-copy the locators
            this.serviceLocator = new OcapLocator[rinfo.serviceLocator.length];

            for (int i = 0; i < this.serviceLocator.length; i++)
            {
                try
                {
                    this.serviceLocator[i] = new OcapLocator(rinfo.serviceLocator[i].toExternalForm());
                }
                catch (org.davic.net.InvalidLocatorException e)
                {
                    this.serviceLocator[i] = null;
                }
            } // END for (each serviceLocators in rinfo)
        }

        if (rinfo.serviceComponents != null)
        { // Need to deep-copy the service components
            this.serviceComponents = new RecordedServiceComponentInfo[rinfo.serviceComponents.length];

            for (int i = 0; i < this.serviceComponents.length; i++)
            {
                // Create a copy of each RecordedServiceComponentInfo
                this.serviceComponents[i] = new RecordedServiceComponentInfo(rinfo.serviceComponents[i]);
            } // END for (each service component in rinfo)
        }
    } // END RecordingInfo.RecordingInfo(long uniqueId, final RecordingInfo
      // rinfo)

    /**
     * Overrides {@link Object#toString}. This is mostly in-place for
     * testing/debugging.
     * 
     * @return String representation of this object
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("RecordingInfo 0x")
                .append(Long.toHexString(this.hashCode()))
                .append(":[recID ")
                .append((recordingId == null) ? "null" : recordingId)
                .append(",start ")
                .append(startTime)
                .append(",dur ")
                .append(duration)
                .append(",loc ")
                .append(toString(serviceLocator))
                .append(",started ")
                .append(actualStartTime)
                .append(",mtime ")
                .append(mediaTime)
                .append(',')
                .append(super.toString())
                .append(']');

        return sb.toString();
    }

    /**
     * @see #toString()
     */
    private StringBuffer toString(OcapLocator[] loc)
    {
        if (loc == null) return new StringBuffer("null");

        StringBuffer sb = new StringBuffer("{");
        for (int i = 0; i < loc.length; ++i)
        {
            if (loc[i] == null)
                sb.append("null,");
            else
                sb.append(loc[i]);

            if (i + 1 < loc.length) sb.append(',');
        }
        sb.append('}');
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
     * @see #fap
     * @see #serviceLocator
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Write default fields
        out.defaultWriteObject();

        // Write out non-serializable/transient fields

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

        // OcapLocator
        out.writeBoolean(serviceLocator != null);
        if (serviceLocator != null)
        {
            out.writeInt(serviceLocator.length);
            for (int i = 0; i < serviceLocator.length; ++i)
            {
                out.writeBoolean(serviceLocator[i] != null);
                if (serviceLocator[i] != null) out.writeUTF(serviceLocator[i].toExternalForm());
            }
        }

        // deliverySystemType
        out.writeInt(getDeliverySystemTypeInt(deliverySystemType));

        // serviceInformationType
        out.writeInt(RecordedServiceComponentInfo.getServiceInformationTypeInt(serviceInformationType));

    }

    /**
     * Provide for de-serialization of non-serializable objects contained
     * within. This takes care of reading <i>transient</i> fields from the given
     * <code>ObjectInputStream</code>.
     * <p>
     * This method is implemented for serialization only, not for general usage.
     * 
     * @param in
     * 
     * @see #fap
     * @see #serviceLocator
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // Read default fields
        in.defaultReadObject();

        // Read in non-seriallizable/transient fields

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

            fap = new ExtendedFileAccessPermissions((hash & WORLD_R) != 0, (hash & WORLD_W) != 0, (hash & ORG_R) != 0,
                    (hash & ORG_W) != 0, (hash & APP_R) != 0, (hash & APP_W) != 0, readOid, writeOid);
        }

        // OcapLocator
        if (in.readBoolean())
        {
            int locatorCount = in.readInt();
            serviceLocator = new OcapLocator[locatorCount];
            for (int i = 0; i < locatorCount; ++i)
            {
                if (in.readBoolean())
                {
                    String loc = in.readUTF();
                    try
                    {
                        serviceLocator[i] = new OcapLocator(loc);
                    }
                    catch (org.davic.net.InvalidLocatorException e)
                    {
                        throw new IOException("Invalid locator in RecordingInfo: " + loc);
                    }
                }
            }
        }

        // deliverySystemType
        int deliveryTypeInt = in.readInt();
        this.deliverySystemType = getDeliverySystemType(deliveryTypeInt);

        // serviceInformationType
        int siTypeInt = in.readInt();
        this.serviceInformationType = RecordedServiceComponentInfo.getServiceInformationType(siTypeInt);

        // Check to see if the bitrate stored in the superclass is
        // zero. If it is, then update the fields of the super class
        // the current values read by this subclass.
        if (super.getBitRate() == 0)
        {
            convertUpdateSuper();
        }
    }

    void convertUpdateSuper()
    {
        super.setBitRate(this.bitRate);
        super.setExpirationDate(this.expirationDate);
        super.setFap(this.fap);
        super.setPriority(this.priority);
        super.setRetentionPriority(this.retentionPriority);
        super.setOrganization(this.organization);
    }

    /**
     * Package private method to translate <code>int</code>s to
     * <code>DeliverySystemType</code>s.
     * 
     * @param type
     * @return DeliverySystemType
     */
    static DeliverySystemType getDeliverySystemType(int type)
    {
        switch (type)
        {
            case 1:
                return DeliverySystemType.CABLE;
            case 2:
                return DeliverySystemType.SATELLITE;
            case 3:
                return DeliverySystemType.TERRESTRIAL;
            case 4:
                return DeliverySystemType.UNKNOWN;
            default:
                throw new IllegalArgumentException("Unknown int code: [" + type + "] for deliverySystemType.");
        }
    }

    /**
     * Package private method to translate <code>deliverySystemType</code>s to
     * <code>int</code>s.
     * 
     * @param type
     * @return
     */
    static int getDeliverySystemTypeInt(DeliverySystemType type)
    {
        if (type.equals(DeliverySystemType.CABLE))
            return 1;
        else if (type.equals(DeliverySystemType.SATELLITE))
            return 2;
        else if (type.equals(DeliverySystemType.TERRESTRIAL))
            return 3;
        else if (type.equals(DeliverySystemType.UNKNOWN))
            return 4;
        else
            throw new IllegalArgumentException("Unknown DeliverySystemType... "
                    + " did the JavaTV API add a new one as this method "
                    + "supports all DeliverySystemTypes as of 6-21-05");
    }

    private static final int WORLD_R = 0x20;

    private static final int WORLD_W = 0x10;

    private static final int ORG_R = 0x08;

    private static final int ORG_W = 0x04;

    private static final int APP_R = 0x02;

    private static final int APP_W = 0x01;

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.tv.service.navigation.ServiceDescription#getServiceDescription()
     */
    public String getServiceDescription()
    {
        return this.serviceDescription;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.service.SIRetrievable#getUpdateTime()
     */
    public Date getUpdateTime()
    {
        return this.serviceDescriptionUpdateTime;
    }

    /**
     * @return the LongName field for the ServiceDetails this object represents.
     */
    public String getLongName()
    {
        return longName;
    }

    /**
     * @return Returns the deliverySystemType for the ServiceDetails this object
     *         represents.
     */
    public DeliverySystemType getDeliverySystemType()
    {
        // TODO: (brian) should this and ServiceInformationType be set to
        // something other than the Actual SI
        // value since they are recorded?
        return deliverySystemType;
    }

    /**
     * @return Returns the serviceInformationType.
     */
    public ServiceInformationType getServiceInformationType()
    {
        // TODO: (brian) should this and DeliverySystemType be set to something
        // other than the Actual SI
        // value since they are recorded?
        return serviceInformationType;
    }

    /**
     * @return Returns the caSystemIds.
     */
    public int[] getCaSystemIds()
    {
        if (caSystemIds == null) return new int[0];
        return caSystemIds;
    }

    /**
     * @return Returns the isFree.
     */
    public boolean isFree()
    {
        // TODO: (brian) default is ?
        return isFree;
    }

    /**
     * Gets the value of expirationDate
     * 
     * @return the value of expirationDate
     */
    public Date getExpirationDate()
    {
        return super.getExpirationDate();
    }

    /**
     * Sets the value of expirationDate
     * 
     * @param argExpirationDate
     *            Value to assign to this.expirationDate
     */
    public void setExpirationDate(Date argExpirationDate)
    {
        super.setExpirationDate(argExpirationDate);
    }

    /**
     * Gets the value of organization
     * 
     * @return the value of organization
     */
    public String getOrganization()
    {
        return super.getOrganization();
    }

    /**
     * Sets the value of organization
     * 
     * @param argOrganization
     *            Value to assign to this.organization
     */
    public void setOrganization(String argOrganization)
    {
        super.setOrganization(argOrganization);
    }

    /**
     * Gets the value of fap
     * 
     * @return the value of fap
     */
    public ExtendedFileAccessPermissions getFap()
    {
        return super.getFap();
    }

    /**
     * Sets the value of fap
     * 
     * @param argFap
     *            Value to assign to this.fap
     */
    public void setFap(ExtendedFileAccessPermissions argFap)
    {
        super.setFap(argFap);
    }

    /**
     * Gets the value of priority
     * 
     * @return the value of priority
     */
    public byte getPriority()
    {
        return super.getPriority();
    }

    /**
     * Sets the value of priority
     * 
     * @param argPriority
     *            Value to assign to this.priority
     */
    public void setPriority(byte argPriority)
    {
        super.setPriority(argPriority);
    }

    /**
     * Gets the value of retention priority
     * 
     * @return the value of retention priority
     */
    public int getRetentionPriority()
    {
        return super.getRetentionPriority();
    }

    /**
     * Sets the value of retention priority
     * 
     * @param argRetentionPriority
     *            Value to assign to this.retentionPriority
     */
    public void setRetentionPriority(int argRetentionPriority)
    {
        super.setRetentionPriority(argRetentionPriority);
    }

    /**
     * Gets the value of bitRate
     * 
     * @return the value of bitRate
     */
    public byte getBitRate()
    {
        return super.getBitRate();
    }

    /**
     * Sets the value of bitRate
     * 
     * @param argBitRate
     *            Value to assign to this.bitRate
     */
    public void setBitRate(byte argBitRate)
    {
        super.setBitRate(argBitRate);
    }

    /**
     * Gets the value of duration
     * 
     * @return the value of duration
     */
    public long getDuration()
    {
        return this.duration;
    }

    /**
     * Sets the value of duration
     * 
     * @param argDuration
     *            Value to assign to this.duration
     */
    public void setDuration(long argDuration)
    {
        this.duration = argDuration;
    }

    /**
     * Gets the value of startTime
     * 
     * @return the value of startTime
     */
    public long getStartTime()
    {
        return this.startTime;
    }

    /**
     * Sets the value of startTime
     * 
     * @param argStartTime
     *            Value to assign to this.startTime
     */
    public void setStartTime(long argStartTime)
    {
        this.startTime = argStartTime;
    }

    /**
     * Sets the value of serviceDescription
     * 
     * @param argServiceDescription
     *            Value to assign to this.serviceDescription
     */
    public void setServiceDescription(String argServiceDescription)
    {
        this.serviceDescription = argServiceDescription;
    }

    /**
     * Gets the value of serviceDescriptionUpdateTime
     * 
     * @return the value of serviceDescriptionUpdateTime
     */
    public Date getServiceDescriptionUpdateTime()
    {
        return this.serviceDescriptionUpdateTime;
    }

    /**
     * Sets the value of serviceDescriptionUpdateTime
     * 
     * @param argServiceDescriptionUpdateTime
     *            Value to assign to this.serviceDescriptionUpdateTime
     */
    public void setServiceDescriptionUpdateTime(Date argServiceDescriptionUpdateTime)
    {
        this.serviceDescriptionUpdateTime = argServiceDescriptionUpdateTime;
    }

    /**
     * Sets the value of longName
     * 
     * @param argLongName
     *            Value to assign to this.longName
     */
    public void setLongName(String argLongName)
    {
        this.longName = argLongName;
    }

    /**
     * Sets the value of deliverySystemType
     * 
     * @param argDeliverySystemType
     *            Value to assign to this.deliverySystemType
     */
    public void setDeliverySystemType(DeliverySystemType argDeliverySystemType)
    {
        this.deliverySystemType = argDeliverySystemType;
    }

    /**
     * Sets the value of serviceInformationType
     * 
     * @param argServiceInformationType
     *            Value to assign to this.serviceInformationType
     */
    public void setServiceInformationType(ServiceInformationType argServiceInformationType)
    {
        this.serviceInformationType = argServiceInformationType;
    }

    /**
     * Gets the value of isFree
     * 
     * @return the value of isFree
     */
    public boolean isIsFree()
    {
        return this.isFree;
    }

    /**
     * Sets the value of isFree
     * 
     * @param argIsFree
     *            Value to assign to this.isFree
     */
    public void setIsFree(boolean argIsFree)
    {
        this.isFree = argIsFree;
    }

    /**
     * Sets the value of caSystemIds
     * 
     * @param argCaSystemIds
     *            Value to assign to this.caSystemIds
     */
    public void setCaSystemIds(int[] argCaSystemIds)
    {
        this.caSystemIds = argCaSystemIds;
    }

    /**
     * Gets the value of actualStartTime
     * 
     * @return the value of actualStartTime
     */
    public long getActualStartTime()
    {
        return this.actualStartTime;
    }

    /**
     * Sets the value of actualStartTime
     * 
     * @param argActualStartTime
     *            Value to assign to this.actualStartTime
     */
    public void setActualStartTime(long argActualStartTime)
    {
        this.actualStartTime = argActualStartTime;
    }

    /**
     * Gets the value of deletionTime
     * 
     * @return the value of deletionTime
     */
    public long getDeletionTime()
    {
        return this.deletionTime;
    }

    /**
     * Sets the value of deletionTime
     * 
     * @param argDeletionTime
     *            Value to assign to this.deletionTime
     */
    public void setDeletionTime(long argDeletionTime)
    {
        this.deletionTime = argDeletionTime;
    }

    /**
     * Gets the value of deletionReason
     * 
     * @return the value of deletionReason
     */
    public int getDeletionReason()
    {
        return this.deletionReason;
    }

    /**
     * Sets the value of deletionReason
     * 
     * @param argDeletionReason
     *            Value to assign to this.deletionReason
     */
    public void setDeletionReason(int argDeletionReason)
    {
        this.deletionReason = argDeletionReason;
    }

    /**
     * Gets the value of failedExceptionReason
     * 
     * @return the value of failedExceptionReason
     */
    public int getFailedExceptionReason()
    {
        return this.failedExceptionReason;
    }

    /**
     * Sets the value of failedExceptionReason
     * 
     * @param argFailedExceptionReason
     *            Value to assign to this.failedExceptionReason
     */
    public void setFailedExceptionReason(int argFailedExceptionReason)
    {
        this.failedExceptionReason = argFailedExceptionReason;
    }

    /**
     * Gets the value of mediaTime
     * 
     * @return the value of mediaTime
     */
    public long getMediaTime()
    {
        return this.mediaTime;
    }

    /**
     * Sets the value of mediaTime
     * 
     * @param argMediaTime
     *            Value to assign to this.mediaTime
     */
    public void setMediaTime(long argMediaTime)
    {
        this.mediaTime = argMediaTime;
    }

    /**
     * Gets the value of serviceLocator
     * 
     * @return the value of serviceLocator
     */
    public OcapLocator[] getServiceLocator()
    {
        return this.serviceLocator;
    }

    /**
     * Sets the value of serviceLocator
     * 
     * @param argServiceLocator
     *            Value to assign to this.serviceLocator
     */
    public void setServiceLocator(OcapLocator[] argServiceLocator)
    {
        this.serviceLocator = argServiceLocator;
    }

    /**
     * Gets the value of serviceName
     * 
     * @return the value of serviceName
     */
    public String getServiceName()
    {
        return this.serviceName;
    }

    /**
     * Sets the value of serviceName
     * 
     * @param argServiceName
     *            Value to assign to this.serviceName
     */
    public void setServiceName(String argServiceName)
    {
        this.serviceName = argServiceName;
    }

    /**
     * Gets the value of recordingId
     * 
     * @return the value of recordingId
     */
    public String getRecordingId()
    {
        return this.recordingId;
    }

    /**
     * Sets the value of recordingId
     * 
     * @param argRecordingId
     *            Value to assign to this.recordingId
     */
    public void setRecordingId(String argRecordingId)
    {
        this.recordingId = argRecordingId;
    }
}
