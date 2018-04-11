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

package org.cablelabs.impl.ocap.hardware;

import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hardware.Host;
import org.ocap.hardware.PowerModeChangeListener;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.event.RebootEvent;

/**
 * Defines additional methods provided by the {@link Host} implementation.
 * 
 * @author Aaron Kamienski
 * @author Alan Cossitt (DSExt)
 */
public interface ExtendedHost
{
    /**
     * Audio mode constant for normal "on" mode.
     */
    public final static int AUDIO_ON = 1;
    
    /**
     * Audio mode constant for muted audio.
     */
    public final static int AUDIO_MUTED = 2;
    
    /**
     * This method may be used to force a reboot without consulting the
     * currently installed {@link RebootEvent reboot event handler}.
     */
    public void forceReboot();

    /**
     * This method may be used to request a reboot giving a specific reason.
     * Invoking <code>reboot(RebootEvent.REBOOT_BY_TRUSTED_APP)</code> is
     * equivalent to calling {@link Host#reboot()}.
     * 
     * @param reason
     *            one of {@link RebootEvent#REBOOT_BY_IMPLEMENTATION},
     *            {@link RebootEvent#REBOOT_BY_TRUSTED_APP},
     *            {@link RebootEvent#REBOOT_FOR_UNRECOVERABLE_HW_ERROR}, or
     *            {@link RebootEvent#REBOOT_FOR_UNRECOVERABLE_SYS_ERROR}
     */
    public void reboot(int reason);

    /**
     * Transition the audio mode of the system to the given mode.
     * <p>
     * 
     * If the audio mode is already in the target mode, this method SHALL do
     * nothing.
     * 
     * @param mode 
     *              The new audio mode for the system.
     * 
     * @throws IllegalArgumentException
     *             if <i>mode</i> is not one of AUDIO_ON or AUDIO_MUTED
     */
    public void setAudioMode(int mode) throws IllegalArgumentException; 
    
    /**
     * Retrieves the current audio state of the box.
     * 
     * @return AUDIO_ON if audio is not muted, AUDIO_MUTED if audio is muted.
     * 
     * @see #AUDIO_ON
     * @see #AUDIO_MUTED
     */
    public int getAudioMode();
}
