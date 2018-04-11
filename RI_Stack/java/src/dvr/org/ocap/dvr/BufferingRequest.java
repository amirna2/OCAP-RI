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

import javax.tv.service.Service;

import org.dvb.application.AppID;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.storage.ExtendedFileAccessPermissions;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.recording.RecordingManagerImpl;
import org.cablelabs.impl.manager.recording.RecordingManagerInterface;

/**
 * This class represents an application request for buffering. An application
 * can call the <code>createInstance</code> method to create a request.
 */
public abstract class BufferingRequest
{
    static RecordingManagerInterface recordingManager;

    /**
     * Protected constructor, not to be used by applications.
     */
    protected BufferingRequest()
    {
    }

    /**
     * Creates a BufferingRequest object.
     * 
     * @param service
     *            The service to buffer.
     * @param minDuration
     *            Minimum duration in seconds to buffer.
     * @param maxDuration
     *            Maximum duration in seconds to buffer.
     * @param efap
     *            Extended file access permissions for this request. If this
     *            parameter is null, no write permissions are given to this
     *            request. Read permissions for <code>BufferingRequest</code>
     *            instances are always world regardless of read permissions set
     *            by this parameter.
     * @throws IllegalArgumentException
     *             if the service parameter is not a valid <code>Service</code>,
     *             or if <code>minDuration</code> is less than
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}, or
     *             if <code>maxDuration</code> is less than
     *             <code>minDuration</code>.
     */
    public static BufferingRequest createInstance(Service service, long minDuration, long maxDuration,
            ExtendedFileAccessPermissions efap)
    {
        synchronized (BufferingRequest.class)
        {
            if (recordingManager == null)
            {
                recordingManager = (RecordingManagerInterface) ((org.cablelabs.impl.manager.RecordingManager) ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class)).getRecordingManager();
            }
        }

        return recordingManager.createBufferingRequest(service, minDuration, maxDuration, efap);
    }

    /**
     * Gets the Service this request is attempting to buffer.
     * 
     * @return Service being bufferred for this request.
     */
    public abstract Service getService();

    /**
     * Sets the Service this request is attempting to buffer.
     * 
     * @param service
     *            The <code>Service</code> to buffer for this request.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is not a valid <code>Service</code>.
     * @throws SecurityException
     *             if the calling applications does not have one of the write
     *             ExtendedFileAccessPermissions set by the
     *             <code>createInstance</code> or
     *             <code>setExtendedFileAccessPermissions</code> methods.
     */
    public abstract void setService(Service service) throws IllegalArgumentException;

    /**
     * Gets the minimum content buffering duration for this request.
     * 
     * @return The minimum content buffering duration in seconds.
     */
    public abstract long getMinimumDuration();

    /**
     * Sets the minimum duration of content that SHALL be buffered for this
     * request. If this method necessitates a buffer re-size the implementation
     * MAY flush the contents of the buffer.
     * 
     * @param minDuration
     *            Minimum duration in seconds.
     * 
     * @throws IllegalArgumentException
     *             If the parameter is greater than the current value and Host
     *             device does not have enough space to meet the request, or if
     *             the parameter is greater than the maximum duration set by the
     *             <code>createInstance</code> or
     *             <code>setMaximumDuration</code> methods, or if the parameter
     *             is less than the duration returned by
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}.
     * 
     * @throws SecurityException
     *             if the calling application does not have one of the write
     *             ExtendedFileAccessPermissions set by the
     *             <code>createInstance</code> or
     *             <code>setExtendedFileAccessPermissions</code> methods.
     */
    public abstract void setMinimumDuration(long minDuration);

    /**
     * Gets the maximum duration to buffer for this request. Returns the value
     * set by the <code>createInstance</code> or <code>setMaximumDuration</code>
     * methods.
     * 
     * @return Maximum duration in seconds.
     */
    public abstract long getMaxDuration();

    /**
     * Sets the maximum duration of content that MAY be buffered for this
     * <code>BufferingRequest</code>. Informs the implementation that storing
     * more content than this is not needed by the application owning this
     * <code>BufferingRequest</code>.
     * 
     * @param duration
     *            The maximum duration in seconds.
     * 
     * @throws IllegalArgumentException
     *             if the duration parameter is negative or if the parameter is
     *             less than the minimum duration set by the
     *             <code>createInstance</code> or
     *             <code>setMaximumDuration</code> methods,or if the parameter
     *             is less than the duration returned by
     *             {@link OcapRecordingManager#getSmallestTimeShiftDuration}..
     * @throws SecurityException
     *             if the calling application does not have one of the write
     *             ExtendedFileAccessPermissions set by the
     *             <code>createInstance</code> or
     *             <code>setExtendedFileAccessPermissions</code> methods.
     */
    public abstract void setMaxDuration(long duration);

    /**
     * Gets the ExtendedFileAccessPermissions for this request.
     * 
     * @return The ExtendedFileAccessPermissions.
     */
    public abstract ExtendedFileAccessPermissions getExtendedFileAccessPermissions();

    /**
     * Sets the ExtendedFileAccessPermissions for this request.
     * 
     * @param efap
     *            The ExtendedFileAccessPermissions for this request.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is null;
     * @throws SecurityException
     *             if the calling application is not the creator of this
     *             request.
     */
    public abstract void setExtendedFileAccessPermissions(ExtendedFileAccessPermissions efap);

    /**
     * Gets the AppID of the application that created the request. If null is
     * returned the implementation created the request.
     * 
     * @return AppID of the owning application.
     */
    public abstract AppID getAppID();
}
