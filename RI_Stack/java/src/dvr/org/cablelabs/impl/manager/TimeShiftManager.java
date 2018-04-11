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

import java.util.Vector;

import javax.tv.service.Service;

import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl;
import org.cablelabs.impl.manager.timeshift.TimeShiftWindow;

/**
 * A <code>Manager</code> that provides a discovery and sharing mechanism for
 * TimeShiftWindows and associated NetworkInterfaces.
 * 
 * The <code>TimeShiftManager</code> implementation provides access to
 * abstractions known as {@link org.cablelabs.impl.manager.TimeShiftWindow}s.
 * The TimeShiftWindow binds TimeShiftBuffer(s) and a
 * <code>NetworkInterface</code> according to their particular service and a
 * non-fixed window of time.
 * 
 * This interface enables the discovery and enumeration of
 * <code>TimeShiftWindow</code>s based upon a variety of criteria, always
 * returning a handle to a TimeShiftWindow control interface
 * {@link org.cablelabs.impl.manager.TimeShiftManager.TimeShiftWindowClient}.
 * 
 * @see org.cablelabs.impl.manager.TimeShiftWindowClient
 * @see org.cablelabs.impl.manager.timeshift.TimeShiftWindow
 * 
 * @author Craig Pratt
 */
public interface TimeShiftManager extends Manager
{
    /**
     * Returns a TimeShiftWindowClient for a TimeShiftWindow which is tuned or
     * in the process of tuning to Service s, and is capable of a timeshift of
     * at least <code>minDuration</code> seconds.
     * 
     * Note that this call will return a client to a TimeShiftWindow which is in
     * any of the following states: TUNE_PENDING, TUNED, or BUFFERING.
     * 
     * The ResourceUsage parameter <code>ru</code> will be used to represent the
     * usage of the interface. If the TimeShiftWindow must acquire an interface,
     * <code>ru</code> will be used to perform the reservation and represent the
     * interface for resource contention, if no NetworkInterface is available.
     * If a NetworkInterface cannot be acquired for the TimeShiftWindow,
     * NoFreeInterfaceException is thrown.
     * 
     * @param s
     *            The service requested for timeshift
     * @param minDuration
     *            The duration required for the timeshift. If 0, any timeshift
     *            duration is considered acceptable.
     * @param maxDuration
     *            The largest duration the client is interested in timeshifting.
     *            If more than one TimeShiftWindow is available for
     *            <code>s</code>, the one with a duration which best accomodates
     *            maxDuration will be used. maxDuration may also be facored in
     *            when establishing the timeshift duration. If 0, only the
     *            minDuration will be used to locate/activate a timeshift.
     * @param uses
     *            Designates which timeshift functionality is required. e.g.
     *            <code>TSWUSE_LIVEPLAYBACK|TSWUSE_BUFFERPLAYBACK</code>. A
     *            returned TimeShiftClient will be capable of being attached to
     *            for these uses.
     * @param ru
     *            The ResourceUsage of the client attempting to utilize the
     *            timeshift. The <code>ru</code> will be used to reserve the
     *            NetworkInterface and perform resource contention, if tuning is
     *            required. Otherwise, <code>ru</code> will be added to the
     *            NetworkInterface's SharedResourceUsage.
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority 
     *            Priority for listener notifications. Higher numerical
     *            values imply higher priority (when in doubt, use LISTENER_PRIORITY_DEFAULT)
     * @return A <code>TimeShiftWindowClient</code> which references a
     *         TimeShiftWindow which is tuned to or in the process of tuning to
     *         Service <code>s</code> and is capable of timeshifting duration
     *         <code>minDuration</code>.
     * @throws NoFreeInterfaceException
     *             if the TimeShiftManager could not obtain an existing or new
     *             TimeShiftWindow, even after resource contention.
     */
    public TimeShiftWindowClient getTSWByDuration(final Service s, final long minDuration, final long maxDuration,
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, final int tswclPriority)
            throws NoFreeInterfaceException;

    /**
     * Returns a TimeShiftWindowClient for a TimeShiftWindow which overlaps with
     * the designated time span for Service s. If the end of the time span is in
     * the future, a TimeShiftWindow may be returned which is in the process of
     * tuning to Service s.
     * 
     * If more than one TimeShiftWindow may satisfy this request, the one
     * containing the largest portion of the designated time span will be
     * returned.
     * 
     * Note that this call will return a client to a TimeShiftWindow which is in
     * any of the following states: TUNE_PENDING, TUNED, or BUFFERING.
     * 
     * @param s
     *            The service requested for timeshift
     * @param startTime
     *            The requested start time for the time span the client is
     *            interested accessing content from the Service. This time
     *            should be at or before the current time. Units are
     *            milliseconds from midnight, January 1, 1970 UTC.
     * @param duration
     *            Designates the duration of the time span, in milliseconds.
     * @param uses
     *            Designates which timeshift functionality is required. e.g.
     *            <code>TSWUSE_RECORDING</code> A returned TimeShiftClient will
     *            be capable of being attached to for these functions and will
     *            be reserved for these uses. TimeShiftWindowClient.attachFor()
     *            should be performed by the caller when appropriate.
     * @param ru
     *            The ResourceUsage of the client attempting to utilize the
     *            timeshift. The <code>ru</code> will be used to reserve the
     *            NetworkInterface and perform resource contention, if tuning is
     *            required. Otherwise, <code>ru</code> will be added to the
     *            NetworkInterface's SharedResourceUsage.
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority 
     *            Priority for listener notifications. Higher numerical
     *            values imply higher priority (when in doubt, use LISTENER_PRIORITY_DEFAULT)
     * @see org.cablelabs.impl.manager.TimeShiftWindowClient#attachFor(int)
     * @return A <code>TimeShiftWindowClient</code> which references a
     *         TimeShiftWindow which is tuned to or in the process of tuning to
     *         Service <code>s</code> and, if multiple TSWs are buffering
     *         <code>s</code>, starts closest to <code>startTime</code>.
     * @throws IllegalStateException
     *             if no TimeShiftWindow contains content which overlaps with
     *             the designated time span.
     * @throws NoFreeInterfaceException
     *             if the TimeShiftManager could not obtain an existing or new
     *             TimeShiftWindow, even after resource contention.
     * 
     */
    public TimeShiftWindowClient getTSWByTimeSpan(final Service s, final long startTime, final long duration,
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, final int tswclPriority)
            throws NoFreeInterfaceException, IllegalStateException;

    /**
     * Returns a TimeShiftWindowClient for a TimeShiftWindow which is tuned or
     * in the process of tuning to Service s using NetworkInterface
     * <code>ni</code> and can support the designated <code>uses</code>. If a
     * usable TimeShiftWindow is found, a TimeShiftClient for the
     * TimeShiftWindow will be returned. If no TimeShiftWindow is available
     * 
     * Note that this call will return a client to a TimeShiftWindow which is in
     * any of the following states: TUNE_PENDING, TUNED, or BUFFERING.
     * 
     * If the TimeShiftWindow utilizing <code>ni</code> cannot support the
     * designated <code>uses</code>, NoFreeInterfaceException is thrown. The
     * ResourceUsage parameter <code>ru</code> will be used to represent the
     * usage of the interface.
     * 
     * This method is intended for the implementation of recording via the
     * ServiceContextRecordingSpec.
     * 
     * @param s
     *            The service requested for timeshift
     * @param ni
     *            The NetworkInterface the client would like a TimeShiftWindow
     *            for.
     * @param startTime
     *            The requested start time for the time span the client is
     *            interested accessing content from the Service. This time
     *            should be at or before the current time. Units are
     *            milliseconds from midnight, January 1, 1970 UTC.
     * @param duration
     *            Designates the duration of the time span, in milliseconds.
     * @param uses
     *            Designates which timeshift functionality is required. e.g.
     *            <code>TSWUSE_RECORDING</code> A returned TimeShiftClient will
     *            be capable of being attached to for these functions.
     * @param ru
     *            <code>ru</code> to be added to the NetworkInterface's
     *            SharedResourceUsage.
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow accociated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority 
     *            Priority for listener notifications. Higher numerical
     *            values imply higher priority (when in doubt, use LISTENER_PRIORITY_DEFAULT)
     * @return A <code>TimeShiftWindowClient</code> which references a
     *         TimeShiftWindow which is tuned to or in the process of tuning to
     *         Service <code>s</code> and, if multiple TSWs are buffering
     *         <code>s</code>, starts closest to <code>startTime</code>.
     * @throws NoFreeInterfaceException
     *             if there is no TimeShiftWindow tuned to Service
     *             <code>s</code>, <code>ni</code> is not tuned to Service
     *             <code>s</code>, or the TimeShiftWindow associated with
     *             <code>ni</code> cannot support the designated usage.
     */
    public TimeShiftWindowClient getTSWByInterface(final Service s, final NetworkInterface ni, final long startTime,
            final long duration, final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, final int tswclPriority)
            throws NoFreeInterfaceException;

    /**
     * Returns a TimeShiftWindowClient for a TimeShiftWindow which is tuned or
     * in the process of tuning to Service s, and is capable of a timeshift of
     * any duration.
     * 
     * The ResourceUsage parameter <code>ru</code> will be used to represent the
     * usage of the interface. If the TimeShiftWindow must acquire an interface,
     * <code>ru</code> will be used to perform the reservation and represent the
     * interface for resource contention, if no NetworkInterface is available.
     * If a NetworkInterface cannot be acquired for the TimeShiftWindow,
     * NoFreeInterfaceException is thrown.
     * 
     * @param s
     *            The service requested for timeshift
     * @param uses
     *            Designates which timeshift functionality is required. e.g.
     *            <code>TSWUSE_LIVEPLAYBACK|TSWUSE_BUFFERPLAYBACK</code>. A
     *            returned TimeShiftClient will be capable of being attached to
     *            for these uses.
     * @param ru
     *            The ResourceUsage of the client attempting to utilize the
     *            timeshift. The <code>ru</code> will be used to reserve the
     *            NetworkInterface and perform resource contention, if tuning is
     *            required. Otherwise, <code>ru</code> will be added to the
     *            NetworkInterface's SharedResourceUsage.
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority 
     *            Priority for listener notifications. Higher numerical
     *            values imply higher priority (when in doubt, use LISTENER_PRIORITY_DEFAULT)
     * @return A <code>TimeShiftWindowClient</code> which references a
     *         TimeShiftWindow which is tuned to or in the process of tuning to
     *         Service <code>s</code> and is capable of timeshifting duration
     *         <code>minDuration</code>.
     * @throws NoFreeInterfaceException
     *             if the TimeShiftManager could not obtain an existing or new
     *             TimeShiftWindow, even after resource contention.
     */
    public TimeShiftWindowClient getTSWByService(final Service s, 
            final int uses, final ResourceUsage ru, final TimeShiftWindowChangedListener tswcl, int tswclPriority)
            throws NoFreeInterfaceException;

    /**
     * Establish if the TSW can be removed from the list of active TimeShiftwindows
     * and remove it from the list, if so.
     * 
     * @param tsw The TSW to be checked
     */
    void releaseUnusedTSWResources(final TimeShiftWindow tsw);

    /**
     * Returns a list of ResourceUsages that would be in use on the associated
     * NetworkInterface if the RecordingResourceUsage(s) in <code>rul</code>
     * were not present.
     * 
     * This method is intended for the use of the ResourceContentionWarning
     * logic to determine what the usage of a particular interface will be when
     * particular recording usages are no longer present. This is used to
     * determine the usage of an interface after a recording's end time.
     * 
     * @param ni
     *            The NetworkInterface to check for usage
     * @param rul
     *            The usages on the particular <code>ni</code>.
     * @param rrul
     *            The RecordingResourceUsages to consider as non-present.
     * @return List of the usages on <code>ni</code> with <code>rrul</code>
     *         considered non-present.
     */
    public Vector getRULWithoutRecs(final NetworkInterfaceImpl ni, final Vector rul, final RecordingResourceUsage[] rrul);

    /**
     * Used to determine if a particular set of usages on a single
     * NetworkInterface can support an additional RecordingResourceUsage.
     * 
     * This method is intended for the use of the ResourceContentionWarning
     * logic to determine if a scheduled (non-started) recording will be able to
     * share a NetworkInterface or will require a discreet NetworkInterface.
     * 
     * @param rul
     *            List of resource usages to check against.
     * @param ni
     *            The <code>NetworkInterface</code> to which the list of
     *            resource usages belong.
     * @param rru
     *            The RecordingResourceUsages to consider as non-present.
     * @param s
     *            The service of the rru.
     * @return <code>true</code> if <code>rul</code> can support
     *         <code>rru</code> and <code>false</code> otherwise.
     */
    public boolean canRULSupportRecording(Vector rul, NetworkInterface ni, RecordingResourceUsage rru, Service s);

    // 
    // TimeShiftWindowChangedListener priorities
    //
    public static final int LISTENER_PRIORITY_HIGH = 30;
    public static final int LISTENER_PRIORITY_LOW = 0;
    public static final int LISTENER_PRIORITY_DEFAULT = LISTENER_PRIORITY_LOW;


    // 
    // TimeShiftWindow USES. These are OR-able.
    //

    /**
     * <code>use</code> parameter: For holding the NI reservation.
     */
    public static final int TSWUSE_NIRES = 0x01;

    /**
     * <code>use</code> parameter: For performing live playback from the
     * associated NI.
     */
    public static final int TSWUSE_LIVEPLAYBACK = 0x02;

    /**
     * <code>use</code> parameter: For buffering into the timeshift.
     */
    public static final int TSWUSE_BUFFERING = 0x04;

    /**
     * <code>use</code> parameter: For recording/converting into a recording
     * file.
     */
    public static final int TSWUSE_RECORDING = 0x08;

    /**
     * <code>use</code> parameter: For performing playback from the timeshift
     * buffer(s). Note: Does not imply use of the associated NI.
     */
    public static final int TSWUSE_BUFFERPLAYBACK = 0x10;

    /**
     * <code>use</code> parameter: For performing playback from the timeshift
     * buffer(s) to the network. Note: Does not imply use of the associated NI.
     */
    public static final int TSWUSE_NETPLAYBACK = 0x20;

    // 
    // TimeShiftWindow STATES
    //

    public static final int TSWSTATE_INVALID = 0;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is not tuned. The TimeShiftWindow supports buffered
     * playback, but not buffering, conversion, or live playback in this state.
     */
    public static final int TSWSTATE_IDLE = 1;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is in the process of reserving the tuner. The
     * TimeShiftWindow supports buffered playback, but not buffering,
     * conversion, or live playback in this state. The state will transition to
     * either the <code>TSWSTATE_TUNE_PENDING</code> or
     * <code>TSWSTATE_IDLE</code> state from this state.
     */
    public static final int TSWSTATE_RESERVE_PENDING = 2;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is in the process of tuning. The TimeShiftWindow supports
     * buffered playback, but not buffering, conversion, or live playback in
     * this state. The state will transition to the
     * <code>TSWSTATE_READY_TO_BUFFER</code>,
     * <code>TSWSTATE_NOT_READY_TO_BUFFER</code>, or TSWSTATE_IDLE state from
     * this state.
     */
    public static final int TSWSTATE_TUNE_PENDING = 3;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is tuned but not buffering and conditions support
     * buffering. The TimeShiftWindow supports live or buffered playback, but no
     * buffering or conversion is ongoing in this state. Attaching for
     * buffering, recording, and live or buffered playback is allowed in this
     * state.
     */
    public static final int TSWSTATE_READY_TO_BUFFER = 4;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is tuned but not buffering and conditions do not support
     * buffering. The TimeShiftWindow supports buffered playback, but not
     * buffering, conversion, or live playback in this state. Attaching for
     * buffering, recording, and live playback is not allowed in this state.
     */
    public static final int TSWSTATE_NOT_READY_TO_BUFFER = 5;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow has initiated native buffering but is waiting for the
     * indication that buffering has started.
     */
    public static final int TSWSTATE_BUFF_PENDING = 6;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when the
     * TimeShiftWindow is tuned and buffering.
     */
    public static final int TSWSTATE_BUFFERING = 7;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when
     * timeshift buffering is being shut down. This state will be exited once
     * all clients have detached for buffering.
     */
    public static final int TSWSTATE_BUFF_SHUTDOWN = 8;

    /**
     * TimeShiftWindow state: The TimeShiftWindow will be in this state when te
     * tuner is being released. This state will be exited once all clients have
     * detached for buffering and live playback.
     */
    public static final int TSWSTATE_INTSHUTDOWN = 9;

    //findbugs comments that final arrays are not really final - elements are subject to change.
    //In this case the array is indexed by above state codes and only in the context of log messages
    //and toString methods. findbugs has been configured to ignore this "error".
    public static final String stateString[] = { "INVALID", "IDLE", "RESERVE_PENDING", "TUNE_PENDING",
            "READY_TO_BUFFER", "NOT_READY_TO_BUFFER", "BUFF_PENDING", "BUFFERING", "BUFF_SHUTDOWN", "INT_SHUTDOWN" };

    // 
    // TimeShiftWindow Reason codes
    //

    /**
     * TimeShiftWindow reason code signifying no particular reason.
     */
    public static final int TSWREASON_NOREASON = 0;

    /**
     * TimeShiftWindow reason code signifying that the change was due to
     * successful tune completion.
     */
    public static final int TSWREASON_TUNESUCCESS = 1;

    /**
     * TimeShiftWindow reason code signifying that the change was due to tune
     * failure.
     */
    public static final int TSWREASON_TUNEFAILURE = 2;

    /**
     * TimeShiftWindow reason code signifying that the change was due to a loss
     * of the NetworkInterface.
     */
    public static final int TSWREASON_INTLOST = 3;

    /**
     * TimeShiftWindow reason code signifying that the change was due to a loss
     * of the NetworkInterface.
     */
    public static final int TSWREASON_NOFREEINT = 4;

    /**
     * TimeShiftWindow reason code signifying that the change was due to a PID
     * change that requires flushing the TimeShiftBuffer and restarting it.
     */
    public static final int TSWREASON_PIDCHANGE = 5;

    /**
     * TimeShiftWindow reason code signifying that the change was due to a need
     * to increase the size of the timeshift.
     */
    public static final int TSWREASON_SIZEINCREASE = 6;

    /**
     * TimeShiftWindow reason code signifying that the change was due to a need
     * to decrease the size of the timeshift.
     */
    public static final int TSWREASON_SIZEREDUCTION = 7;

    /**
     * TimeShiftWindow reason code signifying that the change was due to the
     * associated Service being remapped to another transport or program.
     */
    public static final int TSWREASON_SERVICEREMAP = 8;

    /**
     * TimeShiftWindow reason code signifying that the change was due to the
     * associated Service being removed.
     */
    public static final int TSWREASON_SERVICEVANISHED = 9;

    /**
     * TimeShiftWindow reason code signifying that the change was due to the
     * tuner establishing or re-establishing synchronization with the stream
     * being acquired via the associated tuner.
     */
    public static final int TSWREASON_SYNCACQUIRED = 10;

    /**
     * TimeShiftWindow reason code signifying that the change was due to the
     * tuner losing or not achieving synchronization with the stream being
     * acquired via the associated tuner.
     */
    public static final int TSWREASON_SYNCLOST = 11;

    /**
     * TimeShiftWindow reason code signifying that the change was due to
     * insufficient components.
     */
    public static final int TSWREASON_NOCOMPONENTS = 12;

    /**
     * TimeShiftWindow reason code signifying that the change was due to
     * insufficient components.
     */
    public static final int TSWREASON_COMPONENTSADDED = 13;

    /**
     * TimeShiftWindow reason code signifying that the change was due to
     * conditional access being withdrawn.
     */
    public static final int TSWREASON_ACCESSWITHDRAWN = 14;

    /**
     * TimeShiftWindow reason code signifying that the change was due to
     * conditional access being granted.
     */
    public static final int TSWREASON_ACCESSGRANTED = 15;

    //findbugs comments that final arrays are not really final - elements are subject to change.
    //In this case the array is indexed by above reason codes and only in the context of log messages
    //and toString methods. findbugs has been configured to ignore this "error".
    public static final String[] reasonString = { "NOREASON", 
                                                  "TUNESUCCESS", 
                                                  "TUNEFAILURE", 
                                                  "INTLOST", 
                                                  "NOFREEINT",
                                                  "PIDCHANGE", 
                                                  "SIZEINCREASE", 
                                                  "SIZEREDUCTION", 
                                                  "SERVICEREMAP", 
                                                  "SERVICEVANISHED", 
                                                  "SYNCACQUIRED",
                                                  "SYNCLOST", 
                                                  "NOCOMPONENTS", 
                                                  "COMPONENTSADDED", 
                                                  "ACCESSWITHDRAWN", 
                                                  "ACCESSGRANTED" };

    /**
     * Return a vector of all TSW's currently in the system.
     * 
     * @param tswcl
     *            A TimeShiftWindowChangedListener to add to all the clients
     *            received.
     * @return A Vector of TimeShiftWindowClient's for the
     */
    public Vector getAllTSWs();

    /**
     * Set a listener for new TimeShiftWindow's.
     */
    public void setNewTimeShiftWindowListener(TimeShiftWindowListener l);
    
    /**
     * Get a cached reference to the PODManager
     */
    public PODManager getPODManager();
} // END interface TimeShiftManager
