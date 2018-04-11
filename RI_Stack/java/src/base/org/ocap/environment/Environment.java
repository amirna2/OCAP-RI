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

import org.cablelabs.impl.manager.EnvironmentManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * Represents an environment that provides the context in which applications
 * run. <h4>Environment state machine</h4> Environments SHALL be in one of four
 * states; inactive, selected, presenting and background. These are defined as
 * follows;
 * <ul>
 * <li>Environments in the inactive state SHALL have no running applications at
 * all.
 * <li>Environments in the selected state have all applications running to the
 * maximum extent possible and able to interact with the end-user.
 * <li>Environments in the presenting state may have running applications which
 * are visible to the end-user but these SHALL NOT be able to receive input from
 * the remote.control
 * <li>Environments in the background state may have running applications but
 * these applications SHALL NOT be in the normal mode.
 * </ul>
 * <h4>Transitions from selected to any other state and back</h4> When the OCAP
 * environment leaves the selected state, all assumable modules provided by the
 * cable MSO SHALL be disabled and the manufacturer original enabled. When the
 * OCAP environment enters the selected state, assumable modules provided by the
 * cable MSO that were disabled SHALL be re-enabled. <h4>Transitions from
 * selected or presenting to background</h4> When an environment changes state
 * from either selected or presenting to background, the following SHALL apply:
 * <ul>
 * <li>applications able to run in cross-environment mode SHALL be put in
 * cross-environment mode by the implementation
 * <li>applications able to run in background mode SHALL be put in background
 * mode by the implementation
 * <li>applications signaled as pauseable MAY be put in the paused state by the
 * implementation
 * <li>all other applications SHALL be terminated
 * <li>The implementation SHALL hide the user interfaces of applications which
 * are put in background or paused mode or which are terminated. HScene
 * instances shall have their visibility set to false.
 * <p>
 * NOTE: Need to specify if any events are generated when this happens.
 * </ul>
 * <h4>Transitions from background to selected or presenting</h4> When an
 * environment changes state from background to either selected or presenting,
 * the following SHALL apply:
 * <ul>
 * <li>all auto-start unbound applications which were terminated due to their
 * environment going into the background state SHALL be started if their service
 * is still selected
 * <li>all auto-start bound applications which were terminated due to their
 * environment going into the background state SHALL be started if still
 * signaled in a selected service
 * <li>all applications from the newly selected environment that are running in
 * cross-environment or background mode SHALL be returned to normal mode and
 * restrictions on them as a consequence of them running in those modes SHALL be
 * lifted
 * <li>visible user interfaces of cross-environment applications whose
 * environment becomes selected SHALL continue to remain visible
 * <li>the policy of the newly selected environment is responsible for
 * determining which of the applications in that environment should be the first
 * to have focus.
 * <li>Any pauseable applications which were paused when this environment went
 * into the background state and which are still paused shall be returned to the
 * active state
 * </ul>
 */

public abstract class Environment
{

    /**
     * Constructor for environments. This is provided for the use of
     * implementations or other specifications and is not to be used by
     * applications.
     */
    protected Environment()
    {
    }

    /**
     * Return the calling applications home environment
     * 
     * @return an environment
     */
    public static Environment getHome()
    {
        EnvironmentManager env = (EnvironmentManager) ManagerManager.getInstance(EnvironmentManager.class);
        return env.getOcapEnvironment();
    }

    /**
     * Add a listener for environment events.
     * 
     * @param l
     *            the listener to add
     */
    public void addEnvironmentListener(EnvironmentListener l)
    {
    }

    /**
     * Remove a listener for environment events.
     * 
     * @param l
     *            the listener to remove
     */
    public void removeEnvironmentListener(EnvironmentListener l)
    {
    }

    /**
     * Queries the state of this environment.
     * 
     * @return the state of this environment
     */
    public EnvironmentState getState()
    {
        return null;
    }

    /**
     * Request this environment become selected. This call is asynchronous and
     * completion SHALL be reported with an EnvironmentEvent being sent to
     * registered EnvironmentListeners.
     * <p>
     * This request SHALL be unconditionally granted except under the following
     * circumstances.
     * <ul>
     * <li>if a deadlock is detected with two or more environments repeatedly
     * requesting they be selected each time they become de-selected.
     * Implementations MAY include logic to detect this situation if it happens
     * and refuse to change selected environment after an implementation
     * specific number of changes in an implementation specific period.
     * <li>if this environment is in the presenting state due to it running in a
     * PiP or PoP session and making this environment selected is not permitted
     * by a PiP control mechanism on the OCAP host device
     * </ul>
     * 
     * @throws java.lang.IllegalStateException
     *             if a state change is already in progress for this environment
     *             or if the request fails for one of the circumstances defined
     *             above
     * @throws SecurityException
     *             if and only if the calling application does not have
     *             <code>MonitorAppPermission("environment.selection")</code>
     */
    public void select()
    {
    }

    /**
     * Request this environment cease being selected.
     * <p>
     * NOTE It is implementation dependent which environment becomes selected
     * when this call is used.
     * 
     * @throws SecurityException
     *             if and only if the calling application does not have
     *             <code>MonitorAppPermission("environment.selection")</code>
     */
    public void deselect()
    {
    }
}
