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

package org.ocap.environment;

import org.dvb.application.AppID;

/**
 * The <code>EnvironmentStateChangedEvent</code> class indicates the completion
 * of a state transition of an environment. The following steps SHALL happen
 * before a change of selected environment is reported as completed:
 * <ul>
 * <li>the resource policy SHALL be changed to that of the new selected
 * environment
 * <li>applications in the environment not allowed to run in its new state SHALL
 * have been killed
 * <li>applications in the environment allowed to run but not allowed to show a
 * user interface in its new state SHALL have that UI hidden, for OCAP-J
 * applications this means the HScene SHALL be hidden with the same result as a
 * call to setVisible(false).
 * <li>reservations of application exclusive events for the old applications
 * which are allowed to run in the new selected environment SHALL have been
 * cancelled
 * <li>if the newly selected environment has previously been in the selected or
 * presenting state then all previously terminated applications have been
 * re-started as {@link Environment described} as part of the transition to the
 * selected state (where re-started for Xlets mean the call to the initXlet
 * method has completed)
 * <li>returning to normal mode any running applications which are in
 * cross-environment mode, background mode or paused mode
 * <li>returning to the active state any pauseable applications which were
 * paused when this environment last stopped being selected and which are still
 * paused
 * </ul>
 * Reporting a change of selected environment as having been completed SHALL NOT
 * wait for the following steps:
 * <ul>
 * <li>completion of the re-starting of applications (where completion for Xlets
 * means completion of calls to the startXlet method)
 * <li>requesting HScenes be visible
 * <li>requesting focus
 * </ul>
 * When any screen re-draws happen is implementation dependent and may be
 * deferred until the new applications are ready to redraw themselves.
 */
public class EnvironmentStateChangedEvent extends EnvironmentEvent
{
    private EnvironmentState m_fromState;

    private EnvironmentState m_toState;

    /**
     * Create an EnvironmentStateChangedEvent object.
     * 
     * @param source
     *            the <code>Environment</code> where the state transition
     *            happened
     * @param fromstate
     *            the state the environment was in before the state transition *
     *            was requested
     * @param tostate
     *            the state the environment is in after the completion of the
     *            state transition
     */
    public EnvironmentStateChangedEvent(Environment source, EnvironmentState fromstate, EnvironmentState tostate)
    {
        super(source);
        m_fromState = fromstate;
        m_toState = tostate;
    }

    /**
     * Return the state the environment was in before the state transition was
     * requested as passed to the constructor of this event.
     * 
     * @return the old state
     */
    public EnvironmentState getFromState()
    {
        return m_fromState;
    }

    /**
     * Return the state the environment is in after the completion of the state
     * transition as passed to the constructor of this event.
     * 
     * @return the new state
     */
    public EnvironmentState getToState()
    {
        return m_toState;
    }
}
