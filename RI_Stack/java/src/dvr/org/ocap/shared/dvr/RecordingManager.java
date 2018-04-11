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

package org.ocap.shared.dvr;

import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.recording.RecordingManagerImpl;

/**
 * RecordingManager represents the entity that performs recordings.
 */
public abstract class RecordingManager
{
    /**
     * Constructor for instances of this class. This constructor is provided for
     * the use of implementations and specifications which extend this
     * specification. Applications shall not define sub-classes of this class.
     * Implementations are not required to behave correctly if any such
     * application defined sub-classes are used.
     */
    protected RecordingManager()
    {
    }

    /**
     * Gets the list of entries maintained by the RecordingManager. This list
     * includes both parent and leaf recording requests. For applications with
     * RecordingPermission("read", "own"), only RecordingRequests of which the
     * calling application has visibility as defined by any RecordingRequest
     * specific security attributes will be returned. For applications with
     * RecordingPermission("read", "*"), all RecordingRequests will be returned.
     *
     * @return an instance of RecordingList
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public abstract RecordingList getEntries();

    /**
     * Gets the list of recording requests matching the specified filter. For
     * applications with RecordingPermission("read", "own"), only
     * RecordingRequests of which the calling application has visibility as
     * defined by any RecordingRequest specific security attributes will be
     * returned. For applications with RecordingPermission("read", "*"), all
     * RecordingRequests matching the specified filter will be returned.
     *
     * @param filter
     *            the filter to use on the total set of recording requests
     *
     * @return an instance of RecordingList
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public abstract RecordingList getEntries(RecordingListFilter filter);

    /**
     * Adds an event listener for changes in status of recording requests. For
     * applications with RecordingPermission("read", "own"), the listener
     * parameter will only be informed of changes that affect RecordingRequests
     * of which the calling application has visibility as defined by any
     * RecordingRequest specific security attributes. For applications with
     * RecordingPermission("read", "*"), the listener parameter will be informed
     * of all changes.
     *
     * @param rcl
     *            The listener to be registered.
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     */
    public abstract void addRecordingChangedListener(RecordingChangedListener rcl);

    /**
     * Removes a registed event listener for changes in status of recording
     * requests. If the listener specified is not registered then this method
     * has no effect.
     *
     * @param rcl
     *            the listener to be removed.
     */
    public abstract void removeRecordingChangedListener(RecordingChangedListener rcl);

    /**
     * Requests the recording of the stream or streams according to the source
     * parameter. The concrete sub-class of RecordingSpec may define additional
     * semantics to be applied when instances of that sub-class are used.
     *
     * @param source
     *            specification of stream or streams to be recorded and how they
     *            are to be recorded.
     *
     * @return an instance of RecordingRequest that represents the added
     *         recording.
     *
     * @throws IllegalArgumentException
     *             if the source is an application defined class or as defined
     *             in the concrete sub-class of RecordingSpec for instances of
     *             that class
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("create",..) or
     *             RecordingPermission("*",..)
     *
     */
    public abstract RecordingRequest record(RecordingSpec source) throws IllegalArgumentException,
            AccessDeniedException;

    /**
     * Gets the singleton instance of RecordingManager.
     *
     * @return an instance of RecordingManager
     */
    public static RecordingManager getInstance()
    {
        org.cablelabs.impl.manager.RecordingManager rm = (org.cablelabs.impl.manager.RecordingManager) ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class);
        RecordingManagerImpl rmi = (RecordingManagerImpl) rm.getRecordingManager();

        boolean waitCondition = false;

        // For applications, wait until the RecordingManager initialization has
        // completed
        // before returning from getInstance(). This is to avoid holding up all
        // of
        // ManagerManager initialization until recordings are loaded and
        // initialized
        // (which can be significant)
        try
        {
            waitCondition = rmi.waitForInitializationToComplete();
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("The wait for RecordingManager was interrupted before it could complete.");
        }

        if (waitCondition == false)
        {
            throw new IllegalStateException("RecordingManager initialization timed out.");
        }

        return rmi;
    } // END getInstance()

    /**
     * Look up a recording request from the identifier. Implementations of this
     * method should be optimised considering the likely very large number of
     * recording requests. For applications with RecordingPermission("read",
     * "own"), only RecordingRequests of which the calling application has
     * visibility as defined by any RecordingRequest specific security
     * attributes will be returned.
     *
     * @param id
     *            an identifier as returned by RecordingRequest.getId
     * @return the corresponding RecordingRequest
     * @throws IllegalArgumentException
     *             if there is no recording request corresponding to this
     *             identifier or if the recording request is not visible as
     *             defined by RecordingRequest specific security attributes
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     * @see RecordingRequest#getId
     */
    public abstract RecordingRequest getRecordingRequest(int id) throws IllegalArgumentException;
}
