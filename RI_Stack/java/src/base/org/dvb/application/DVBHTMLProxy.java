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

/**
 * A <code>DVBHTMLProxy</code> Object is a proxy to a DVBHTML application.
 */
public interface DVBHTMLProxy extends AppProxy
{
    /**
     * The application is in the loading state.
     * 
     * @since MHP 1.0.2
     */
    public static final int LOADING = 6;

    /**
     * The application is in the killed state.
     * 
     * @since MHP 1.0.2
     */
    public static final int KILLED = 7;

    /**
     * Loads the initial entry page of the application and waits for a signal.
     * This method mimics the PREFETCH control code and is intended to be called
     * instead of and not as well as start. Calling prefetch on a started
     * application will have no effect.
     * 
     * @throws SecurityException
     *             if the calling application does not have permission to start
     *             applications
     * @since MHP1.0
     */
    public void prefetch();

    /**
     * Sends the application a start trigger at the specified time.
     * 
     * @param starttime
     *            the specified time to send a start trigger to the application.
     *            If the time has already passed the application manager shall
     *            send the trigger immediately. Dates pre-epoch shall always
     *            cause the application manager to send the trigger immediately.
     * @throws SecurityException
     *             if the calling application does not have permission to start
     *             applications
     * 
     * @since MHP1.0
     */
    public void startTrigger(java.util.Date starttime);

    /**
     * Sends the application a trigger with the given payload at the specified
     * time.
     * 
     * @param time
     *            the specified time to send a start trigger to the application.
     *            If the time has already passed the application manager should
     *            send the trigger immediately. Dates pre-epoch shall always
     *            cause the application manager to send a 'now' trigger.
     * 
     * @param triggerPayload
     *            the specified payload to deliver with the trigger. The payload
     *            is specified as object, but this will be refined once DVB-HTML
     *            Triggers are properly defined.
     * @throws SecurityException
     *             if the calling application does not have permission to start
     *             applications
     * 
     * @since MHP1.0
     */
    public void trigger(java.util.Date time, Object triggerPayload);
}
