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

package org.cablelabs.impl.media.mpe;

import javax.media.GainControl;
import javax.media.Time;

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.util.PidMapTable;
import org.ocap.media.S3DConfiguration;

public interface DVRAPI
{
    public static final int ALARM_MEDIATIME_NANOS_NOT_SPECIFIED = -1;
    
    /**
     * Native event codes
     */
    public interface Event
    {
        //out of space: 0x1000
        public static final int END_OF_FILE = 0x1001, /*
                                                       * reached the end of the
                                                       * file
                                                       */
        START_OF_FILE = 0x1002, /* reached the start of file */
        //conversion stop: 0x1003
        PLAYBACK_PIDCHANGE = 0x1004, /*
                                      * PID change occurred at/near point of
                                      * presentation
                                      */
        
        SESSION_CLOSED = 0x1005, /* indicates a dvr session is complete */
        //session recording: 0x1006
        //session no data: 0x1007
        //cci update: 0x1008
        PLAYBACK_ALARM = 0x1009;
    }

    public class RecordingDeletedException extends Exception
    {
        RecordingDeletedException(String name)
        {
            super("recording " + ((name == null) ? "" : (name + " ")) + "deleted before playback started");
        }
    }

    /**
     * This represents the data returned from starting a playback session.
     */
    public static class Playback
    {
        public Playback(int playbackHandle, float playbackRate, float initialGain)
        {
            this.handle = playbackHandle;
            this.rate = playbackRate;
            this.initialGain = initialGain;
        }

        public final int handle;

        public final float rate;

        public final float initialGain;

        public String toString()
        {
            return "Playback {handle=" + MediaAPIImpl.handleToString(handle) + ", rate=" + rate + ", gain: " + initialGain + "}";
        }
    }

    /**
     * Start decoding a set of PIDs from a time-shift buffer. If a decoding
     * session is already in progress, the new set of PIDs must be in the same
     * service and share the same PCR_PID (clock sync PID).
     * 
     * @param listener
     *            The {@link EDListener} that should receive asynchronous events
     *            for the decode session.
     * @param vd
     *            The native handle of the video device to use for decoding.
     * @param tsb
     *            The native handle of the TSB from which to decode.
     * @param pidMapTable
     *            the pidMapTable containing program IDs and types of service
     *            components to be presented.
     * @param cci
     *            the CCI byte to apply
     * @param alarmMediaTime
     *            the alarm mediatime or -1
     * @param start
     *            The start media time (nanoseconds). Could be one of the
     *            {@link MediaTime} constants.
     * @param rate
     *            The initial playback rate, updated with actual rate
     * @param blocked
     *            The initial viewable state of the TSB playback
     * @param mute
     *            True if audio is muted
     * @param gain
     *            The audio gain
     * @return Returns a {@link Playback} object for the decoding session.
     */
    Playback decodeTSB(EDListener listener, int vd, int tsb, PidMapTable pidMapTable, byte cci, long alarmMediaTime, long start, float rate,
            boolean blocked, boolean mute, float gain);

    /**
     * Start decoding a set of PIDs from a recording. If a decoding session is
     * already in progress, the new set of PIDs must be in the same service and
     * share the same PCR_PID (clock sync PID).
     * 
     * @param listener
     *            The {@link EDListener} that should receive asynchronous events
     *            for the decode session.
     * @param vd
     *            The native handle of the video device to use for decoding.
     * @param recording
     *            The native name of the recording to decode.
     * @param pidMapTable
     *            the pidMapTable containing program IDs and types of service
     *            components to be presented.
     * @param cci
     *            the CCI byte to apply
     * @param alarmMediaTime
     *            the alarm mediatime or -1
     * @param start
     *            A start media time (nanoseconds). Could be one of the
     *            {@link MediaTime} constants.
     * @param rate
     *            The initial playback rate, updated with actual rate
     * @param mute
     *            True if audio is muted
     * @param gain
     *            The audio gain
     * @return Returns a {@link Playback} object for the decoding session.
     * @throws RecordingDeletedException
     *             If the specified recording has been deleted before playback
     *             could be started.
     */
    Playback decodeRecording(EDListener listener, int vd, String recording, PidMapTable pidMapTable, byte cci, long alarmMediaTime, long start,
                             float rate, boolean blocked, boolean mute, float gain) throws RecordingDeletedException;

    /**
     * Stop decoding session that was started by decodeTSB/Recording().
     * 
     * @param dvr native MPE DVR playback handle
     * @param holdFrame  display the last frame if true
     *
     */
    void stopDVRDecode(int dvr, boolean holdFrame);

    /**
     * Get the current playback rate.
     * 
     * @param dvr
     *            native MPE DVR playback handle
     * @return Returns the current rate.
     */
    float getRate(int dvr);

    /**
     * Set the playback rate for a decoding session.
     * 
     * @param dvr
     *            native MPE DVR playback handle
     * @param rate
     *            the requested rate
     * @return Returns the actual playback rate, which will be as close as
     *         possible to the requested <code>rate</code>.
     */
    float setRate(int dvr, float rate);

    /**
     * Set the playback position to the specified media time.
     * 
     * @param dvr
     *            native MPE DVR playback handle
     * @param time
     *            the requested media time (in nanoseconds)
     */
    void setMediaTime(int dvr, long time);

    /**
     * Get the current media time.
     * 
     * @param dvr
     *            native MPE DVR playback handle
     * @return Returns a {@link Time} representing the current media time.
     */
    Time getMediaTime(int dvr);

    /**
     * Set the blocking state of the presentation.
     * 
     * @param dvr
     *            native MPE DVR playback handle.
     * @param block
     *            <code>true</code> means block; <code>false</code> means not.
     */
    void blockPresentation(int dvr, boolean block);

    /**
     * For recordings, this function returns the media time of the closest
     * renderable frame to the given mediaTime in the given direction. It is
     * expected that calling mpeos_dvrPlaybackGetTime() after calling
     * mpeos_dvrPlaybackSetTime() with a value returned from this function will
     * result in the same value returned in frameTime.
     * 
     * @param name
     *            the native name of the recording
     * @param originalTime
     *            original time
     * @param direction
     *            before or on
     */
    Time getRecordingMediaTimeForFrame(String name, Time originalTime, int direction);

    /**
     * For a tsb, this function returns the media time of the closest renderable
     * frame to the given mediaTime in the given direction. It is expected that
     * calling mpeos_dvrPlaybackGetTime() after calling
     * mpeos_dvrPlaybackSetTime() with a value returned from this function will
     * result in the same value returned in frameTime.
     * 
     * @param tsbHandle
     *            the native name of the recording
     * @param originalTime
     *            original time
     * @param direction
     *            before or on
     */
    Time getTsbMediaTimeForFrame(int tsbHandle, Time originalTime, int direction);

    /**
     * 
     * @param direction
     * @return
     */
    boolean stepFrame(int dvr, int direction);

    /**
     * Update presenting pids for the current playback session
     */
    void playbackChangePids(int dvr, PidMapTable pidMapTable);

    /**
     * Mute or unmute the audio of a decode session
     *
     * @param sessionHandle the session
     * @param mute new mute state
     */
    void setMute(int sessionHandle, boolean mute);

    /**
     * Update the audio gain value as described by {@link GainControl#setDB}
     *
     * @param sessionHandle the session
     * @param gain new audio gain value in decibels
     */
    float setGain(int sessionHandle, float gain);

    /**
     * Get the input video scan mode for an ongoing decode
     * session.  Returns 
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @return Returns the input video scan mode: one of
     *                org.ocap.media.VideoFormatControl#SCANMODE_UNKNOWN,
     *                org.ocap.media.VideoFormatControl#SCANMODE_INTERLACED,
     *             or org.ocap.media.VideoFormatControl#SCANMODE_PROGRESSIVE.

     * @see org.ocap.media.VideoFormatControl#getDecoderFormatConversion()
     */
    int getInputVideoScanMode(int vd);

    /**
     * Returns the 3D configuration info of the video.
     * See [OCCEP] for the 3D formatting data definition.
     * Returns <code>null</code> if no 3D formatting data is present
     * (e.g., in the case of 2D video).  Note: Rapid changes in
     * 3D signaling may cause the returned S3DConfiguration object
     * to be stale as soon as this method completes.
     *
     *
     * @return The signaled 3D formatting data, or <code>null</code> if no
     * 3D formatting data is present.
     */
    S3DConfiguration getS3DConfiguration(int vd);

    /**
     * Set CCI for the decode session
     * @param sessionHandle
     * @param cci
     */
    void setCCI(int sessionHandle, byte cci);

    /**
     * Set an alarm mediatime for the decode session
     *
     * @param sessionHandle
     * @param alarmMediaTime a valid mediatime or -1 to unset
     */
    void setAlarm(int sessionHandle, long alarmMediaTime);

}
