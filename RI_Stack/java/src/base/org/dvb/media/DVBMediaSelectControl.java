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

package org.dvb.media;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.locator.Locator;

/**
 * DVBMediaSelectControl extends <code>MediaSelectControl</code> allowing the
 * selection of different kinds of content in a running <code>Player</code>. The
 * extension is to allow the selection in a single operation of all the media
 * service components in a service without needing knowledge about which media
 * service components are present in that service.
 *
 * @see javax.tv.media.MediaSelectControl
 * @since MHP 1.0.2
 */

public interface DVBMediaSelectControl extends javax.tv.media.MediaSelectControl
{
    /**
     * Selects for presentation the media service components from a service. If
     * some content is currently playing, it is replaced in its entirety by the
     * media service components from the specified service. This is an
     * asynchronous operation that is completed upon receipt of a
     * MediaSelectEvent. Note that for most selections that imply a different
     * time base or otherwise change synchronization relationships, a
     * RestartingEvent will be posted by the Player. The rules for deciding
     * which media service components shall be presented are defined in the main
     * body of the present document.
     *
     * @param l
     *            the locator for a service
     * @throws InvalidLocatorException
     *             If the locator provided does not reference a service.
     * @throws InvalidServiceComponentException
     *             If the locator provided does not reference a service which
     *             contains at least one media service component
     * @throws InsufficientResourcesException
     *             If the operation cannot be completed due to a lack of system
     *             resources.
     */
    public void selectServiceMediaComponents(Locator l) throws InvalidLocatorException,
            InvalidServiceComponentException, InsufficientResourcesException;

}
