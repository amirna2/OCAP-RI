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

package org.cablelabs.lib.utils.oad.dvr;

import org.ocap.shared.dvr.LeafRecordingRequest;


////////////////////////////////////////////////////////////////////////
/// OcapAppDriverInterfaceDVR
//
/// This interface defines the methods which provide the underlying driver-like 
/// functionality for RiScriptlet and RiExerciser (and potentially other applications) 
/// for Recording, TSB and DVR Related functionality.
/// 
/// This interface defines methods which require DVR functionality and can only be used 
/// by OCAP Xlets when Ocap Stack supports the OCAP DVR extension.  Methods which
/// require support of OCAP DVR extension should be added to this interface.  
/// If a method does not require the DVR OCAP extension, it should not be added 
/// to this interface, but OcapAppInterfaceCore or to any other applicable OCAP 
/// extension interface, i.e. OcapAppDriverInterfaceHN, etc.
///
/// Methods defined in this interface should only have java primitive type parameters. 
/// There are no callbacks or events stemming from these methods.  Some APIs
/// have been added to "WaitFor" asynchrounous APIs to complete.  These types
/// of APIs should be added if there is desire to synchrounously wait for
/// given states, (see WaitForTuningState())...
//
public interface OcapAppDriverInterfaceDVR
{
    // Static final ints to be used for recording states in Rx scripts
    public static final int COMPLETED_STATE = LeafRecordingRequest.COMPLETED_STATE;
    public static final int DELETED_STATE = LeafRecordingRequest.DELETED_STATE;
    public static final int FAILED_STATE = LeafRecordingRequest.FAILED_STATE;
    public static final int IN_PROGRESS_INCOMPLETE_STATE = 
        LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE;
    public static final int IN_PROGRESS_INSUFFICIENT_SPACE_STATE = 
        LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE;
    public static final int IN_PROGRESS_STATE = LeafRecordingRequest.IN_PROGRESS_STATE;
    public static final int IN_PROGRESS_WITH_ERROR_STATE = 
        LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE;
    public static final int INCOMPLETE_STATE = LeafRecordingRequest.INCOMPLETE_STATE;
    public static final int PENDING_NO_CONFLICT_STATE = 
        LeafRecordingRequest.PENDING_NO_CONFLICT_STATE;
    public static final int PENDING_WITH_CONFLICT_STATE = 
        LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE;
    
    // Methods for initializing instances of OcapAppDriverInterfaceDVR
    void setNumTuners(int numTuners);
    
    
    ////////////////////////////////////////////////////////////////////////
    /// TSB
    //
    
    /**
     * Enable or disable the TSB for the given tuner (by index)
     * @param tunerIndex the index of the tuner
     * @param enabled true to enable TSB, false to disable TSB
     * @return  true
     */
    boolean tsbControl(boolean enable);
    
    /**
     * Return the enabled state of the TSB for the given tuner (by index)
     * @param tunerIndex the index of the tuner
     * @return true if TSB is enabled for the given tuner, false otherwise
     */
    boolean isTsbEnabled();
    
    /**
     * Returns whether DVR buffering is enabled or not
     * @return a boolean indicating whether DVR buffering is enabled
     */
    boolean isBufferingEnabled();
    
    /**
     *  Toggles between requesting a buffer and canceling a buffer request on the
     *  Service at the given index. If a buffer is requested, this method cancels
     *  the request. If a buffer is not requested this method requests buffering.
     *  The intent of this method is to be able to issue a buffering request on
     *  a Service other than the one currently selected.
     *  @param serviceIndex the index of the Service on which to issue a buffering
     *  request
     */
    void toggleBufferingRequest(int serviceIndex);
    
    /**
     *  Toggles DVR buffering between being enabled and disabled
     */
    void toggleBufferingEnabled();
    
    /**
     * Returns a formatted double indicating the buffer time of the indicated
     * player in seconds
     * 
     * @param startTime a boolean indicating whether the startTime should be 
     * retrieved (if true) of the end time should be retrieved (if false)
     * 
     * @return a formatted double indicating the buffer time in seconds, or 
     * Double.NaN if an error occurs
     */
    double getBufferTime(boolean startTime);
    
    /**
     * Return the amount of disk space available
     * @return the amount of available disk space in bytes
     */
    long getDiskSpace();

    /**
     * Return the boolean ability to create a file of the given size
     * @param fileSize the size of the File in bytes
     * @return true if there is enough free space in bytes to create the file
     * of the specified size, false otherwise
     */
    boolean checkDiskSpace(long fileSize);


    ////////////////////////////////////////////////////////////////////////
    /// Recording
    //

    /**
     * Record the Service on the given tuner for the given duration starting
     * after the given delay in the background if set.
     * @param tunerIndex the index of the tuner to record
     * @param duration the duration of the recording to make in seconds
     * @param delay the delay of the recording start time in seconds
     * @param background true to indicate a background recording, false to
     * indicate a recording that is not background
     * @return true if the recording is scheduled, else false
     */
    boolean recordTuner(int tunerIndex, long duration, long delay,
                               boolean background);

    /**
     * Record the given Service at tuner index 0 for the given duration starting
     * after the given delay in the background if set.
     * @param serviceIndex the index of the Service to be recorded
     * @param duration the duration of the recording in seconds
     * @param delay the delay of the recording start time in seconds
     * @param background true to indicate a background recording, false to 
     * indicate a recording that is not background
     * @return true if the recording is scheduled, else false
     */
    boolean recordService(int serviceIndex, long duration,
                                 long delay, boolean background);

    /**
     * Wait for the recording (provided tuner index) to reach the given state or
     * until the given timeout occurs.
     * @param tunerIndex the index of the tuner
     * @param timeout the time to wait for the state in seconds
     * @param recordingState the state to wait for
     * @return true if the state was attained, else false
     */
    boolean waitForRecordingState(int tunerIndex, long timeout,
                                         int recordingState);
    
    /**
     * Stop the recording on the given tuner
     * @param tunerIndex the index of the tuner to stop recording
     * @return true if successful, else false
     */
    boolean recordingStop(int tunerIndex);

    /**
     * Get the number of recordings
     * @return the number of available recordings or -1 on error
     */
    int getNumRecordings();

    /**
     * Returns a String containing information about the recording at the given
     * index
     * @param recordingIndex the index of the recording
     * @return the info for the recording at the provided recording index
     */
    String getRecordingInfo(int recordingIndex);

    /**
     * Return the recording's duration at the provided index
     * @param recordingIndex the index of the recording
     * @return the duration of the recording at the given index in seconds, or
     * -1 for error
     */
    long getRecordingDuration(int recordingIndex);

    /**
     * Return the current state of the recording on the provided tuner index
     * @param tunerIndex the index of the tuner
     * @return the current state of the recording at the given index
     */
    int getCurrentRecordingState(int tunerIndex);

    /**
     * Return the provided tuner index recording's duration
     * @param tunerIndex the index of the tuner
     * @return the duration of the current recording from the given tuner, or -1
     * for error
     */
    long getCurrentRecordingDuration(int tunerIndex);
    
    /** 
     * Deletes a recording indicated by the given index
     * @param recordingIndex the index of the recording to be deleted
     * @return true if the recording was successfully deleted, false otherwise
     */
    boolean deleteRecording(int recordingIndex);
    
    /**
     * Deletes all recordings currently contained in the OcapRecordingManager
     * @return true if all recordings were successfully deleted, false otherwise
     */
    boolean deleteAllRecordings();

    /**
     * Returns string representation of the supplied recording state.
     * 
     * @param state get string describing this state
     * @return  string describing supplied state
     */
    public String getRecStateStr(int state);


    ////////////////////////////////////////////////////////////////////////
    /// DVR Local Playback
    //

    /**
     * Creates and starts a DVR playback.  This method will not return until
     * playback is presenting or failure is encountered.
     * 
     * @param recordingIndex the index of the DVR recording for which to create
     * playback
     * @param waitTimeSecs  amount of time in seconds to wait for playback to start
     * 
     * @return true if playback was successfully started and is presenting, false otherwise.
     *         Returns false if playback was already active.
     */
    boolean playbackStart(int recordingIndex, int waitTimeSecs);

    /**
     * A helper method which will play the next recording index - can be called only after createDvrPlayback is initially called.
     * Will play index zero if the last recording index is currently playing.
     *
     * Playback is asynchronous - uses ServiceContext.select instead of createDvrPlayback to support fast tuning.
     */
    void playNext();

    /**
     * A helper method which will play the previous recording index - can be called only after createDvrPlayback is initially called.
     * Will play the last recording index if index zero is currently playing.
     *
     * Playback is asynchronous - uses ServiceContext.select instead of createDvrPlayback to support fast tuning.
     */
    void playPrevious();
}

