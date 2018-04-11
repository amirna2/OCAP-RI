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
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.DeliverySystemType;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;

/**
 * A central store of recording information that can be serialized.
 * Serialization need not be the method used to store
 * <code>RecordingInfo2</code> objects in persistent storage, but it <i>is</i>
 * supported if necessary. -- IMPORTANT -- You must manually keep this data in
 * sync with DVRUpgradeRecordingInfo, DVRUpgradeManager and
 * RecordingManagerImpl.addOrphanedRecording().
 * 
 * @see org.ocap.dvr.recording.LeafRecordingRequest
 * @see org.cablelabs.impl.recording.RecordingInfoTree
 * @see org.cablelabs.impl.manager.RecordingDBManager
 * 
 * @author Craig Pratt
 */
public class RecordingInfo2 extends RecordingInfoNode implements Serializable // LightweightTriggerEventStoreWrite
{
    private static final long serialVersionUID = 4525408774778243072L;

    // OcapRecordingProperties fields 
    
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
     * The resource priority field of this recording.
     * 
     */
    private int resourcePriority;

    /**
     * The desired recording bitRate for this recording.
     * 
     * @see org.ocap.dvr.RecordingSession#getRequestedBitRate
     */
    private byte bitRate;

    // Various RecordingSpec fields (LocatorRecordingSpec, PrivateRecordingSpec, ServiceContextRecordingSpec, ServiceRecordingSpec)
    
    public static final int SPECTYPE_UNKNOWN = 0;
    public static final int SPECTYPE_LOCATOR = 1;
    public static final int SPECTYPE_SERVICE = 2;
    public static final int SPECTYPE_SERVICECONTEXT = 3;
    
    /**
     * The type of RecordingSpec associated with the RecordingRequest
     * 
     * @see org.ocap.shared.dvr.RecordingSpec
     */
    private int requestType = SPECTYPE_UNKNOWN;

    /**
     * The desired recording duration.
     * 
     * @see org.ocap.dvr.RecordingSession#getRequestedDuration
     */
    private long requestedDuration;

    /**
     * The scheduled recording start time.
     * 
     * @see org.ocap.dvr.RecordingSession#getStartTime
     */
    private long requestedStartTime;

    /**
     * The locator(s) for the recording request (used for LocatorRecordingSpec/ServiceRecordingSpec)
     * 
     * @see org.ocap.shared.dvr.LocatorRecordingSpec
     * @see org.ocap.shared.dvr.ServiceRecordingSpec
     */
    private transient OcapLocator[] serviceLocators;
    
    public static final int SERVICENUMBER_UNDEFINED = -1;
    
    /**
     * The Service number for the recording request (used to uniquely re-bind the Service for the ServiceRecordingSpec)
     * 
     * @see org.ocap.shared.dvr.ServiceRecordingSpec
     */
    private int serviceNumber = SERVICENUMBER_UNDEFINED;

    // ServiceDescription fields.

    /**
     * The <code>String</code> that is the service description for this
     * RecordingInfo2. Used to satisfy the implementation of ServiceDescription.
     */
    private String serviceDescription;

    /**
     * The <code>Date</code> that is the update time for this recording info.
     * Used to satisfy the implementation of ServiceDescription.
     */
    private Date serviceDescriptionUpdateTime;

    // ServiceDetails fields
    private String longServiceName;

    // ServiceDetails.DeliverySystemType
    private transient DeliverySystemType serviceDeliverySystem = DeliverySystemType.UNKNOWN;

    // SIElement ServiceInformationType
    private transient ServiceInformationType serviceInformationType = ServiceInformationType.UNKNOWN;

    // ServiceDetails isFree
    private boolean isFree;

    // ServiceDetails caSystemIds
    private int[] caSystemIds;

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
     * The reason for this recording's failure
     */
    private int failedExceptionReason;

    /**
     * Time the SegmentedRecordedService is to initiate playback.
     * 
     * @see org.ocap.shared.dvr.RecordedService#setMediaTime()
     */
    private long mediaTime;

    /**
     * For access to the RecordedSegmentInfo containers
     */
    private Vector recordedSegmentList = new Vector();

    /**
     * Constructs an empty container with the given <i>uniqueId</i>. It is the
     * responsibility of the caller to manage the uniqueIds (and make sure that
     * they are, in fact, unique). This constructor should not be used directly,
     * but a factory method instead.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordedSegmentInfo</code>
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordingInfo2(long uniqueId)
    {
        super(uniqueId);
    }

    /**
     * Constructs a RecordingInfo2 container from a RecordingInfo.
     * 
     * This is supplied to convert any recordings persisted using the
     * (depricated) RecordingInfo into a RecordingInfo2.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordedSegmentInfo</code>
     * @param rinfo
     *            RecordingInfo to construct from
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordingInfo2(long uniqueId, final RecordingInfo rinfo)
    {
        // Copy base RecordingInfoNode fields, which are the same between
        // a RecordingInfo and RecordingInfo2
        super(uniqueId, rinfo);

        // Assert: Fields copied by RecordingInfoNode are:
        // fap, organization, appId, state, priority,
        // bitRate, and retentionPriority

        // Convert the remaining RecordingInfo fields

        this.bitRate = rinfo.getBitRate();
        this.requestedStartTime = rinfo.getStartTime();
        this.requestedDuration = rinfo.getDuration();
        this.serviceDeliverySystem = rinfo.getDeliverySystemType();
        this.serviceInformationType = rinfo.getServiceInformationType();
        this.serviceDescription = rinfo.getServiceDescription();
        this.longServiceName = rinfo.getLongName();
        this.isFree = rinfo.isFree();
        this.deletionTime = rinfo.getDeletionTime();
        this.deletionReason = rinfo.getDeletionReason();
        this.failedExceptionReason = rinfo.getFailedExceptionReason();
        this.mediaTime = rinfo.getMediaTime();

        final OcapLocator[] serviceLocator = rinfo.getServiceLocator();

        if (serviceLocator != null)
        { // Need to deep-copy the locators
            this.serviceLocators = new OcapLocator[serviceLocator.length];

            for (int i = 0; i < this.serviceLocators.length; i++)
            {
                try
                {
                    this.serviceLocators[i] = new OcapLocator(serviceLocator[i].toExternalForm());
                }
                catch (org.davic.net.InvalidLocatorException e)
                {
                    this.serviceLocators[i] = null;
                }
            } // END for (each serviceLocators in rinfo)
        }

        final Date serviceDescriptionUpdateTime = rinfo.getServiceDescriptionUpdateTime();

        if (serviceDescriptionUpdateTime != null)
        {
            this.serviceDescriptionUpdateTime = new Date(serviceDescriptionUpdateTime.getTime());
        }

        final int[] caSystemIds = rinfo.getCaSystemIds();

        if (caSystemIds != null)
        {
            this.caSystemIds = new int[caSystemIds.length];
            System.arraycopy(caSystemIds, 0, this.caSystemIds, 0, caSystemIds.length);
        }

        // See if there's a native recording name
        if (rinfo.getRecordingId() != null)
        {
            // A RecordingInfo can have only one recorded segment
            RecordedSegmentInfo rsi = new RecordedSegmentInfo(rinfo.getServiceName(), rinfo.getRecordingId(),
                    rinfo.getActualStartTime(), rinfo.getMediaTime());

            recordedSegmentList.add(rsi);
        }
    } // END RecordingInfo conversion constructor

    /**
     * Constructs a new instance with the given <i>uniqueId</i> initialized with
     * values from <i>rinfo</i>. It is the responsibility of the caller to
     * manage the uniqueIds (and make sure that they are, in fact, unique). This
     * constructor should not be used directly, but a factory method instead.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordingInfo2</code>
     * @param rinfo
     *            the <code>RecordingInfo2</code> initialize from
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordingInfo2(long uniqueId, final RecordingInfo2 rinfo)
    {
        super(uniqueId, rinfo);

        this.requestedStartTime = rinfo.requestedStartTime;
        this.requestedDuration = rinfo.requestedDuration;
        this.serviceDeliverySystem = rinfo.serviceDeliverySystem;
        this.serviceInformationType = rinfo.serviceInformationType;
        this.serviceDescription = rinfo.serviceDescription;
        this.longServiceName = rinfo.longServiceName;
        this.isFree = rinfo.isFree;
        this.deletionTime = rinfo.deletionTime;
        this.deletionReason = rinfo.deletionReason;
        this.failedExceptionReason = rinfo.failedExceptionReason;
        this.mediaTime = rinfo.mediaTime;
        this.resourcePriority = rinfo.resourcePriority;

        if (rinfo.serviceLocators != null)
        { // Need to deep-copy the locators
            this.serviceLocators = new OcapLocator[rinfo.serviceLocators.length];

            for (int i = 0; i < this.serviceLocators.length; i++)
            {
                try
                {
                    this.serviceLocators[i] = new OcapLocator(rinfo.serviceLocators[i].toExternalForm());
                }
                catch (org.davic.net.InvalidLocatorException e)
                {
                    this.serviceLocators[i] = null;
                }
            } // END for (each serviceLocators in rinfo)
        }

        if (rinfo.serviceDescriptionUpdateTime != null)
        {
            this.serviceDescriptionUpdateTime = new Date(rinfo.serviceDescriptionUpdateTime.getTime());
        }

        if (rinfo.caSystemIds != null)
        {
            this.caSystemIds = new int[rinfo.caSystemIds.length];
            System.arraycopy(rinfo.caSystemIds, 0, this.caSystemIds, 0, rinfo.caSystemIds.length);
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
        StringBuffer sbSegList = new StringBuffer();
        for (int i = 0; i < recordedSegmentList.size(); i++)
        {
            RecordedSegmentInfo info = (RecordedSegmentInfo) recordedSegmentList.elementAt(i);
            sbSegList.append("recordedSegmentList[" + i + "]=" + info + ", ");
        }
        if (recordedSegmentList.size() == 0)
        {
            sbSegList.append("no segments");
        }

        StringBuffer sb = new StringBuffer();
        sb.append("RecordingInfo2 0x")
                .append(Long.toHexString(this.hashCode()))
                .append(":[reqtype ")
                .append(requestType)
                .append(",loc ")
                .append(toString(serviceLocators))
                .append(",snum ")
                .append(serviceNumber)
                .append(",sdesc ")
                .append((serviceDescription == null) ? "null" : serviceDescription)
                .append(",start ")
                .append(requestedStartTime)
                .append(",dur ")
                .append(requestedDuration)
                .append(",dtime ")
                .append(deletionTime)
                .append(",drsn ")
                .append(deletionReason)
                .append(",fexp ")
                .append(failedExceptionReason)
                .append(",mtime ")
                .append(mediaTime)
                .append(",rsrcePrior ")
                .append(this.resourcePriority)
                .append(",nsegs ")
                .append(recordedSegmentList.size())
                .append(",segs ")
                .append(sbSegList)
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
                sb.append("null");
            else
                sb.append(loc[i]);

            if (i + 1 < loc.length) sb.append(',');
        }
        sb.append('}');
        return sb;
    }

    /**
     * Overrides {@link Object#equals}. This is mostly in-place for testing. In
     * general, the {@link #uniqueId} should be used to determine if two records
     * refer to the same recording.
     * 
     * @return <code>true</code> if all fields match, <code>false</code>
     *         otherwise
     */
    public boolean equals(Object obj)
    {
        if (obj == null || (getClass() != obj.getClass())) return false;
        if (!super.equals(obj)) return false;

        RecordingInfo2 o = (RecordingInfo2) obj;

        if (!((requestedDuration == o.requestedDuration) && (requestedStartTime == o.requestedStartTime)
                && (deletionReason == o.deletionReason) && (deletionTime == o.deletionTime)
                && (failedExceptionReason == o.failedExceptionReason) && (mediaTime == o.mediaTime) && (resourcePriority == o.resourcePriority)))
            return false;

        if (serviceLocators != o.serviceLocators) // same array or both null
        {
            if (serviceLocators == null || o.serviceLocators == null
                    || serviceLocators.length != o.serviceLocators.length) return false;

            for (int i = 0; i < serviceLocators.length; ++i)
            {
                if (serviceLocators[i] == null)
                {
                    if (o.serviceLocators[i] != null) return false;
                }
                else if (!serviceLocators[i].equals(o.serviceLocators[i]))
                {
                    return false;
                }
            }
        }

        return true;
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
     * @see #serviceLocators
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
        out.writeBoolean(serviceLocators != null);
        if (serviceLocators != null)
        {
            out.writeInt(serviceLocators.length);
            for (int i = 0; i < serviceLocators.length; ++i)
            {
                out.writeBoolean(serviceLocators[i] != null);
                if (serviceLocators[i] != null) out.writeUTF(serviceLocators[i].toExternalForm());
            }
        }

        // deliverySystemType
        out.writeInt(getServiceDeliverySystemInt(serviceDeliverySystem));

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
     * @see #serviceLocators
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
            serviceLocators = new OcapLocator[locatorCount];
            for (int i = 0; i < locatorCount; ++i)
            {
                if (in.readBoolean())
                {
                    String loc = in.readUTF();
                    try
                    {
                        serviceLocators[i] = new OcapLocator(loc);
                    }
                    catch (org.davic.net.InvalidLocatorException e)
                    {
                        throw new IOException("Invalid locator in RecordedSegmentInfo: " + loc);
                    }
                }
            }
        }

        // deliverySystemType
        int deliveryTypeInt = in.readInt();
        this.serviceDeliverySystem = getServiceDeliverySystem(deliveryTypeInt);

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
    static DeliverySystemType getServiceDeliverySystem(int type)
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
    static int getServiceDeliverySystemInt(DeliverySystemType type)
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
    public Date getServiceUpdateTime()
    {
        return this.serviceDescriptionUpdateTime;
    }

    /**
     * @return the LongName field for the ServiceDetails this object represents.
     */
    public String getLongServiceName()
    {
        return longServiceName;
    }

    /**
     * @return Returns the deliverySystemType for the ServiceDetails this object
     *         represents.
     */
    public DeliverySystemType getServiceDeliverySystem()
    {
        // TODO: (brian) should this and ServiceInformationType be set to
        // something other than the Actual SI
        // value since they are recorded?
        return serviceDeliverySystem;
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
     * @return the requestType
     */
    public int getRequestType()
    {
        return requestType;
    }

    /**
     * @param requestType the requestType to set
     */
    public void setRequestType(int requestType)
    {
        this.requestType = requestType;
    }

    /**
     * Gets the value of duration
     * 
     * @return the value of duration
     */
    public long getRequestedDuration()
    {
        return this.requestedDuration;
    }

    /**
     * Sets the value of duration
     * 
     * @param argDuration
     *            Value to assign to this.duration
     */
    public void setRequestedDuration(long argDuration)
    {
        this.requestedDuration = argDuration;
    }

    /**
     * Gets the value of startTime
     * 
     * @return the value of startTime (system time)
     */
    public long getRequestedStartTime()
    {
        return this.requestedStartTime;
    }

    /**
     * Sets the value of startTime
     * 
     * @param argStartTime
     *            System time value to assign to this.startTime
     */
    public void setRequestedStartTime(long argStartTime)
    {
        this.requestedStartTime = argStartTime;
    }

    /**
     * Gets the value of serviceLocator
     * 
     * @return the value of serviceLocator
     */
    public OcapLocator[] getServiceLocators()
    {
        return this.serviceLocators;
    }

    /**
     * Sets the value of serviceLocator
     * 
     * @param argServiceLocator
     *            Value to assign to this.serviceLocator
     */
    public void setServiceLocators(OcapLocator[] argServiceLocator)
    {
        this.serviceLocators = argServiceLocator;
    }

    /**
     * @return the serviceNumber
     */
    public int getServiceNumber()
    {
        return serviceNumber;
    }

    /**
     * @param serviceNumber the serviceNumber to set
     */
    public void setServiceNumber(int serviceNumber)
    {
        this.serviceNumber = serviceNumber;
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
     * Sets the value of longServiceName
     * 
     * @param argLongName
     *            Value to assign to this.longName
     */
    public void setLongServiceName(String argLongName)
    {
        this.longServiceName = argLongName;
    }

    /**
     * Sets the value of deliverySystemType
     * 
     * @param argDeliverySystemType
     *            Value to assign to this.deliverySystemType
     */
    public void setServiceDeliverySystem(DeliverySystemType argDeliverySystemType)
    {
        this.serviceDeliverySystem = argDeliverySystemType;
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
     * Gets the value of resource priority
     * 
     * @return the value of resource priority
     */
    public int getResourcePriority()
    {
        return this.resourcePriority;
    }

    /**
     * Sets the value of resource priority
     * 
     * @param argResourcePriority
     *            Value to assign to this.resourcePriority
     */
    public void setResourcePriority(int argResourcePriority)
    {
        this.resourcePriority = argResourcePriority;
    }

    /**
     * Retrieve a count of the RecordedSegmentInfo elements associated with this
     * RecordingInfo2.
     * 
     * @return number of RecordedSegmentInfo elements
     */
    public int getNumSegments()
    {
        return recordedSegmentList.size();
    }

    /**
     * Get an Enumeration over all of the RecordedSegmentInfo elements
     * associated with this RecordingInfo2.
     * 
     * @return Enumerator of associated RecordedSegmentInfo elements
     */
    public Enumeration getRecordedSegmentInfoElements()
    {
        return recordedSegmentList.elements();
    }

    /**
     * Adds the RecordedSegmentInfo to the end of the associated
     * RecordedSegmentInfo list, which is maintained in-order.
     * 
     * @param rsi
     *            RecordedSegmentInfo to add
     * @return The index of the segment added
     */
    public int addRecordedSegmentInfo(RecordedSegmentInfo rsi)
    {
        recordedSegmentList.add(rsi);
        return recordedSegmentList.size() - 1;
    }

    /**
     * Delete the RecordedSegmentInfo from the list of associated
     * RecordedSegmentInfo list
     * 
     * @param rsi
     *            RecordedSegmentInfo to delete
     */
    public void deleteRecordedSegmentInfo(RecordedSegmentInfo rsi)
    {
        recordedSegmentList.remove(rsi);
    }

    /**
     * Delete all associated RecordedSegmentInfo elements associated with the
     * RecordingInfo2
     */
    public void deleteRecordedSegmentInfoElements()
    {
        recordedSegmentList.removeAllElements();
    }

    /**
     * Get the first RecordedSegmentInfo in the list of associated
     * RecordedSegmentInfo elements.
     */
    public RecordedSegmentInfo getFirstRecordedSegment()
    {
        return (RecordedSegmentInfo) (recordedSegmentList.firstElement());
    }

    /**
     * Get the last RecordedSegmentInfo in the list of associated
     * RecordedSegmentInfo elements.
     */
    public RecordedSegmentInfo getLastRecordedSegment()
    {
        return (RecordedSegmentInfo) (recordedSegmentList.lastElement());
    }

    public RecordedSegmentInfo getNextToLastRecordedSegment()
    {
        return (RecordedSegmentInfo) (recordedSegmentList.elementAt(recordedSegmentList.size() - 2));
    }

    /*
     * (non-Javadoc)
     * 
     * modfies lwte.time
     * 
     * @seeorg.cablelabs.impl.manager.lightweighttrigger.
     * LightweightTriggerEventStoreWrite
     * #addLightweightTriggerEvent(org.cablelabs
     * .impl.manager.lightweighttrigger.LightweightTriggerEvent)
     */
    public void addLightweightTriggerEvent(final LightweightTriggerEvent lwte)
    {
        // work backwards through the list
        for (int i = recordedSegmentList.size() - 1; i >= 0; i--)
        {
            RecordedSegmentInfo segInfo = (RecordedSegmentInfo) recordedSegmentList.elementAt(i);
            if (lwte.getTimeMillis() >= segInfo.getActualStartTime())
            {
                // goes into this segment
                // now convert system time to media time from beginning of
                // segment. System time is in ms
                lwte.setTimeMillis(lwte.getTimeMillis() - segInfo.getActualStartTime());
                segInfo.addLightweightTriggerEvent(lwte);
                break;
            }
        }
    }

    /**
     * @param enumTt
     */
    private void addLightweightTriggerEvents(final Enumeration enumTt)
    {
        while (enumTt.hasMoreElements())
        {
            LightweightTriggerEvent elem = (LightweightTriggerEvent) enumTt.nextElement();
            addLightweightTriggerEvent(elem);
        }
    }

    public void addLightweightTriggerEvents(final Vector v)
    {
        Enumeration vEnum = v.elements();
        addLightweightTriggerEvents(vEnum);
    }

} // END class RecordingInfo2
