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

import javax.tv.service.navigation.DeliverySystemType;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.util.TimeAssociatedElement;
import org.cablelabs.impl.util.TimeTable;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;

/**
 * This is a container class for everything about a RecordedSegment. --
 * IMPORTANT -- You must manually keep this data in sync with
 * DVRUpgradeRecordedSegmentInfo, DVRUpgradeManager and
 * RecordingManagerImpl.addOrphanedRecording().
 * 
 * @see org.ocap.dvr.recording.LeafRecordingRequest
 * @see org.cablelabs.impl.recording.RecordingInfoTree
 * @see org.cablelabs.impl.manager.RecordingDBManager
 * 
 * @author Craig Pratt
 */
public class RecordedSegmentInfo extends TimeAssociatedElement implements Serializable
{
    private static final long serialVersionUID = -8431980711128119296L;

    /**
     * The time this recording was actually started in system time
     * 
     * @see org.ocap.dvr.RecordingSession#getStartTime
     */
    private long actualStartTime;

    /**
     * Time the RecordedService is to initiate playback. This is the saved play
     * head position in units of media time
     * 
     * @see org.ocap.shared.dvr.RecordedService#setMediaTime()
     */
    private long mediaTime;

    /**
     * The name of the recorded service.
     * 
     * @see org.ocap.dvr.RecordedService#getName
     */
    private String serviceName;

    /**
     * A platform-specific unique representation of the actual recording in
     * persistent storage.
     */
    private String nativeRecordingName;

    /**
     * Container for the time-associated Service details
     * (TimeAssociatedDetailsInfo objects) associated with this
     * RecordedSegmentInfo
     */
    private TimeTable timeAssociatedDetails;

    /**
     * Container for LightweightTriggerEvents or other time based objects stored
     * in this segment.
     */
    private LightweightTriggerEventTimeTable lightweightTriggers;

    /**
     * Container for the time-associated CCI indications (CopyControlInfo 
     * objects) associated with this RecordedSegmentInfo
     */
    private TimeTable cciEvents;
    
    /**
     * Flag to designate that the segment is not copy-able (e.g. recording was 
     * made from content that was marked COPY_ONCE in the CCI/EMI bits)
     */
    private boolean isCopyProtected;
    
    /**
     * Constructor with no field initialization.
     * 
     */
    public RecordedSegmentInfo()
    {
        super(0);
    }

    /**
     * Constructor with basic field setter. Note: All parameters must be
     * non-mutable.
     */
    public RecordedSegmentInfo(final String serviceName, final String nativeRecordingName, final long actualStartTime,
            long mediaTime)
    {
        super(actualStartTime);
        this.serviceName = serviceName;
        this.actualStartTime = actualStartTime;
        this.mediaTime = mediaTime;
        this.nativeRecordingName = nativeRecordingName;
        this.lightweightTriggers = null;
        this.timeAssociatedDetails = new TimeTable();
        this.isCopyProtected = false;
        this.cciEvents = new TimeTable();
    }

    /**
     * Constructor with basic field setter. Note: All parameters must be
     * non-mutable.
     */
    public RecordedSegmentInfo(final String serviceName, final String nativeRecordingName, final long actualStartTime, // system
                                                                                                                       // time
            final long mediaTime, TimeTable detailsTimeTable, LightweightTriggerEventTimeTable ttc, final boolean copyProtected,
            final TimeTable cciTimeTable)
    {
        super(actualStartTime);
        this.serviceName = serviceName;
        this.actualStartTime = actualStartTime;
        this.mediaTime = mediaTime;
        this.nativeRecordingName = nativeRecordingName;
        this.timeAssociatedDetails = detailsTimeTable;
        this.lightweightTriggers = ttc;
        this.isCopyProtected = copyProtected;
        this.cciEvents = cciTimeTable;
    }

    /**
     * Constructs a new instance with the given <i>uniqueId</i> initialized with
     * values from <i>rinfo</i>. It is the responsibility of the caller to
     * manage the uniqueIds (and make sure that they are, in fact, unique). This
     * constructor should not be used directly, but a factory method instead.
     * Note: All parameters must be non-mutable.
     * 
     * @param uniqueId
     *            the uniqueId for this <code>RecordedSegmentInfo</code>
     * @param rinfo
     *            the <code>RecordedSegmentInfo</code> initialize from
     * 
     * @see org.cablelabs.impl.manager.RecordingDBManager
     */
    public RecordedSegmentInfo(final RecordedSegmentInfo rsinfo)
    {
        super(rsinfo.actualStartTime);
        this.actualStartTime = rsinfo.actualStartTime;
        this.mediaTime = rsinfo.mediaTime;
        this.serviceName = rsinfo.serviceName;
        this.nativeRecordingName = rsinfo.nativeRecordingName;
        this.lightweightTriggers = rsinfo.lightweightTriggers;
        this.timeAssociatedDetails = rsinfo.timeAssociatedDetails;
        this.isCopyProtected = rsinfo.isCopyProtected;
        this.cciEvents = rsinfo.cciEvents;
    } // END RecordedSegmentInfo.RecordedSegmentInfo(RecordedSegmentInfo rinfo)

    /**
     * Overrides {@link Object#toString}. This is mostly in-place for
     * testing/debugging.
     * 
     * @return String representation of this object
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("RecordedSegmentInfo@0x").append(Integer.toHexString(this.hashCode()))
          .append(":{rname ").append((nativeRecordingName == null) ? "null" : nativeRecordingName)
          .append(",svcname ").append((serviceName == null) ? "null" : serviceName)
          .append(",stime ").append(actualStartTime)
          .append(",mtime ").append(mediaTime)
          .append(",detailsTT ").append(timeAssociatedDetails)
          .append(",copyprotect ").append(isCopyProtected)
          .append(",CCI ").append((cciEvents == null ? "cci==null" : cciEvents.toString()))
          .append(",trigger ").append((lightweightTriggers == null ? "triggers==null" : lightweightTriggers.toString()))
          .append('}');

        return sb.toString();
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

        RecordedSegmentInfo o = (RecordedSegmentInfo) obj;

        if (!(actualStartTime == o.actualStartTime && mediaTime == o.mediaTime
                && ((serviceName == null) ? (o.serviceName == null) : (serviceName.equals(o.serviceName))) && ((nativeRecordingName == null) ? (o.nativeRecordingName == null)
                : (nativeRecordingName.equals(o.nativeRecordingName))))) return false;

        if (!timeAssociatedDetails.equals(o.timeAssociatedDetails)) return false;

        return true;
    }
    
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (int)(actualStartTime ^ (actualStartTime >>> 32));
        hash = 31 * hash + (int)(mediaTime ^ (mediaTime >>> 32));
        hash = 31 * hash + (serviceName == null ? 0 : serviceName.hashCode());
        hash = 31 * hash + (nativeRecordingName == null ? 0 : nativeRecordingName.hashCode());
        hash = 31 * hash + (timeAssociatedDetails == null ? 0 : timeAssociatedDetails.hashCode());
        return hash;
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

        // deal with RecordedSegmentInfo serialized before
        // LightweightTriggerEvents were
        // added
        if (lightweightTriggers == null)
        {
            lightweightTriggers = new LightweightTriggerEventTimeTable();
        }
        // Read in non-seriallizable/transient fields
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
    public String getNativeRecordingName()
    {
        return this.nativeRecordingName;
    }

    /**
     * Sets the value of recordingId
     * 
     * @param argRecordingId
     *            Value to assign to this.recordingId
     */
    public void setNativeRecordingName(String argNativeRecordingName)
    {
        this.nativeRecordingName = argNativeRecordingName;
    }

    /**
     * Get the initial service components for this RecordedSegmentInfo.
     * 
     * @return Enumerator over the set of initial service components
     */
    public TimeTable getTimeAssociatedDetails()
    {
        return timeAssociatedDetails;
    }

    /**
     * Get the service components for this RecordedSegmentInfo.
     * 
     * @return Vector containing the service components available at mediaTime
     */
    public TimeAssociatedDetailsInfo getDetailsForTime(long mediaTime)
    {
        return (TimeAssociatedDetailsInfo) timeAssociatedDetails.getEntryBefore(mediaTime + 1);
    }

    public LightweightTriggerEventTimeTable getLightweightTriggerEventTimeTable()
    {
        return lightweightTriggers;
    }

    public void addLightweightTriggerEvent(LightweightTriggerEvent lwte)
    {
        getLightweightTriggerEventTimeTable().addLightweightTriggerEvent(lwte);
    }

    public void addLightweightTriggerEvents(TimeTable tt)
    {
        getLightweightTriggerEventTimeTable().addLightweightTriggerEvents(tt);
    }

    /**
     * Get the TimeTable for CCI indications associated with the RecordedSegment
     * 
     * @return TimeTable for CCI indications associated with the RecordedSegment
     */
    public TimeTable getCCITimeTable()
    {
        return cciEvents;
    }
    
    /**
     * Returns whether the segment is considered "copy protected". (e.g. recording was 
     * made from content that was marked COPY_ONCE in the CCI/EMI bits)
     * 
     * @return true if the segment is considered "copy protected" and false otherwise
     */
    public boolean isCopyProtected()
    {
        return this.isCopyProtected;
    }

    /**
     * Sets the copy protected flag. Should be set if/when a segment is marked
     * "copy protected" (e.g. signaling marked the content COPY_ONCE in the CCI/EMI bits)
     * 
     * @param copyProtected True to mark the segment "Copy Protected" and false to 
     * indicate the segment is "Copyable"
     */
    public void setCopyProtected(boolean copyProtected)
    {
        this.isCopyProtected = copyProtected;
    }
    
    public long getTimeMillis()
    {
        return time;
    }

    public void setTimeMillis(final long timeMS)
    {
        time = timeMS;
    }
    
    /**
     *  Provide a copy of this LightweightTriggerEvent.
     */
    public Object clone() throws CloneNotSupportedException
    {
        return new RecordedSegmentInfo(this);
    }
} // END class RecordedSegmentInfo
