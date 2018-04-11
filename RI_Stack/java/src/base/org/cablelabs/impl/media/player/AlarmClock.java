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

package org.cablelabs.impl.media.player;

import javax.media.MediaException;

/**
 * This interface defines methods and types for creating alarms based on media
 * times.
 * 
 * @author schoonma
 * 
 */
public interface AlarmClock
{
    public class AlarmException extends MediaException
    {
        public AlarmException(String reason)
        {
            super(reason);
        }
    }

    /**
     * This is the interface to an alarm, created by the
     * {@link Player#createAlarm(AlarmSpec, Callback)} method. Its methods provide control over the
     * created {@link Alarm}.
     * <p>
     * An alarm is either <b>active</b> or <b>inactive</b>. When active, the
     * {@link Alarm} is sensitive to media time changes; when inactive, it is
     * not. When initially created (via
     * {@link Player#createAlarm(AlarmSpec, Callback)}, an {@link Alarm}
     * is inactive.
     */
    public interface Alarm
    {
        /**
         * This interface defines the method that will be called back when an
         * alarm fires or becomes deactivated.
         */
        public interface Callback
        {
            /**
             * Called when the alarm "fires"&mdash;i.e., when the media time
             * passes the time specified by the alarm.
             * 
             * @param alarm
             *            The {@link Alarm} that fired.
             */
            void fired(Alarm alarm);

            /**
             * Called when an alarm becomes destroyed by the system because it
             * is no longer valid&mdash;e.g., if an error occurs when an alarm
             * changes internal state.
             * 
             * @param alarm
             *            The {@link Alarm} that became deactivated.
             * @param reason
             *            An {@link AlarmException} that indicates the failure
             *            reason; this could be, for example, an
             *            {@link Exception} of some kind that was thrown by the
             *            internal alarm machinery.
             */
            void destroyed(Alarm alarm, AlarmException reason);
        }

        /**
         * Activate the alarm.
         *
         * @throws AlarmException if it is unable to activate the alarm.
         */
        void activate() throws AlarmException;

        /**
         * Deactivate the alarm.
         */
        void deactivate();

        /**
         * @return Returns the {@link FixedAlarmSpec} that was used to create the
         *         {@link Alarm}.
         */
        AlarmSpec getAlarmSpec();
    }

    /**
     * Create a new {@link Alarm} from an {@link AlarmSpec} and associate it
     * with the {@link Player}. Initially, the alarm will be <em>disabled</em>.
     * To enable the alarm, call {@link Alarm#activate()} }.
     * 
     * @param spec
     *            The {@link FixedAlarmSpec} for which the {@link Alarm} is created.
     * @param callback
     *            The
     *            {@link org.cablelabs.impl.media.player.Player.Alarm.Callback
     *            Callback} to be invoked when the alarm fires.
     * @return Returns a newly created {@link Alarm} in the <em>in</em>active
     *         state.
     */
    Alarm createAlarm(AlarmSpec spec, Alarm.Callback callback);

    /**
     * Destroys an {@link Alarm}, created previously by a call to
     * {@link #createAlarm}. If the {@link Alarm} was active, it will
     * be deactivated.
     * 
     * @param alarm
     *            The {@link Alarm} to destroy. If the alarm is
     *            <code>null</code> or does not exist, it is ignored.
     */
    void destroyAlarm(Alarm alarm);

    interface AlarmSpec
    {
        /**
         * Accessor
         * @return alarmspec name
         */
        String getName();

        /**
         * Provide the mediatime at which this alarm should fire.
         * @return
         */
        long getMediaTimeNanos();

        /**
         * Provide the amount of wall time in nanoseconds when the alarm should fire relative to baseMediaTimeNanos mediatime and provided rate
         * @param baseMediaTimeNanos
         * @param rate
         * @return
         */
        long getDelayWallTimeNanos(long baseMediaTimeNanos, float rate);

        /**
         * Can the alarmSpec be scheduled based on the provided rate and base mediatime nanos
         * @param rate
         * @param baseMediaTimeNanos
         * @return
         */
        boolean canSchedule(float rate, long baseMediaTimeNanos);

        /**
         * Should the alarmspec fire based on the current mediatime nanos and the updated mediatime nanos
         * @param currentMediaTimeNanos
         * @param newMediaTimeNanos
         * @return
         */
        boolean shouldFire(long currentMediaTimeNanos, long newMediaTimeNanos);

        /**
         * These constants define the direction in which a media time alarm is
         * triggered.
         */
        public static interface Direction
        {
            /** Triggered only while playing at a negative rate. */
            public static final byte REVERSE = -1;

            /** Triggered while playing at a positive rate. */
            public static final byte FORWARD = 1;
        }
    }
}
