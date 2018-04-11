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

import javax.media.MediaLocator;
import javax.media.protocol.DataSource;
import javax.tv.service.Service;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.presentation.Presentation;

/**
 * This interface defines player implementation methods that are exposed to the
 * rest of the implementation..
 * 
 * @author schoonma
 */
public interface Player extends javax.media.Player, AlarmClock
{
    /**
     * Get the associated data source.
     * 
     * @return Returns the {@link DataSource} assigned to the {@link Player}.
     */
    DataSource getSource();

    /**
     * Get the {@link CallerContext} that owns the player.
     * 
     * @return Returns the {@link CallerContext} of the application that "owns"
     *         the player&mdash;i.e., the application that caused the player to
     *         be created. In the case of a player created by a
     *         {@link javax.tv.service.selection.ServiceContext ServiceContext},
     *         this is the application that called
     *         {@link javax.tv.service.selection.ServiceContext#select(Service)}
     *         that resulted in the player being created. In the case of a
     *         player created directly by an application that calls
     *         {@link javax.media.Manager#createPlayer(MediaLocator)}, it is the
     *         {@link CallerContext} of the calling application.
     */
    CallerContext getOwnerCallerContext();

    /**
     * Get the application priority of the owner of the player.
     * 
     * @return Returns the priority of the application that owns the player. If
     *         a priority cannot be obtained (from the owner's CallerContext),
     *         then -1 is returned.
     */
    int getOwnerPriority();

    /**
     * Get the current presentation.
     * 
     * @return the current {@link Presentation} instance.
     */
    Presentation getPresentation();
}
