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

package org.cablelabs.impl.util;

import java.util.EventListener;

import javax.tv.service.selection.ServiceContext;

import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.storage.FreeSpaceListener;

/**
 * The <code>DVREventMulticaster</code> extends <code>EventMulticaster</code>
 * and is intended to assist the platform implementation with the management of
 * various DVR specific event listeners.
 * <p>
 * The <code>DVREventMulticaster</code> class is meant to handle event
 * dispatching for the following events:
 * 
 * <ul>
 * <li> {@link org.ocap.dvr.storage.TimeShiftBufferEvent TimeShiftBufferEvent}
 * </ul>
 * 
 * @see org.ocap.dvr.storage.TimeShiftBufferListener
 * 
 * @author Todd Earles
 */
public class DVREventMulticaster extends EventMulticaster implements TimeShiftListener, RecordingPlaybackListener,
        FreeSpaceListener
{
    protected DVREventMulticaster(EventListener a, EventListener b)
    {
        super(a, b);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new EventMulticaster instance which chains a with
     * b.
     * 
     * @param a
     *            event listener-a
     * @param b
     *            event listener-b
     */
    protected static EventListener addInternal(EventListener a, EventListener b)
    {
        if (a == null) return b;
        if (b == null) return a;
        return new DVREventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new EventMulticaster instance which chains a with
     * b.
     * <p>
     * This method is different from {@link #addInternal} in that it adds the
     * given listener only once!
     * 
     * @param a
     *            event listener-a
     * @param b
     *            event listener-b
     */
    protected static EventListener addOnceInternal(EventListener a, EventListener b)
    {
        // If a is empty just return b
        // If b already contains a, just return b
        if (a == null || contains(b, a)) return b;
        // If b is empty just return a
        // If a already contains b, just return a
        if (b == null || contains(a, b)) return a;
        return new DVREventMulticaster(a, b);
    }

    /**
     * Adds <i>FreeSpaceListener-a </i> with <i>FreeSpaceListener-b </i> and
     * returns the resulting multicast listener.
     * 
     * @param a
     *            FreeSpaceListener-a
     * @param b
     *            FreeSpaceListener-b
     * @return the resulting multicast listener
     */
    public static FreeSpaceListener add(FreeSpaceListener a, FreeSpaceListener b)
    {
        return (FreeSpaceListener) addOnceInternal(a, b);
    }

    /**
     * Removes a listener from this multicaster and returns the result.
     * 
     * <p>
     * This is identical to the version in HEventMulticaster, but it must be
     * here so that it is compiled against EventMulticaster's versions of the
     * addInternal and removeInternal static methods.
     * 
     * @param oldl
     *            the listener to be removed
     */
    protected EventListener remove(EventListener oldl)
    {
        if (oldl == a) return b;
        if (oldl == b) return a;
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b)
        {
            return this;
        }
        return addInternal(a2, b2);
    }

    /**
     * Removes the old <code>FreeSpaceListener-oldl</code> from
     * <i>FreeSpaceListener-l </i> and returns the resulting multicast listener.
     * 
     * @param l
     *            FreeSpaceListener-l
     * @param oldl
     *            the FreeSpaceListener being removed
     * @return the resulting multicast listener
     */
    public static FreeSpaceListener remove(FreeSpaceListener l, FreeSpaceListener oldl)
    {
        return (FreeSpaceListener) removeInternal(l, oldl);
    }

    // Description copied from RecordingPlaybackListener
    public void notifyRecordingPlayback(ServiceContext serviceContext, int artificialCarouselId, int[] carouselIDs)
    {
        if (a != null)
            ((RecordingPlaybackListener) a).notifyRecordingPlayback(serviceContext, artificialCarouselId, carouselIDs);
        if (b != null)
            ((RecordingPlaybackListener) b).notifyRecordingPlayback(serviceContext, artificialCarouselId, carouselIDs);
    }

    // Description copied from FreeSpaceListener
    public void notifyFreeSpace()
    {
        if (a != null) ((FreeSpaceListener) a).notifyFreeSpace();
        if (b != null) ((FreeSpaceListener) b).notifyFreeSpace();

    }

    /**
     * Adds <i>RecordingPlaybackListener</i> with
     * <i>RecordingPlaybackListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            RecordingPlaybackListener-a
     * @param b
     *            SRecordingPlaybackListener-b
     * @return the resulting multicast listener
     */
    public static RecordingPlaybackListener add(RecordingPlaybackListener a, RecordingPlaybackListener b)
    {
        return (RecordingPlaybackListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>RecordingPlaybackListener</code> from
     * <i>RecordingPlaybackListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            RecordingPlaybackListener-l
     * @param oldl
     *            the RecordingPlaybackListener being removed
     * @return the resulting multicast listener
     */
    public static RecordingPlaybackListener remove(RecordingPlaybackListener l, RecordingPlaybackListener oldl)
    {
        return (RecordingPlaybackListener) removeInternal(l, oldl);
    }

    /**
     * Adds <i>TimeShiftBufferListener-a</i> with
     * <i>TimeShiftBufferListener-b</i> and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            TimeShiftBufferListener-a
     * @param b
     *            TimeShiftBufferListener-b
     * @return the resulting multicast listener
     */
    // CHANGE MADE FOR iTSB INTEGRATION
    public static TimeShiftListener add(TimeShiftListener a, TimeShiftListener b)
    {
        return (TimeShiftListener) addOnceInternal(a, b);
    }

    /**
     * Removes the old <code>TimeShiftBufferListener</code> from
     * <i>TimeShiftBufferListener-l</i> and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            TimeShiftBufferListener-l
     * @param oldl
     *            the TimeShiftBufferListener being removed
     * @return the resulting multicast listener
     */
    // CHANGE MADE FOR iTSB INTEGRATION
    public static TimeShiftListener remove(TimeShiftListener l, TimeShiftListener oldl)
    {
        return (TimeShiftListener) removeInternal(l, oldl);
    }

    // Description copied from TimeShiftListener
    public void receiveTimeShiftevent(TimeShiftEvent event)
    {
        if (a != null) ((TimeShiftListener) a).receiveTimeShiftevent(event);
        if (b != null) ((TimeShiftListener) b).receiveTimeShiftevent(event);
    }
}
