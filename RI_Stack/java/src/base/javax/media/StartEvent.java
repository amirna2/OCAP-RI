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

package javax.media;

/**
 * <code>StartEvent</code> is a <code>TransitionEvent</code> that indicates that
 * a <code>Controller</code> has entered the <i>Started</i> state. Entering the
 * <i>Started</i> state implies that <code>syncStart</code> has been invoked,
 * providing a new <i>media time</i> to <i>time-base time</i> mapping.
 * <code>StartEvent</code> provides the <I>time-base time</I> and the
 * <I>media-time</I> that <i>Started</i> this <CODE>Controller</CODE>.
 * 
 * @see Controller
 * @see ControllerListener
 * @version 1.31, 97/08/23
 */
public class StartEvent extends TransitionEvent
{

    private Time mediaTime, timeBaseTime;

    /**
     * Construct a new <code>StartEvent</code>. The <code>from</code> argument
     * identifies the <code>Controller</code> that is generating this event. The
     * <code>mediaTime</code> and the <code>tbTime</code> identify the
     * <I>media-time</I> to <I>time-base-time</I> mapping that <i>Started</i>
     * the <code>Controller</code>
     * 
     * @param from
     *            The <code>Controller</code> that has <I>Started</I>.
     * @param mediaTime
     *            The media time when the <code>Controller</code>
     *            <I>Started</I>.
     * @param tbTime
     *            The time-base time when the <code>Controller</code>
     *            <I>Started</I>.
     * 
     */
    public StartEvent(Controller from, int previous, int current, int target, Time mediaTime, Time tbTime)
    {
        super(from, previous, current, target);
        this.mediaTime = mediaTime;
        this.timeBaseTime = tbTime;
    }

    /**
     * Get the clock time (<I>media time</I>) when the <code>Controller</code>
     * started.
     * 
     * @return The <code>Controller's</code>&nbsp;<I>media time</I> when it started.
     */
    public Time getMediaTime()
    {
        return mediaTime;
    }

    /**
     * Get the time-base time that started the <code>Controller</code>.
     * 
     * @return The <I>time-base time</I> associated with the
     *         <code>Controller</code> when it started.
     */
    public Time getTimeBaseTime()
    {
        return timeBaseTime;
    }

    /**
     * Returns the String representation of this event's values.
     */
    public String toString()
    {
        return getClass().getName() + "[source=" + eventSrc + ",previous=" + stateName(previousState) + ",current="
                + stateName(currentState) + ",target=" + stateName(targetState) + ",mediaTime=" + mediaTime
                + ",timeBaseTime=" + timeBaseTime + "]";
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
