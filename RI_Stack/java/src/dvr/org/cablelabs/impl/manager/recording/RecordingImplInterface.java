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

package org.cablelabs.impl.manager.recording;

import java.util.Vector;

import org.cablelabs.impl.manager.RecordingExt.RecordingUpdateListener;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventChangeListener;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.davic.net.tuning.NetworkInterface;
import org.dvb.application.AppID;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.SegmentedRecordedService;

public interface RecordingImplInterface
{
    /**
     * Event code to indicate that this recording has run out of disk space
     * TODO: integrate Java event codes w/ MPE level definitions
     */
    static final int MPE_DVR_EVT_OUT_OF_SPACE = 0x1000;

    static final int MPE_DVR_EVT_CONVERSION_STOP = 0x1003;

    static final int MPE_DVR_EVT_PLAYBACK_PID_CHANGE = 0x1004;

    static final int MPE_DVR_EVT_SESSION_CLOSED = 0x1005;

    /**
     * Native DVR error codes
     */
    static final int MPE_DVR_ERR_NOERR = 0x00; // no error

    static final int MPE_DVR_ERR_INVALID_PID = 0x01; // invalid pid error

    static final int MPE_DVR_ERR_INVALID_PARAM = 0x02; // a parameter is invalid

    static final int MPE_DVR_ERR_OS_FAILURE = 0x03; // error occured at the
                                                    // OSlevel

    static final int MPE_DVR_ERR_PATH_ENGINE = 0x04; // operation is notm
                                                     // supported

    static final int MPE_DVR_ERR_UNSUPPORTED = 0x05; // operation is notm
                                                     // supported

    static final int MPE_DVR_ERR_NOT_ALLOWED = 0x06; // operation is not alowed

    static final int MPE_DVR_ERR_DEVICE_ERR = 0x07; // hardware device error

    static final int MPE_DVR_ERR_OUT_OF_SPACE = 0x08; // no more space on the
                                                      // HDD

    static final int MPE_DVR_ERR_NOT_IMPLEMENTED = 0x09; // no more space on the
                                                         // HDD

    static final int MPE_DVR_ERR_NO_ACTIVE_SESSSION = 0x0a; // no more space on
                                                            // the HDD

    // This priority should be defined elsewhere!!
    // Used while registering for PAT/PMT changes
    static final int RECORDING_PRIORITY = 10;

    /**
     * Add application specific private data. If the key is already in use, the
     * data corresponding to key is overwritten.
     * 
     * @param key
     *            the ID under which the data is to be added
     * @param data
     *            the data to be added
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     * 
     * @throws IllegalArgumentException
     *             if the size of the data is more than the size supported by
     *             the implementation. The implementation shall support at least
     *             256 bytes of data.
     * @throws NoMoreDataEntriesException
     *             if the recording request is unable to store any more
     *             Application data. The implementation shall support atleast 16
     *             data entries per recording request.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public void addAppData(String key, java.io.Serializable data) throws NoMoreDataEntriesException,
            AccessDeniedException;

    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte);

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.manager.lightweighttrigger.
     * LightweightTriggerEventStoreWrite
     * #addLightweightTriggerEvents(org.cablelabs.impl.util.TimeTable)
     */
    public void addLightweightTriggerEvents(Vector v);

    /**
     * Adds a listener to receive notification upon recording state changes and
     * disables.
     * 
     * @param listener
     */
    public void addRecordingUpdateListener(RecordingUpdateListener listener);

    /**
     * Checks that the caller has write ExtendedFileAccessPermissions.
     * 
     * @throws AccessDeniedException
     *             if the caller does not have read access.
     */
    public void checkWriteExtFileAccPerm() throws AccessDeniedException;

    public boolean checkStored(LightweightTriggerEvent lwte, RecordedSegmentInfo info);

    /**
     * Called by RecordingImpl and RecordedServiceImpl to dispose of the
     * recording/native resources.
     * 
     * @param notifyRTM
     *            Notify the retention manager that the disk space has changed.
     *            Should be set to false if this deletion originated in the
     *            retention manager, in which case it already knows.
     */
    /**
     * @param notifyRTM
     */
    public void deleteRecordedServiceData(boolean notifyRTM);

    /**
     * called by our implementation to trigger this recording's expiration
     */
    public void expire();

    /**
     * Retrieves the recordings AlarmSpec object.
     * 
     * @return The AlarmSpec object for this recording.
     */
    public Object getAlarmSpec();

    /**
     * Gets the application identifier of the application that owns this
     * recording request. The owner of a root recording request is the
     * application that called the RecordingManager.record(..) method. The owner
     * of a non-root recording request is the owner of the root for the
     * recording request.
     * 
     * @return Application identifier of the owning application.
     */
    public AppID getAppID();

    public int getArtificialCarouselID();

    /**
     * Gets the duration requested for the recording.
     * 
     * @return The duration of the recording in milli-seconds.
     */
    public long getDuration();

    public LightweightTriggerEvent getEventByName(RecordedSegmentInfo info, String name);

    public String[] getEventNames(RecordedSegmentInfo info);

    public Object getExpirSpec();

    /**
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getId()
     */
    public int getId();

    /**
     * retrieve the internal state of this recording impl
     */
    public int getInternalState();

    /**
     * retrieves the locator of the service to be recorded for this recording.
     * If the recording is based on ServiceContext, null will be returned
     * 
     * @return the locator corresponding to the service to be selected
     */
    public OcapLocator[] getLocator();

    /**
     * Returns the log prefix
     */
    public String getLogPrefix();

    /**
     * get the media playback time for a recorded service
     */
    public long getMediaTime();

    /**
     * returns the native name corresponding to this recording
     */
    public String getName();

    /**
     * Retrieve the <code>NetworkInterface</code>
     * 
     * @return ni the NetworkInterface
     */
    public NetworkInterface getNetworkInterface();

    /**
     * @see org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#getPlayers()
     */
    public Vector getPlayers();

    /**
     * 
     * @return returns the request priority
     *         (RECORD_WITH_CONFLICTS/RECORD_IF_NO_CONFLICTS)
     */
    public byte getPriority();

    /**
     * Gets the duration actually recorded for the recording.
     * 
     * @return The duration of the recording in milli-seconds.
     */
    public long getRecordedDuration();

    /**
     * Returns the RecordingInfo object that contains the metadata about this
     * recording.
     */
    public RecordingInfo2 getRecordingInfo();

    /**
     * Returns the RecordingSpec corresponding to the recording request. This
     * will be either the source as specified in the call to the record(..)
     * method which caused this recording request to be created or the
     * RecordingSpec generated by the system during the resolution of the
     * original application specified RecordingSpec. Any modification to the
     * RecordingSpec due to any later calls to the SetRecordingProperties
     * methods on this instance will be reflected on the returned RecordingSpec.
     * <p>
     * When the implementation generates a recording request while resolving
     * another recording request, a new instance of the RecordingSpec is created
     * with an identical copy of the RecordingProperties of the parent recording
     * request.
     * 
     * @return a RecordingSpec containing information about this recording
     *         request.
     */
    public RecordingSpec getRecordingSpec();

    /**
     * 
     * @return returns the start time for this recording.
     */
    public long getRequestedStartTime();

    /**
     * Retrieve the ResourceUsage associated with the recording
     * 
     * @return The RecordingResourceUsage for the recording
     */
    public org.ocap.dvr.RecordingResourceUsage getResourceUsage();

    /**
     * Returns the saved state of the recording request.
     * 
     * @return Saved state of the recording request.
     */
    public int getSavedState();

    /**
     * Returns the {@link SegmentedRecordedService} corresponding to the
     * recording request.
     * 
     * @return The recorded service associated with the recording request.
     * @throws IllegalStateException
     *             if the recording request is not in INCOMPLETE_STATE,
     *             IN_PROGRESS_STATE, IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
     *             IN_PROGRESS_WITH_ERROR_STATE, IN_PROGRESS_INCOMPLETE_STATE,
     *             or COMPLETED_STATE.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public SegmentedRecordedService getService() throws IllegalStateException, AccessDeniedException;

    /**
     * Gets the estimated space, in bytes, required for the recording.
     * 
     * @return Space required for the recording in bytes. This method returns
     *         zero if the recordings is in failed state.
     */
    public long getSpaceRequired();

    /**
     * Returns the state of the recording request.
     * 
     * @return State of the recording request.
     */
    public int getState();

    /**
     * Find the MediaStorageVolume that a recording is on. Get it from the
     * recording if it's there, otherwise, get the default.
     * 
     * @return The MediaStorageVolume this recording is on.
     */
    public MediaStorageVolume getVolume();

    /**
     * Get a new TimeShiftWindowClient from the TimeShiftWindow associated with
     * the RecordingRequest (if any). Active RecordingRequests (those in the
     * IN_PROGRESS or IN_PROGRESS_INCOMPLETE states) should generally have a
     * TimeShiftWindowClient.
     * 
     * @return A TimeShiftWindowClient on the RecordingImpl's TimeShiftWindow or
     *         null if the RecordingImpl is not actively recording
     * @param reserveFor
     *            the reservation bitmask
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     */
    public TimeShiftWindowClient getNewTSWClient( int reserveFor, 
                                                  TimeShiftWindowChangedListener tswcl );

    /**
     * Complete initialization of the RecordingImpl according to the set state
     * and persist the recording.
     * 
     * This MUST be called prior to passing off the RecordingImpl reference.
     * 
     */
    public void initializeForState();

    /**
     * returns true if this recording is currently being presented
     */
    public boolean isPresenting();

    /**
     * Returns true of the RecordingImpl has 1 or more playable RecordedServices
     * 
     * @return true if the RecordingImpl has 1 or more playable RecordedService or false
     *         if it has no RecordedServices
     */
    public boolean hasPlayableRecordedService();

    /**
     * Returns whether the destined <code>MediaStorageVolume</code> for this
     * recording is present and ready or not. This method SHALL return
     * <code>true</code> under the following conditions:
     * <ul>
     * <li>
     * <code>getRecordingProperties().getDestination().getStatus() == READY</code>
     * <li> <code>getRecordingProperties().getDestination()</code> returns null
     * and a default recording volume exists for which
     * <code>getStatus() == READY</code>
     * </ul>
     * Otherwise, <code>false</code> SHALL be returned.
     * 
     * @return <code>true</code> if the explicit or an implicit destination
     *         volume is present and ready; <code>false</code> otherwise
     */
    public boolean isStorageReady();

    /**
     * Notify listeners if the current state is different from saved state.
     * 
     * @return false if state didn't change or true if state changed (and
     *         clients notified)
     */
    public boolean notifyIfStateChangedFromSaved();

    public long nGetRecordedDurationMS(String recordingName);

    public long nGetRecordedSize(String recordingName);

    public void registerChangeNotification(LightweightTriggerEventChangeListener listener);

    /**
     * Removes a recording change listener
     * 
     * @param listener
     */
    public void removeRecordingUpdateListener(RecordingUpdateListener listener);

    /**
     * Restore the current state of the recording request from the saved state.
     */
    public void restoreState();

    public void saveRecordingInfo(int updateFlag);

    /**
     * Set the AlarmSpec object.
     * 
     * @param spec
     *            the alarm spec.
     */
    public void setAlarmSpec(Object spec);

    public void setDeletionDetails(long deleteTime, int reason);

    public void setExpirSpec(Object expirSpec);

    /**
     * set the media playback time for a recorded service
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)*
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public void setMediaTime(long time) throws AccessDeniedException;

    public void setRootAndParentRecordingRequest(RecordingRequest root, RecordingRequest parent);

    /**
     * Sets the saved state of the recording request to the current state.
     */
    public void saveState();

    /**
     * Modify the RecordingProperties corresponding to the RecordingSpec for
     * this recording request. Applications may change any properties associated
     * with a recording request by calling this method. Changing the properties
     * may result in changes in the states of this recording request. Changing
     * the properties of a parent recording request will not automatically
     * change the properties of any of its child recording requests that are
     * already created. Any child recording requests created after the
     * invocation of this method will inherit the new values for the properties.
     * 
     * @param properties
     *            the new recording properties to set.
     * 
     * @throws IllegalStateException
     *             if changing one of the parameters that has been modified in
     *             the new recording properties is not legal for the current
     *             state of the recording request. For example, changing the
     *             duration of a completed recording request. The parameters
     *             that may be modified during each state of the recording
     *             request is defined in the specifcation text.
     * 
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
            AccessDeniedException;

    /**
     * Set the state of this recording. For all publicly visible state
     * transitions, this method should notify the NavigationManager that a state
     * transition has occurred.
     */
    public void setStateAndNotify(int state);

    /**
     * Set the state of this recording without notifying listeners that a state
     * transition has occurred.
     */
    public void setStateNoNotify(int state);

    /**
     * Set the in-progress state of this recording. This is only for use by 
     * RecordingRetentionManager and is only allowed to switch to/from
     * IN_PROGRESS states
     */
    public void setInProgressSpaceState(int state);
    
    /**
     * called by our implementation to start the native recording
     */
    public void startInternal();

    /**
     * called outside the class but is user induced
     */
    public void stopExternal();

    /**
     * called by our implementation to stop the native recording
     */
    public void stopInternal();

    public void unregisterChangeNotification(LightweightTriggerEventChangeListener listener);

    /**
     * Determines if the actual start time is outside of the start time
     * tolerance
     */
    public boolean verifyStartTimeWithinTolerance();

    /**
     * Set the external state of the recording to FAILED_STATE, COMPLETED_STATE,
     * or INCOMPLETE_STATE based on the amount of content physically recorded.
     */
    public void updateFinalStateForRecordedDuration();
}
