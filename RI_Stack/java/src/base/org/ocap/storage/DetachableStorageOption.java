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

package org.ocap.storage;

import java.io.IOException;

/**
 * This interface represents an external device that can be detached. The
 * methods on this interface allow a detachable device to be detached safely. In
 * addition, when a detachable storage device is attached for the first time,
 * its StorageProxy provides a means to initialize the device. If initialization
 * is needed, the StorageProxy will be in one of two states:
 * {@link StorageProxy#UNSUPPORTED_FORMAT UNSUPPORTED_FORMAT} or
 * {@link StorageProxy#UNINITIALIZED UNINITIALIZED}. When the StorageProxy is in
 * one of these two states, the initialize method must be called before the
 * device can be used.
 * 
 **/

public interface DetachableStorageOption extends StorageOption
{
    /**
     * Determines whether the device associated with this storage proxy is ready
     * to be detached.
     * 
     * @return Returns true when the device is currently ready to be detached,
     *         otherwise returns false.
     * 
     **/
    public boolean isDetachable();

    /**
     * Makes the device safe to be detached. Calling this method has extensive
     * impact on applications that may currently be using the associated storage
     * device.
     * <ol>
     * <li>
     * Any in progress java.io operations that are not completed throw
     * IOExceptions.</li>
     * <li>
     * The corresponding storage proxy is either removed from the database or
     * remains with a status of {@link StorageProxy#OFFLINE OFFLINE}. The latter
     * indicates that the device may be brought back online. If it is removed
     * from the database, attempts to use the storage proxy results in an
     * IOException.
     * </ol>
     * This call may block until the filesystem can be put into a consistent
     * state.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("storage").
     * 
     * @throws IOException
     *             if the system is unable to make the device safe to detach.
     **/
    public void makeDetachable() throws IOException;

    /**
     * Makes the device ready for use. If a detachable device is connected and
     * in the {@link StorageProxy#OFFLINE OFFLINE} state, this method attempts
     * to activate the device and make it available. For example, a device may
     * be left in an OFFLINE state after it has been made ready to detach, but
     * not actually unplugged. This method has no effect if the device is
     * already in the {@link StorageProxy#READY READY} state.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("storage").
     * 
     * @throws IOException
     *             if the device was not in the READY or OFFLINE state when the
     *             method was called.
     **/
    public void makeReady() throws IOException;
}
