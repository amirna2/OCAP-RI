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

import java.util.Enumeration;

import javax.media.TimeBase;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreReadChange;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient;
import org.cablelabs.impl.service.ServiceExt;
import org.ocap.resource.ResourceUsage;

/**
 * The <code>TimeShiftWindowClient</code> interface defines how a client may
 * interact with a TimeShiftWindow - which is a shared, common abstraction of a
 * collection of time-shift buffers and an associated network interface all on
 * the same Service.
 * 
 * @see org.cablelabs.impl.manager.TimeShiftManager
 * @see org.cablelabs.impl.manager.timeshift.TimeShiftWindow
 * 
 * @author Craig Pratt
 */
public interface TimeShiftWindowClient extends PlaybackClient, LightweightTriggerEventStoreReadChange
{
    /**
     * Attach to the associated TimeShiftWindow for the designated use(s). This
     * should be performed before the native TimeShiftBuffer use is started
     * (e.g. buffered playback, tsb conversion). The use codes are defined in
     * {@link org.cablelabs.impl.manager.TimeShiftManager}.
     * 
     * Note: Due to mid-flight state change indications, clients may call to
     * attach when the state of the TSW/tuner has changed out from underneath
     * them. So clients should be prepared for an IllegalStateException in these
     * cases and use the TimeShiftWindowChangedListener eventing to drive their
     * recovery.
     * 
     * @param uses
     *            Use(s) that the client is interested in performing on the
     *            TimeShiftWindow.
     * 
     * @throws IllegalStateException
     *             If the TimeShiftWindow cannot support the designated use, or
     *             the associated TimeShiftWindow is not in a valid state for
     *             attachment.
     * @throws IllegalArgumentException
     *             if the uses is invalid.
     */
    public void attachFor(final int uses) throws IllegalStateException, IllegalArgumentException;

    /**
     * Detach to the associated TimeShiftWindow for the designated use(s). This
     * should be performed after use of the native TimeShiftBuffer use(s) is
     * terminated (e.g. after buffered playback is stopped). The use codes are
     * defined in {@link org.cablelabs.impl.manager.TimeShiftManager}.
     * 
     * @param uses
     *            Use(s) that the client is no longer interested in performing
     *            on the TimeShiftWindow.
     * 
     * @throws IllegalArgumentException
     *             if the uses is invalid or there was no corresponding
     *             attachFor() performed by this client.
     */
    public void detachFor(final int uses) throws IllegalArgumentException;

    /**
     * Detach to the associated TimeShiftWindow for all attached uses. This
     * should be performed after use of the native TimeShiftBuffer use(s) is
     * terminated (e.g. after buffered playback is stopped).
     *
     */
    public void detachForAll();

    /**
     * Gets the usages that the TimeShiftWindowClient is interested performing
     * of the TimeShiftWindow
     * 
     * @return Uses that the client is interested in performing on the
     *         TimeShiftWindow
     */
    public int getUses();

    /**
     * Get the state of the associated TimeShiftWindow. The state codes are
     * defined in {@link org.cablelabs.impl.manager.TimeShiftManager}.
     * 
     * @return The current TimeShiftWindow state
     */
    public int getState();

    /**
     * Get the Service the TimeShiftWindow is operating on.
     * 
     * @return The
     */
    public ServiceExt getService();

    /**
     * Get the ResourceUsage associated with this client
     * 
     * @return the ResourceUsage associated with this client
     */
    public ResourceUsage getResourceUsage();
    
    /**
     * Return an enumeration for the TimeShiftBuffers in the TimeShiftWindow.
     * Note: The list of available TSBs may change during enumeration.
     * 
     * @return Enumeration over the TSBs in the TimeShiftWindow.
     */
    public java.util.Enumeration elements();

    /**
     * Return the TimeShiftBuffer which is currently being buffered into.
     * 
     * @return The TimeShiftBuffer which is actively being buffered into.
     * @throws IllegalStateException
     *             if the associated TimeShiftWindow is not in the
     *             <code>TSWSTATE_BUFFERING</code> or <code>TSWSTATE_BUFF_PENDING</code> state.
     */
    public TimeShiftBuffer getBufferingTSB() throws IllegalStateException;

    /**
     * Return the first (and earliest) TimeShiftBuffer in the associated
     * TimeShiftWindow. Returns null if there are no TimeShiftBuffers in the
     * TimeShiftWindow.
     * 
     * @return First TimeShiftBuffer in the TimeShiftWindow or null
     */
    public TimeShiftBuffer getFirstTSB();

    /**
     * Return the last (and latest) TimeShiftBuffer in the associated
     * TimeShiftWindow. The last TSB may or may not be buffering. Returns null
     * if there are no TimeShiftBuffers in the TimeShiftWindow.
     * 
     * @return First TimeShiftBuffer in the TimeShiftWindow or null
     */
    public TimeShiftBuffer getLastTSB();

    /**
     * Return the TimeShiftBuffer mapped to the given TimeShiftWindow TimeBase
     * time.
     * 
     * The TimeShiftWindow represents a continuum mapped to 0 or more
     * TimeShiftBuffers according to the TSB TimeBase times.
     * 
     * If no TSB is mapped to the given time, getTSBForMediaTime() will find
     * either the nearest preceding or nearest following TSB if proximity is set
     * to PROXIMITY_BACKWARD or PROXIMITY_FORWARD, respectively.
     * 
     * Note that the returned TSB is or was active at the given time but may not
     * contain content for the given time due to reclamation/wrapping.
     * 
     * @param timeBaseTime
     *            The target time.
     * @param proximity
     *            Direction to seek for nearest TSB when no TSB contains the
     *            target time
     * @return The TimeShiftBuffer covering the given TimeShiftWindow media
     *         time. Will be null of no TSB covers the given time.
     */
    public TimeShiftBuffer getTSBForTimeBaseTime(long timeBaseTime, int proximity);

    public static final int PROXIMITY_EXACT = 0;

    public static final int PROXIMITY_FORWARD = 2;

    public static final int PROXIMITY_BACKWARD = 3;

    /**
     * Return the TimeShiftBuffer mapped to the given TimeShiftWindow system
     * time.
     * 
     * The TimeShiftWindow is a continuum mapped to 0 or more TimeShiftBuffers,
     * each with its own start and end times.
     * 
     * If no TSB is mapped to the given time, getTSBForSystemTime() will find
     * either the nearest preceding or nearest following TSB if proximity is set
     * to PROXIMITY_BACKWARD or PROXIMITY_FORWARD, respectively.
     * 
     * Note that the returned TSB is or was active at the given media time but
     * may not contain content for the given time (due to reclamation/wrapping).
     * 
     * @param systemTime
     *            Media time to find in the TSB.
     * @param proximity
     *            Direction to seek for nearest TSB when no TSB contains time
     * @return The TimeShiftBuffer covering the given TimeShiftWindow media
     *         time. Will be null of no TSB covers the given time.
     */
    public TimeShiftBuffer getTSBForSystemTime(long systemTime, int proximity);

    /**
     * Return the TimeShiftBuffers with content that overlaps the given time
     * span expressed in system time (see System.currentTimeMillis()).
     * 
     * The TimeShiftWindow is a continuum mapped to 0 or more TimeShiftBuffers,
     * each with its own start and end times.
     * 
     * @param startTimeInSystemTimeMs
     *            Start of the timespan in system time (milliseconds)
     * @param durationMs
     *            Duration of the time span (milliseconds)
     * @return Enumeration containing TimeShiftBuffers with content that
     *         intersect the given timespan.
     */
    public Enumeration getTSBsForSystemTimeSpan(long startTimeInSystemTimeMs, long durationMs);

    /**
     * Return the TimeShiftBuffer which immediately preceeds, in time, the given
     * TSB in the TimeShiftWindow.
     * 
     * @param tsb
     *            Target TimeShiftBuffer
     * @return TimeShiftBuffer that chronologically preceeds tsb in the
     *         TimeShiftWindow
     * @throws IllegalArgumentException
     *             if the given TSB is invalid or fell out of the
     *             TimeShiftWindow frame.
     */
    public TimeShiftBuffer getTSBPreceeding(TimeShiftBuffer tsb);

    /**
     * Return the TimeShiftBuffer which immediately follows, in time, the given
     * TSB in the TimeShiftWindow.
     * 
     * @param tsb
     *            Target TimeShiftBuffer
     * @return TimeShiftBuffer that chronologically follows tsb in the
     *         TimeShiftWindow
     * @throws IllegalArgumentException
     *             if the given TSB is invalid or fell out of the
     *             TimeShiftWindow frame.
     */
    public TimeShiftBuffer getTSBFollowing(TimeShiftBuffer tsb);

    /**
     * Attach the TimeShiftWindowClient to the given TimeShiftBuffer.
     * 
     * This is necessary for retention of the TimeShiftBuffer. Unattached TSBs
     * that fall out of the maximum duration constraints of the TimeShiftWindow
     * are free to be deallocated.
     * 
     * The native TSB handle will not retrievable until the TSB is attached.
     * 
     * @param tsb
     *            TimeShiftBuffer to attach
     * @return TimeShiftBuffer which has been attached (will be tsb)
     * @throws IllegalArgumentException
     *             if the given TSB is invalid or fell out of the
     *             TimeShiftWindow frame.
     */
    public TimeShiftBuffer attachToTSB(TimeShiftBuffer tsb) throws IllegalArgumentException;

    /**
     * Detach the TimeShiftWindowClient from the given TimeShiftBuffer.
     * 
     * The native TSB handle will not retrievable after this call.
     * 
     * @param tsb
     *            TimeShiftBuffer to detach
     * 
     * @throws IllegalArgumentException
     *             if the given TSB is invalid or was not attached by this
     *             client.
     */
    public void detachFromTSB(TimeShiftBuffer tsb) throws IllegalArgumentException;

    /**
     * Detach the TimeShiftWindowClient from all attached TimeShiftBuffers.
     * 
     * The native TSB handles will not retrievable after this call.
     */
    public void detachAllTSBs();

    /**
     * Gets the native TimeShiftBuffer handle. The caller should only acquire
     * this handle after (a) ensuring the TimeShiftWindow is in the
     * <code>TSWSTATE_BUFFERING</code> or <code>TSWSTATE_BUFF_PENDING</code> state 
     * and the caller has performed an appropriate attachFor() operation.
     * 
     * @return The native TimeShiftBuffer handle
     * @throws IllegalStateException
     *             if the associated TimeShiftWindow is not in the
     *             <code>TSWSTATE_BUFFERING</code> or <code>TSWSTATE_BUFF_PENDING</code> state.
     */
    public int getBufferingTSBHandle() throws IllegalStateException;

    /**
     * Sets the minimum duration that the client requires to have buffered by
     * the associated TimeShiftWindow, in seconds.
     * 
     * This call may be made in any state but won't take effect until in or a
     * transition to the <code>TSWSTATE_BUFFERING</code> or <code>TSWSTATE_BUFF_PENDING</code>
     * state. If called in one of these states, the associated TimeShiftWindow may have
     * to transition to the <code>SWSTATE_BUFFSHUTDOWN</code> state if the
     * active TimeShiftBuffer is not of sufficient size and cannot be seamlessly
     * increased to meet the specified minimum duration.
     * 
     * If the associated TimeShiftWindow is a buffering state and space is not available to resize
     * the timeshift, an IllegalArgumentException is thrown and the
     * TimeShiftWindow will remain in its current state.
     * 
     * @param duration
     *            Required minimum duration, in seconds.
     * @throws IllegalArgumentException
     *             if the specified duration cannot be honored due to lack of
     *             disk space.
     */
    public void setMinimumDuration(final long duration) throws IllegalArgumentException;

    /**
     * Sets the largest duration that the client is interested in buffering by
     * the associated TimeShiftWindow, in seconds.
     * 
     * This call may be made in any state but won't take effect until in or a
     * transition to the <code>TSWSTATE_BUFFERING</code> or 
     * <code>TSWSTATE_BUFF_PENDING</code>state. This duration
     * change will never cause the TimeShiftWindow to transition to the
     * <code>TSWSTATE_BUFFSHUTDOWN</code> state. But it will be factored in to
     * the actual duration used for the timeshift.
     * 
     * @param duration
     *            Desired duration, in seconds.
     * @throws IllegalArgumentException
     *             of the specified component list contains components which are
     *             not part of the service or if the component list is invalid.
     */
    public void setDesiredDuration(final long duration);

    /**
     * Sets the maximum duration that the timeshift is allowed to buffer, in
     * seconds.
     * 
     * This call may be made in any state but won't take effect until in or a
     * transition to the <code>TSWSTATE_BUFFERING</code> or <code>TSWSTATE_BUFF_PENDING</code> 
     * state. If called in one of these states, the associated TimeShiftWindow may have
     * to transition to the <code>SWSTATE_BUFFSHUTDOWN</code> state if the
     * active TimeShiftBuffer is larger than the specified duration and cannot
     * be dynamically reduced.
     * 
     * Note that this call is intended only to enforce copy control limitations,
     * such as CCI bits.
     * 
     * @param duration
     *            Required maximum duration, in seconds.
     * @throws IllegalArgumentException
     *             of the specified component list contains components which are
     *             not part of the service or if the component list is invalid.
     */
    public void setMaximumDuration(final long duration);

    /**
     * Get the current duration of the timeshift contents, in seconds. This
     * value represents the amount of content in all TSBs contained in the
     * associated TimeShiftWindow.
     * 
     * @return Current duration of timeshifted content, in seconds.
     */
    public long getDuration();

    /**
     * Get the current capacity of the timeshift, in seconds. This value
     * represents the amount of timeshift supported by the associated
     * TimeShiftWindow.
     * 
     * @return Current capacity of the timeshift, in seconds.
     */
    public long getSize();

    /**
     * Change the registered TimeShiftWindowChangedListener.
     * 
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority 
     *            Priority for listener notifications. Higher numerical
     *            values imply higher priority (when in doubt, use 
     *            TimeShiftManager.LISTENER_PRIORITY_DEFAULT)
     */
    public void changeListener(final TimeShiftWindowChangedListener tswcl, final int tswclPriority);

    /**
     * Add a TimeShiftWindowClient to the associated TimeShiftWindow and return
     * a reference. The returned TimeShiftWindowClient will not be attached for
     * any use and will have no restrictions on the associated buffer (e.g.
     * MinimumumDuration/DesiredDuration will be 0, MaximumDuration is
     * Integer.MAX_VALUE). This method does not take a ResourceUsage, so it is
     * presumed that either the associated usage of the NetworkInterface is
     * already represented or the client is associated with a use that doesn't
     * require the NetworkInterface (e.g. BUFFERPLAYBACK).
     * 
     * @param reserveFor
     *            the requested reservation flags bitmask
     * @param tswcl
     *            The TimeShiftWindowChangedListener to be notified when the
     *            TimeShiftWindow associated with the
     *            <code>TimeShiftWindowClient</code> changes.
     * @param tswclPriority The priority of the notification. Higher priority 
     *                      listeners will be notified ahead of lower priority
     *                      listeners (see TimeShiftManager.LISTENER_PRIORITY_*)
     * @see TimeShiftManager
     * 
     * @throws IllegalArgumentException
     *             if the reservations cannot be supported
     */
    public TimeShiftWindowClient addClient(int reserveFor, TimeShiftWindowChangedListener tswcl, int tswclPriority);

    /**
     * Get the network interface. Note, this is a dangerous operation. There is
     * no guarantee that the the NetworkInterface returned by this call will
     * still be in use at any point after it's returned. Callers should use
     * caution, and a TimeShiftWindowChangedListener.
     */
    public ExtendedNetworkInterface getNetworkInterface();

    /**
     * Return the current authorization state of the associated 
     * TimeShiftWindow.
     * 
     * @return true if the associated TimeShiftWindow's NI is tuned and
     *         buffering is authorized and false otherwise.
     */
    public boolean isAuthorized();
    
    /**
     * Return the LTSID associated with the current authorization for the associated 
     * TimeShiftWindow.
     * 
     */
    public short getLTSID();
    
    /**
     * Remove the client's interest in the associated TimeShiftWindow. After
     * this call, the client's interest in the TimeShiftWindow is removed and
     * its TimeShiftWindowChangedListener will no longer be invoked.
     * 
     * release() should be called prior to the client removing its reference to
     * the TimeShiftWindowClient.
     */
    public void release();

    /**
     * Compare two TimeShiftWindowClient's to make sure they correspond to the
     * same underlying TimeShiftWindow object.
     * 
     * @param other
     *            The other TimeShiftWindowClient to compare to.
     */
    public boolean sameAs(TimeShiftWindowClient other);

    /**
     * Get the TimeBase being used by the associated TimeShiftWindow. Calls to
     * TimeShiftBuffer.getTimeBaseStart/EndTime() will be returned relative to
     * this TimeBase.
     * 
     * @return The TimeShiftWindow's TimeBase
     */
    public TimeBase getTimeBase();

    /**
     * Get the TimeBase start time corresponding with the start of the
     * associated TimeShiftWindow (in nanoseconds).
     * 
     * @return The associated TimeShiftWindow's start time, according to the
     *         TimeBase returned by getTimeBase() (in nanoseconds)
     */
    public long getTimeBaseStartTime();

    /**
     * Get the system start time corresponding with the start of the
     * associated TimeShiftWindow (in milliseconds).
     * 
     * @return The associated TimeShiftWindow's start time, according to the
     *         TimeBase returned by getTimeBase() (in milliseconds)
     */
    public long getSystemStartTimeMs();
} // END interface TimeShiftWindowClient
