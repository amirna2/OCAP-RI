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

package org.ocap.dvr.storage;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageOption;

/**
 * This interface represents an option object provided by a
 * {@link org.ocap.storage.StorageProxy} that supports media volumes (
 * {@link MediaStorageVolume}) that are used by the DVR recording and playback
 * APIs for storing media content.
 * <p>
 * The interface distinguishes between content accessible through the DVR APIs
 * and as general purpose files. Implementations may store these different type
 * of content in one or more filesystems. This is transparent to an application.
 * Only the general purpose files are visible through the normal file and
 * directory classes in java.io.
 * <p>
 * The interface can be used to query the amount of storage the storage proxy
 * has for storing all types of application-visible content. (Some of the
 * capacity may be reserved for internal system use.)
 * <p>
 * The interface also supports the initialization of the storage proxy with a
 * specified allocation between the two types. However, on some implementations,
 * changing the allocations may require filesystems to be destroyed and
 * recreated which may result in the deletion of all application-visible content
 * associated with the storage proxy, including any storage volumes. On other
 * implementations, a change in allocations may require some or all content of
 * the type being reduced to be destroyed. Initialization should be done with
 * extreme caution.
 */
public interface MediaStorageOption extends StorageOption
{
    /**
     * Allocates a {@link MediaStorageVolume}. A media volume can contain
     * multi-media content that may impose I/O bandwidth criteria upon the
     * storage device. The new volume will be owned by the application that
     * allocated it.
     * 
     * @param name
     *            Name of the new MediaStorageVolume.
     * @param fap
     *            Access permissions of the new MediaStorageVolume.
     * 
     * @return Allocated volume storage.
     * 
     * @throws IllegalArgumentException
     *             if the name does not meet Java 1.1.8 directory naming
     *             conventions, or if the type is not supported by the storage
     *             device.
     * @throws SecurityException
     *             if the calling application is unsigned.
     */
    public MediaStorageVolume allocateMediaVolume(String name, ExtendedFileAccessPermissions fap)
            throws IllegalArgumentException;

    /**
     * Gets the default volume that the implementation setup as the default
     * recording volume for the containing {@link org.ocap.storage.StorageProxy}
     * .
     * 
     * @return Default recording volume for the storage device.
     */
    public MediaStorageVolume getDefaultRecordingVolume();

    /**
     * Gets the playback bandwidth in bits-per-second when only one playback
     * stream and no record streams are open on the entire storage device.
     * 
     * @return Playback bandwidth in bits-per-second.
     */
    public long getPlaybackBandwidth();

    /**
     * Gets the record bandwidth in bits-per-second when only one record stream
     * and no playback streams are open on the entire storage device.
     * 
     * @return Record bandwith in bits-per-second.
     */
    public long getRecordBandwidth();

    /**
     * Gets the total capacity of the MEDIAFS available for application use in
     * the storage device.
     * 
     * @return Total audio/video capacity of the storage device.
     */
    public long getTotalMediaStorageCapacity();

    /**
     * Gets total allocatable media storage available for all MediaStorageVolume
     * instances.
     * 
     * @return Size of allocatable media storage in bytes.
     */
    public long getAllocatableMediaStorage();

    /**
     * Gets the total capacity of the GPFS available for application use in the
     * storage device.
     * 
     * @return Total general purpose capacity of the storage device.
     */
    public long getTotalGeneralStorageCapacity();

    /**
     * Initializes the storage device so that the there are at least mediafsSize
     * bytes available for MEDIAFS use. The effects of initialization may
     * include the deletion of all application visible content associated with
     * the storage proxy. Calling this method may remove application access to
     * storage on the device for the duration of the call. It may cause the
     * abnormal termination of applications with open files associated with the
     * storage proxy. This method will block until the storage proxy is again
     * ready for use.
     * 
     * @param mediafsSize
     *            New size of the total MEDIAFS capacity in bytes.
     * 
     * @throws IllegalArgumentException
     *             if the mediafsSize passed is greater than the sum of what is
     *             returned by getTotalGeneralStorageCapacity() and
     *             getTotalMediaStorageCapacity().
     * @throws IllegalStateException
     *             if the sizes cannot be changed by the implementation for any
     *             reason.
     */
    public void initialize(long mediafsSize) throws IllegalArgumentException, IllegalStateException;

    /**
     * Indicates if the storage device supports simultaneous play and record.
     * 
     * @return True if simultaneous play and record is supported, otherwise
     *         returns false.
     */
    public boolean simultaneousPlayAndRecord();
}
