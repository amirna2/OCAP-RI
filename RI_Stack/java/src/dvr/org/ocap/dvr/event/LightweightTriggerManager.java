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

package org.ocap.dvr.event;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerManagerImpl;

/**
 * This class represents a manager that can be used by a privileged application
 * to create an artificial object carousel containing a DSMCCStreamEvent in the
 * top level. The DSMCCStreamEvent can be populated by a privileged application.
 *
 * NOTE this is an expanded version of the GEM lightweight binding of the
 * trigger API (GEM clause P.2.3.1 Lightweight binding of trigger API).
 */
public abstract class LightweightTriggerManager
{

    /**
     * Protected constructor not callable by applications.
     */
    protected LightweightTriggerManager()
    {
    }

    /**
     * Gets an instance of the manager.
     *
     * @return An instance of the light-weight trigger manager.
     */
    public static LightweightTriggerManager getInstance()
    {
        return LightweightTriggerManagerImpl.getInstance();
    }

    /**
     * Registers a handler interested in services with streams listed in the
     * PMT with this stream type.
     * <p>
     * A separate notification of the handler SHALL be made for each service
     * selection (with or without timeshift enabled), recording, buffering
     * request, or tune that references a service containing streams of the given
     * stream type.
     * The provided <code>LightweightTriggerSession</code> SHALL reflect the relevant
     * <code>ServiceContext</code>, <code>RecordingRequest</code>,
     * <code>BufferingRequest</code>, or <code>NetworkInterface</code>.
     *
     * @param handler Handler to register.
     * @param streamType a stream type as signaled in the PMT.
     *
     * @throws IllegalArgumentException if streamType is not in the
     *      range 0x0 to 0xFF.
     * @throws NullPointerException if handler is null.
     * @throws SecurityException if the calling application is not
     *      signed.
     */
    public abstract void registerHandler(LightweightTriggerHandler handler, short streamType);

    /**
     * Unregisters a handler that was previously registered by the
     * registerHandler method.
     *
     * @param handler
     *            The handle to unregister.
     *
     * @throws IllegalArgumentException
     *             if the handler was not previously registered.
     */
    public abstract void unregisterHandler(LightweightTriggerHandler handler);
}
