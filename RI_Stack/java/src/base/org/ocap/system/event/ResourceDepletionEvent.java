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

/*
 * ResourceDepletionEvent.java
 */

package org.ocap.system.event;

/**
 * Event that indicates resources are low, and the system is about to destroy
 * application(s) to attempt to correct this.
 */
public class ResourceDepletionEvent extends SystemEvent
{
    /**
     * Overall system memory is depleted to an implementation specific
     * threshold. This indicates that the platform's application manager is
     * about to destroy an application to free system memory.
     */
    public final static int RESOURCE_SYS_MEM_DEPLETED = BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES;

    /**
     * VM memory for an application is depleted to an implementation specific
     * threshold. This indicates that the platform's application manager is
     * about to destroy an application to free VM memory.
     */
    public final static int RESOURCE_VM_MEM_DEPLETED = BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES + 1;

    /**
     * Available CPU cycles is depleted to an implementation specific threshold.
     * This indicates that the platform's application manager is about to
     * destroy an application to free CPU cycles.
     * <p>
     * Note that the presence of this event type does not imply that the
     * platform's application manager must destroy applications if CPU usage is
     * too high; merely that if it does then it must first send this event.
     */
    public final static int RESOURCE_CPU_BANDWIDTH_DEPLETED = BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES + 2;

    /**
     * Available reverse channel bandwidth is depleted to implementation
     * specific threshold. This indicates that the platform's application
     * manager is about to destroy an application to free return channel
     * bandwidth.
     * <p>
     * Note that the presence of this event type does not imply that the
     * platform's application manager must destroy applications if return
     * channel bandwidth usage is too high; merely that if it does then it must
     * first send this event.
     */
    public final static int RESOURCE_RC_BANDWIDTH_DEPLETED = BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES + 3;

    /**
     * System event constructor assigns an eventId, Date, and
     * ApplicationIdentifier.
     * 
     * @param typeCode
     *            - Unique event type.
     * @param message
     *            - Readable message specific to the event generator.
     * 
     * @throws IllegalArgumentException
     *             if the typeCode is not in a defined application range when
     *             the event is created by an application. Since there are no
     *             defined application ranges for resource depletion events,
     *             this exception will always be thrown if this constructor is
     *             called by an application.
     */
    public ResourceDepletionEvent(int typeCode, String message) throws IllegalArgumentException
    {
        super(typeCode, message);
    }

    /**
     * This constructor is provided for internal use by OCAP implementations;
     * applications SHOULD NOT call it.
     * 
     * @param typeCode
     *            - The unique error type code.
     * @param message
     *            - Readable message specific to the event generator.
     * @param date
     *            - Event date in milli-seconds from midnight January 1, 1970
     *            GMT.
     * @param appId
     *            - The Id of the application logging the event.
     * 
     * @throws SecurityException
     *             if this constructor is called by any application.
     */
    public ResourceDepletionEvent(int typeCode, String message, long date, org.dvb.application.AppID appId)
    {
        super(typeCode, message, date, appId);
    }

    /**
     * Returns the name of this event. Used by the implementation of
     * {@link #privToString}.
     * 
     * @return the name of this event.
     * @see #privToString.
     */
    String privNameToString()
    {
        return "ResourceDepletionEvent";
    }
}
