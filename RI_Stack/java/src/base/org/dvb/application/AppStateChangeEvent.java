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

package org.dvb.application;

import java.util.*;

/**
 * The <code>AppStateChangeEvent</code> class indicates a state transition of
 * the application. These events are only generated for running applications or
 * for non-running applications where an attempt to control the application fails.
 * If the state transition was requested by an application
 * through this API, the method <code>hasFailed</code> indicates whether the
 * state change failed or not. Where a state change succeeds,
 * <code>fromState</code> and <code>toState</code> shall indicate the original
 * and destination state of the transition. If it failed, <code>fromState</code>
 * shall return the state the application was in before the state transition was
 * requested and the <code>toState</code> method shall return the state the
 * application would have been in if the state transition had succeeded.
 * <p>
 * Attempting to start an application which is already in the active state shall fail
 * and generate an <code>AppStateChangeEvent</code> with <code>hasFailed</code>
 * returning true and both fromstate and tostate being <code>STARTED</code>.
 *
 * @since MHP1.0
 */
public class AppStateChangeEvent extends EventObject
{

    /**
     * Create an AppStateChangeEvent object.
     *
     * @param appid
     *            a registry entry representing the tracked application
     * @param fromstate
     *            the state the application was in before the state transition
     *            was requested, where the value of fromState is one of the
     *            state values defined in the AppProxy interface or in the
     *            interfaces inheriting from it
     * @param tostate
     *            state the application would be in if the state transition
     *            succeeds, where the value of toState is one of the state
     *            values defined in the AppProxy interface or in the interfaces
     *            inheriting from it
     * @param hasFailed
     *            an indication of whether the transition failed (true) or
     *            succeeded (false)
     * @param source
     *            the <code>AppProxy</code> where the state transition happened
     */
    public AppStateChangeEvent(AppID appid, int fromstate, int tostate, Object source, boolean hasFailed)
    {
        super(source);
        this.appid = appid;
        this.fromState = fromstate;
        this.toState = tostate;
        this.hasFailed = hasFailed;
    }

    /**
     * The application the listener was tracking has made a state transition
     * from <code>fromState</code> to <code>toState</code>.
     * <p>
     *
     * @return a registry entry representing the tracked application
     * @since MHP1.0
     */
    public AppID getAppID()
    {
        return appid;
    }

    /**
     * The application the listener is tracking was in<code>fromState</code>,
     * where the value of fromState is one of the state values defined in the
     * AppProxy interface or in the interfaces inheriting from it.
     *
     * @return the old state
     * @since MHP1.0
     */
    public int getFromState()
    {
        return fromState;
    }

    /**
     * If the <code>hasFailed</code> method returns false, then the application
     * the listener is tracking is now in <code>toState</code>. If the
     * <code>hasFailed</code> method returns true, then the <code>toState</code>
     * is the state where the state transition was attempted to but the
     * transition failed. The value of <code>toState</code> is one of the state
     * values defined in the <code>AppProxy</code> interface or in the
     * interfaces inheriting from it.
     *
     * @return the intended or actual new state
     * @since MHP1.0
     */
    public int getToState()
    {
        return toState;
    }

    /**
     * This method determines whether an attempt to change the state of an
     * application has failed.
     *
     * @return true if the attempt to change the state of the application
     *         failed, false otherwise
     * @since MHP1.0
     */
    public boolean hasFailed()
    {
        return hasFailed;
    }

    private boolean hasFailed;

    private final AppID appid;

    private final int fromState, toState;
}
