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

package org.ocap.dvr;

import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Encapsulates the details about how a recording is to be made.  Used by the
 * implementation to create a parent or leaf recording request when the
 * <code>RecordingManager record</code> or <code>resolve</code>
 * methods are called.  The only attributes in this class that are used by a
 * <code>ParentRecordingRequest</code> are the access and organization
 * attributes.  All of the other attributes are not used by a parent recording request
 * <code>ParentRecordingRequest</code> for the life cycle of the request.
 * <p></p>
 * When the implementation creates a <code>ParentRecordingRequest</code> using
 * this class it SHALL set the <code>ExtendedFileAccessPermissions</code>
 * to read and write application access rights only.
 * </p><p>
 * For purposes of the <code>RecordingRequest.setRecordingProperties</code> method,
 * properties MAY be changed under the following state conditions:
 * <ul>
 * <li>bitRate - leaf recordings only, in the PENDING_NO_CONFLICT_STATE and
 * PENDING_WITH_CONFLICT_STATE</li>
 * <li>priorityFlag - leaf recordings only, in the PENDING_NO_CONFLICT_STATE and
 * PENDING_WITH_CONFLICT_STATE</li>
 * <li>retentionPriority - leaf recordings only, any state except DELETED_STATE and CANCELLED_STATE</li>
 * <li>access - leaf or parent recordings in any state</li>
 * <li>organization - cannot be changed in any state</li>
 * <li>destination - leaf recordings only, in the PENDING_NO_CONFLICT_STATE and
 * PENDING_WITH_CONFLICT_STATE</li>
 * <li>expirationPeriod - leaf recordings only, any state except DELETED_STATE and CANCELLED_STATE</li>
 * <li>resourcePriority - leaf recordings only, any state except DELETED_STATE, CANCELLED_STATE,
 * FAILED_STATE, COMPLETE_STATE, or INCOMPLETE_STATE</li>
 * </ul>
 */
public class OcapRecordingProperties extends RecordingProperties
{
    /**
     * Indicates an implementation specific value for high bit-rate.
     */
    public static final byte HIGH_BIT_RATE = 1;

    /**
     * Indicates an implementation specific value for low bit-rate.
     */
    public static final byte LOW_BIT_RATE = 2;

    /**
     * Indicates an implementation specific value for medium bit-rate.
     */
    public static final byte MEDIUM_BIT_RATE = 3;

    /**
     * Record only if there are no conflicts. When used as the priorityFlag to
     * the constructor for instances of this class, the recording request in
     * PENDING_NO_CONFLICT_STATE is scheduled only if there are no conflicts. If
     * there are conflicts the record method will create and schedule a
     * recording request in PENDING_WITH_CONFLICT_STATE.
     */
    public static final byte RECORD_IF_NO_CONFLICTS = 1;

    /**
     * Record even when resource conflicts exist. This value could be used as
     * the priorityFlag parameter value to the constructor for instances of this
     * class. When this is used as a priority value, if the request conflicts
     * with another RecordingRequest , resources are resolved using recording
     * resource conflict resolution rules.
     */
    public static final byte RECORD_WITH_CONFLICTS = 2;

    /**
     * Schedule only test recording requests corresponding to this spec.
     * Does not cause a recording to be started.
     * This value could be used as the priorityFlag parameter value to
     * the constructor for instances of this class.  When an
     * OcapRecordingProperties with this value used as a priority value
     * is used to schedule a recording request, any leaf recording
     * requests scheduled will be in the TEST_STATE.  If a test recording
     * request is unresolved, partially resolved or completely resolved,
     * the states would be UNRESOLVED_STATE, PARTIALLY_RESOLVED_STATE and
     * COMPLETELY_RESOLVED_STATE respectively. Test recording requests
     * maybe used by applications to detect potential conflicts before
     * scheduling a regular recording. Scheduling a test recording request
     * will not affect the states of any other recording requests. No
     * events will be generated corresponding to a test recording request.
     * Test recording requests will not change state to any other state.
     */
    public static final byte TEST_RECORDING = 3;

    /**
     * Indicates a recording SHALL be deleted by the implementation as soon as
     * its expiration date is reached.
     */
    public static final int DELETE_AT_EXPIRATION = 0;

    /**
     * Constructs an immutable instance of <code>OcapRecordingProperties</code>
     * with the specified attributes.
     * <p>
     * The unspecified {@link #getResourcePriority resourcePriority} attribute
     * is assigned a value of
     * {@link org.ocap.resource.ResourcePriority#UNKNOWN_PRIORITY
     * UNKNOWN_PRIORITY}.
     *
     * @param bitRate
     *            An application may specify LOW_BIT_RATE, MEDIUM_BIT_RATE, or
     *            HIGH_BIT_RATE. For analog recordings the corresponding
     *            bit-rate values are implementation specific. For digital
     *            recordings these values request optional transrating. When
     *            transrating is supported, HIGH_BIT_RATE indicates no
     *            transrating, and MEDIUM_BIT_RATE to LOW_BIT_RATE indicates
     *            increasing compression with a potential decrease in video
     *            quality.
     * @param expirationPeriod
     *            The period in seconds after the initiation of recording when
     *            leaf recording requests with this recording property are
     *            deemed as expired. The implementation will delete recorded
     *            services based on the expirationPeriod and retentionPriority
     *            parameters. This is done without application intervention and
     *            transitions those recording requests to the deleted state.
     * @param retentionPriority
     *            Indicates when the recording shall be deleted. An application
     *            MAY pass in DELETE_AT_EXPIRATION or a higher value indicating
     *            a retention priority. If the value is not DELETE_AT_EXPIRATION
     *            the recording will be kept after the expirationPeriod has
     *            passed if the implementation does not need the storage space
     *            for any other reason. If the space is needed expired
     *            recordings will be deleted based on retention priority, i.e.
     *            higher value equals higher priority, until the needed space is
     *            achieved.
     * @param priorityFlag
     *            Indication whether the recording should be made regardless of
     *            resource conflict or not. This parameter can contain the
     *            values RECORD_IF_NO_CONFLICTS, TEST_RECORDING or
     *            RECORD_WITH_CONFLICTS.
     * @param access
     *            File access permission for the recording request.
     * @param organization
     *            Name of the organization this recording will be tied to. Used
     *            to authenticate playback applications by matching this
     *            parameter to an organization name field in any playback
     *            application's certificate chain. Can be set to null to disable
     *            this playback application authentication.
     * @param destination
     *            The volume that represents the Storage location of the
     *            recording. When an instance of this class is used with a
     *            ServiceRecordingSpec a LocatorRecordingSpec, or a
     *            ServiceContextRecordingSpec where the specified service
     *            context is not attached to a time-shift buffer, with the value
     *            of this parameter set to null, the implementation shall use
     *            the default recording volume (see
     *            org.ocap.storage.MediaStorageOption ) in one of the storage
     *            devices connected. If the value is null when used with a
     *            ServiceContextRecordingSpec, when the service context
     *            specified in the ServiceContextRecordingSpec is attached to a
     *            time-shift buffer, the default recording volume from the
     *            storage device where the time-shift buffer is located shall be
     *            used. When an instance of this class is used with a
     *            ServiceContextRecordingSpec, the record(..) method will throw
     *            an IllegalArgumentException if the destination is not in same
     *            storage device where an attached time-shift buffer is located.
     * @throws java.lang.IllegalArgumentException
     *             if bitRate does not equal one of LOW_BIT_RATE,
     *             MEDIUM_BIT_RATE, or HIGH_BIT_RATE, or if priorityFlag does
     *             not contain the value RECORD_IF_NO_CONFLICTS, TEST_RECORDING
     *             or RECORD_WITH_CONFLICTS, or if organization is not found in
     *             the application's certificate file.
     */
    public OcapRecordingProperties(byte bitRate, long expirationPeriod, int retentionPriority, byte priorityFlag,
            ExtendedFileAccessPermissions access, String organization, MediaStorageVolume destination)
            throws IllegalArgumentException
    {
        super(expirationPeriod);

        if ((LOW_BIT_RATE == bitRate) || (MEDIUM_BIT_RATE == bitRate) || (HIGH_BIT_RATE == bitRate))
        {
            m_bitRate = bitRate;
        }
        else
        {
            throw new IllegalArgumentException("invalid bit-rate identifier: " + bitRate);
        }

        if ((RECORD_IF_NO_CONFLICTS == priorityFlag) || (TEST_RECORDING == priorityFlag)
                || (RECORD_WITH_CONFLICTS == priorityFlag))
        {
            m_priFlag = priorityFlag;
        }
        else
        {
            throw new IllegalArgumentException("invalid priority flag value: " + priorityFlag);
        }

        if (null == access)
        {
            access = new ExtendedFileAccessPermissions(false, false, false, false, true, true, new int[0], new int[0]);
        }

        m_fap = access;
        m_org = organization;
        m_dest = destination;
        m_retentionPriority = retentionPriority;
        m_resourcePriority = 0;
    }

    /**
     * Constructs an immutable instance of <code>OcapRecordingProperties</code>
     * with the specified attributes.
     *
     * @param bitRate
     *            An application may specify LOW_BIT_RATE, MEDIUM_BIT_RATE, or
     *            HIGH_BIT_RATE. For analog recordings the corresponding
     *            bit-rate values are implementation specific. For digital
     *            recordings these values request optional transrating. When
     *            transrating is supported, HIGH_BIT_RATE indicates no
     *            transrating, and MEDIUM_BIT_RATE to LOW_BIT_RATE indicates
     *            increasing compression with a potential decrease in video
     *            quality.
     * @param expirationPeriod
     *            The period in seconds after the initiation of recording when
     *            leaf recording requests with this recording property are
     *            deemed as expired. The implementation will delete recorded
     *            services based on the expirationPeriod and retentionPriority
     *            parameters. This is done without application intervention and
     *            transitions those recording requests to the deleted state.
     * @param retentionPriority
     *            Indicates when the recording shall be deleted. An application
     *            MAY pass in DELETE_AT_EXPIRATION or a higher value indicating
     *            a retention priority. If the value is not DELETE_AT_EXPIRATION
     *            the recording will be kept after the expirationPeriod has
     *            passed if the implementation does not need the storage space
     *            for any other reason. If the space is needed expired
     *            recordings will be deleted based on retention priority, i.e.
     *            higher value equals higher priority, until the needed space is
     *            achieved.
     * @param priorityFlag
     *            Indication whether the recording should be made regardless of
     *            resource conflict or not. This parameter can contain the
     *            values RECORD_IF_NO_CONFLICTS, TEST_RECORDING or
     *            RECORD_WITH_CONFLICTS.
     * @param access
     *            File access permission for the recording request.
     * @param organization
     *            Name of the organization this recording will be tied to. Used
     *            to authenticate playback applications by matching this
     *            parameter to an organization name field in any playback
     *            application's certificate chain. Can be set to null to disable
     *            this playback application authentication.
     * @param destination
     *            The volume that represents the Storage location of the
     *            recording. When an instance of this class is used with a
     *            ServiceRecordingSpec a LocatorRecordingSpec, or a
     *            ServiceContextRecordingSpec where the specified service
     *            context is not attached to a time-shift buffer, with the value
     *            of this parameter set to null, the implementation shall use
     *            the default recording volume (see
     *            org.ocap.storage.MediaStorageOption ) in one of the storage
     *            devices connected. If the value is null when used with a
     *            ServiceContextRecordingSpec, when the service context
     *            specified in the ServiceContextRecordingSpec is attached to a
     *            time-shift buffer, the default recording volume from the
     *            storage device where the time-shift buffer is located shall be
     *            used. When an instance of this class is used with a
     *            ServiceContextRecordingSpec, the record(..) method will throw
     *            an IllegalArgumentException if the destination is not in same
     *            storage device where an attached time-shift buffer is located.
     * @param resourcePriority
     *            Indicates the application-specified resource priority that
     *            should be used for any implied resource reservations. This
     *            value will be made available for consideration for resource
     *            contention resolution.
     *
     * @throws java.lang.IllegalArgumentException
     *             if bitRate does not equal one of LOW_BIT_RATE,
     *             MEDIUM_BIT_RATE, or HIGH_BIT_RATE; or if priorityFlag does
     *             not contain the value RECORD_IF_NO_CONFLICTS, TEST_RECORDING
     *             or RECORD_WITH_CONFLICTS; or if organization is not found in
     *             the application's certificate file; or if resourcePriority is
     *             not a valid resource priority as defined by the
     *             {@link org.ocap.resource.ResourcePriority} interface.
     */
    public OcapRecordingProperties(byte bitRate, long expirationPeriod, int retentionPriority, byte priorityFlag,
            ExtendedFileAccessPermissions access, String organization, MediaStorageVolume destination,
            int resourcePriority) throws IllegalArgumentException
    {
        super(expirationPeriod);
        if ((LOW_BIT_RATE == bitRate) || (MEDIUM_BIT_RATE == bitRate) || (HIGH_BIT_RATE == bitRate))
        {
            m_bitRate = bitRate;
        }
        else
        {
            throw new IllegalArgumentException("invalid bit-rate identifier: " + bitRate);
        }

        if ((RECORD_IF_NO_CONFLICTS == priorityFlag) || (TEST_RECORDING == priorityFlag)
                || (RECORD_WITH_CONFLICTS == priorityFlag))
        {
            m_priFlag = priorityFlag;
        }
        else
        {
            throw new IllegalArgumentException("invalid priority flag value: " + priorityFlag);
        }

        if (null == access)
        {
            access = new ExtendedFileAccessPermissions(false, false, false, false, true, true, new int[0], new int[0]);
        }

        m_fap = access;
        m_org = organization;
        m_dest = destination;
        m_retentionPriority = retentionPriority;
        m_resourcePriority = resourcePriority;
    }

    /**
     * Return the bitRate to use for the recording
     *
     * @return the bitRate as passed into the constructor
     */
    public byte getBitRate()
    {
        return m_bitRate;
    }

    /**
     * Gets the priority determining how the recording is deleted.
     *
     * @return the retention priority as passed into the constructor
     */
    public int getRetentionPriority()
    {
        return m_retentionPriority;
    }

    /**
     * Return whether or not the recording should be made if there are resource
     * conflicts
     *
     * @return the priority flag passed into the constructor
     */
    public byte getPriorityFlag()
    {
        return m_priFlag;
    }

    /**
     * Return the file access permission to use for the recording
     *
     * @return the file access permission passed into the constructor
     */
    public ExtendedFileAccessPermissions getAccessPermissions()
    {
        return m_fap;
    }

    /**
     * Return the name of the organization that this recording will be tied to
     *
     * @return the organization passed into the constructor
     */
    public String getOrganization()
    {
        return m_org;
    }

    /**
     * Gets the period in seconds the recording expires after being scheduled.
     *
     * @return the expiration period as passed into the constructor
     */
    public long getExpirationPeriod()
    {
        return super.getExpirationPeriod();
    }

    /**
     * Return the volume that represents the storage location of the recording
     *
     * @return the volume passed into the constructor
     */
    public MediaStorageVolume getDestination()
    {
        return m_dest;
    }

    /**
     * Return the application-specified resource priority that may be considered
     * at resource contention resolution time.
     *
     * @return the resource priority
     */
    public int getResourcePriority()
    {
        return m_resourcePriority;
    }

    // a reasonable default based on other source files
    private byte m_bitRate = HIGH_BIT_RATE;

    private MediaStorageVolume m_dest;

    private ExtendedFileAccessPermissions m_fap;

    private String m_org;

    private int m_retentionPriority;

    private int m_resourcePriority;

    private byte m_priFlag = RECORD_IF_NO_CONFLICTS;
}
