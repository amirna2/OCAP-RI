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

package org.cablelabs.lib.utils.oad.hndvr;

////////////////////////////////////////////////////////////////////////
/// OcapAppDriverInterfaceHNDVR
//
/// This interface defines the methods which provide the underlying driver-like 
/// functionality for RiScriptlet and RiExerciser (and potentially other applications) 
/// for HN Related functionality with DVR extension support.
/// 
/// This interface defines methods which require HN and DVR functionality.
/// These methods can only be used by OCAP Xlets when Ocap Stack supports both 
/// OCAP HN and DVR extensions.  
/// Methods which require support of OCAP HN and DVR extension should be added to 
/// this interface.  Methods which do not require support of both OCAP HN extension and DVR
/// extension should not be added to this interface, but should be added to 
/// OcapAppDriverInterfaceDVR or OcapAppDriverInterfaceHN instead.  
///
/// Methods defined in this interface should only have java primitive type parameters. 
/// There are no callbacks or events stemming from these methods.  Some APIs
/// have been added to "WaitFor" asynchrounous APIs to complete.  These types
/// of APIs should be added if there is desire to synchrounously wait for
/// given states, (see WaitForTuningState())...
//
public interface OcapAppDriverInterfaceHNDVR
{
    /**
     * Publish the given recording (by index) to the CDS, with an associated
     * NetRecordingEntry. Note that this will cause an IllegalArgumentException
     * if the RecordingContentItem has already been added to the CDS, either
     * through this method or through the publishRecordingWithoutNRE() method.
     * @param recordingIndex the index of the recording to be published
     * @param timeoutMS amount of time in milliseconds to complete HN action
     * @param publishToRoot indicates whether the recording should be published
     * to the root container or to a sub container
     * @return true if the recording is successfully published, false otherwise
     */
    boolean publishRecording(int recordingIndex, long timeoutMS, boolean publishToRoot);

    /**
     * Publish all recordings found to the CDS, with an associated
     * NetRecordingEntry for each RecordingContentItem published.  Note that 
     * this will cause an IllegalArgumentException if any of the 
     * RecordingContentItems to be published have already been added to the CDS,
     * either through this method or through the publishAllRecordingsWithoutNRE()
     * method.
     * @param timeoutMS amount of time in milliseconds to complete HN action
     * @param publishToRoot indicates whether the recordings should be published
     * to the root container or to a sub container
     * @return true if all recordings were successfully published, false otherwise
     */
    boolean publishAllRecordings(long timeoutMS, boolean publishToRoot);
    
    /**
     * Remove all recordings content items found in the CDS, including parent 
     * containers. This method also removes any NetReordingEntry objects published
     * to the CDS.
     * 
     * @return true if all recordings were successfully un-published/removed, false otherwise
     */
    boolean unPublishAllRecordings();
    
    /**
     * Access the specified local recording usage log
     * @param index the index of the local recording usage log
     * @return a String representation of the specified local recording usage log,
     * or null if no usage String is available
     */
    String getRecordingLogMessage(int index);
    
    /**
     * Access the number of CDS recording usage logs
     * @return the number of CDS recording usage logs
     */
    int getNumCDSRecordingLogUses();
    
    /**
     * Access the specified CDS recording usage log
     * @param index the index of the CDS recording usage log
     * @return a String representation of the specified CDS recording usage log,
     * or null if no usage String is available
     */
    String getCDSRecordingLogMessage(int index);
    
    /**
     * Adds messages concerning use of local recording item to a List
     */
    void logRecordingInUse();
    
    /**
     * Adds messages concerning use of CDS recording item to a List
     * @param timeoutMS amount of time in milliseconds to complete HN action
     */
    void logRecordingInUseViaCDS(long timeoutMS);
    
    /** 
     * Access the number of local recording item usage logs
     * @return the number of local recording item usage logs
     */
    int getNumRecordingLogUses();    
    
    /**
     * Creates a scheduled recording when a recording is requested.
     * 
     * @param index
     *            - TunerIndex to be used for recording
     * @param duration
     *            - Duration of the recording to be scheduled
     * @param delay
     *            - Delay in seconds
     * @param background
     *            - The recording mode to be used for the recording
     * @return true if a recording is scheduled properly, false otherwise.
     */
    boolean createScheduledRecording(int index, long duration, long delay, boolean background);
    
    /**
     * Returns a boolean if the recording was scheduled remotely or not.
     * 
     * @param recordingIndex
     *            The index to identify the recording.
     * @return true if a recording is scheduled remotely, false otherwise.
     */
    boolean isRemoteScheduledRecording(int recordingIndex);
}

