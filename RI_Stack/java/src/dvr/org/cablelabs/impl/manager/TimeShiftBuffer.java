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

package org.cablelabs.impl.manager;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.util.PidMapTable;
import org.cablelabs.impl.util.TimeTable;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;

/**
 * Note: This interface represents elements of the TSB that should be visible
 * to components outside of TimeShiftManager. TimeShiftManager will operate
 * on the impl or a private interface.
 * 
 * @author Craig Pratt
 */
public interface TimeShiftBuffer // extends LightweightTriggerEventStoreWrite
{
    /**
     * Get the start time for the TimeShiftBuffer content in system time. System
     * time is measured in milliseconds between midnight January 1, 1970 UTC and
     * the start of the content in the TSB (this will result in a call to native/MPE)
     * 
     * @return The system start time of the TSB content, in milliseconds.
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or any other error.
     */
    public abstract long getContentStartTimeInSystemTime() throws IllegalStateException;

    /**
     * Get the end time for the TimeShiftBuffer content in system time. System
     * time is measured in milliseconds between midnight January 1, 1970 UTC and
     * the end of the content in the TSB (this will result in a call to native/MPE)
     * 
     * @return The system end time of the TSB content, in milliseconds.
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or any other error.
     */
    public abstract long getContentEndTimeInSystemTime() throws IllegalStateException;

    /**
     * Get the time buffering was started according to TimeBase time (nanoseconds)
     * 
     * @return The start time of the TSB, in nanoseconds.
     * 
     * @throws IllegalStateException
     *             If buffering hasn't been started.
     */
    public abstract long getTimeBaseStartTime() throws IllegalStateException;

    /**
     * Get the time buffering was stopped or Long.MAX_VALUE if buffering is ongoing (nanoseconds)
     * 
     * @return The TimeBase end time of the TSB, in nanoseconds or Long.MAX_VALUE if the
     *         TSB is still buffering.
     * 
     * @throws IllegalStateException
     *             If buffering hasn't been started.
     */
    public abstract long getTimeBaseEndTime() throws IllegalStateException;

    /**
     * Get the time buffering was started (according to System.currentTimeMillis()) (milliseconds)
     * 
     * @return The system time that buffering was started (in milliseconds).
     * 
     * @throws IllegalStateException
     *             If buffering hasn't been started.
     */
    public abstract long getSystemStartTime() throws IllegalStateException;

    /**
     * Get the time buffering was stopped (according to System.currentTimeMillis()) 
     * or System.currentTimeMillis() if the TSB is still buffering (milliseconds)
     * 
     * @return The system end time of the TSB, in nanoseconds or Long.MAX_VALUE if the
     *         TSB is still buffering.
     * 
     * @throws IllegalStateException
     *             If buffering hasn't been started.
     */
    public abstract long getSystemEndTime() throws IllegalStateException;

    /**
     * Get the offset of the TSB start within the TimeShiftWindow, measured in
     * nanoseconds.
     * 
     * @return The offset of the TSB start within the TimeShiftWindow in ns.
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or any other error.
     */
    public abstract long getTSWStartTimeOffset() throws IllegalStateException;

    /**
     * Get the size of the TSB, in seconds. This represents the maximum content
     * duration that can be stored in the TimeShiftBuffer.
     * 
     * @return duration of buffered content, in seconds.
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (deleteBuffer() has
     *             been called) or any other error.
     */
    public abstract long getSize() throws IllegalStateException;

    /**
     * Get the duration of content buffered in the TSB, in nanoseconds
     * 
     * @return duration of buffered content, in nanoseconds.
     * 
     * @throws IllegalStateException
     */
    public abstract long getDuration() throws IllegalStateException;

    /**
     * Return true of the TSB is being buffered into
     * 
     * @return true of the TSB is being buffered into
     * 
     * @throws IllegalStateException
     */
    public abstract boolean isBuffering() throws IllegalStateException;
    
    /**
     * Return true of the TSB is being played out of
     * 
     * @return true of the TSB is being played out of
     * 
     * @throws IllegalStateException
     */
    public abstract boolean isBeingPlayed() throws IllegalStateException;
    
    /**
     * Return true of the TSB is being actively copied into a recording
     * 
     * @return true of the TSB is being actively copied into a recording
     * 
     * @throws IllegalStateException
     */
    public abstract boolean isBeingCopied() throws IllegalStateException;
    
    /**
     * Return true if the TSB has any recorded content
     * 
     * @return true of the TSB has any recorded content
     * 
     * @throws IllegalStateException
     */
    public abstract boolean hasContent();
    
    /**
     * Get the media time of the oldest content within the native TSB, in
     * nanoseconds (this will result in a call to native/MPE)
     * 
     * @return media time of the earliest content within the native TSB, in
     *         nanoseconds
     * 
     * @throws IllegalStateException
     *             if the native TSB handle is invalid
     */
    public abstract long getContentStartTimeInMediaTime() throws IllegalStateException;

    /**
     * Get the media time of the newest content within the native TSB, in
     * nanoseconds (this will result in a call to native/MPE)
     * 
     * @return media time of the latest content within the native TSB, in
     *         nanoseconds
     * 
     * @throws IllegalStateException
     *             if the native TSB handle is invalid
     */
    public abstract long getContentEndTimeInMediaTime() throws IllegalStateException;

    /**
     * Get the components in the TimeShiftBuffer active at the given TimeBase
     * time.
     * 
     * @return PIDMapTable that applies for timeBaseTime
     * 
     * @throws IllegalStateException
     */
    public abstract PidMapTable getPidMapForMediaTime(long timeBaseTime) throws IllegalStateException;

    /**
     * Get the components in the TimeShiftBuffer active at the given system
     * time. System time is measured in milliseconds between midnight January 1,
     * 1970 UTC.
     * 
     * @return PIDMapTable that applies for mediaTime
     * 
     * @throws IllegalStateException
     */
    public abstract PidMapTable getPidMapForSystemTime(long systemTime) throws IllegalStateException;

    /**
     * Get the components in the TimeShiftBuffer per media time.
     * 
     * Note: The returned TimeTable will always have an element at time 0.
     * 
     * Note that the returned TimeTable is subject to element augmentation but
     * not to element removal.
     * 
     * @return TimeTable containing PIDMapTables
     * 
     * @throws IllegalStateException
     */
    public abstract TimeTable getPidMapTimeTable() throws IllegalStateException;

    public abstract LightweightTriggerEventTimeTable getLightweightTriggerEventTimeTable();

    /**
     * Get the CCI events in the TimeShiftBuffer per media time.
     * 
     * Note: The returned TimeTable will always have an element at time 0.
     * 
     * Note that the returned TimeTable is subject to element augmentation but
     * not to element removal.
     * 
     * @return TimeTable containing CCIStatusEvents
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer (never was buffering)
     */
    public abstract TimeTable getCCITimeTable();

    /**
     * Get the native handle for the TimeShiftBuffer
     * 
     * @return Native TimeShiftBuffer handle
     * 
     * @throws IllegalStateException
     *             If there is no native TimeShiftBuffer
     */
    public abstract int getNativeTSBHandle() throws IllegalStateException;

    /**
     * Immediately stores lwte into the TimeShiftBuffer. Checks for a previous
     * event with the same identity
     * 
     * @param lwte
     */
    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte);

    /**
     * Return the reason code associated with the buffering stop (see
     * 
     * @see org.cablelabs.impl.manager.timeshift.TimeShiftBuffer#getNativeTSBHandle()
     *      . Some typical reasons would be TSWREASON_INTLOST,
     *      TSWREASON_PIDCHANGE, TSWREASON_SERVICEREMAP,
     *      TSWREASON_ACCESSWITHDRAWN, or TSWREASON_SYNCLOST. These codes can be
     *      used by the JMF or RecordingManager to properly disposition the
     *      activity and/or perform appropriate signaling.
     * 
     *      If the buffer has never been stopped, return
     *      TimeShiftManager.TSWREASON_NOREASON
     * 
     * @return The reason code associated with the buffering stop.
     */
    public int getBufferingStopReason();
} // END interface TimeShiftBuffer
