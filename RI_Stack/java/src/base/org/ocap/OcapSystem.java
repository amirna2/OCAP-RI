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

package org.ocap;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapTestManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.util.SecurityUtil;

import java.io.IOException;

import org.ocap.system.MonitorAppPermission;

/**
 * This class provides system utility functions.
 *
 * @author Brent Foust
 * @author Frank Sandoval
 * @author Shigeaki Watanabe
 *
 * @author Brent Thompson (Vidiom)
 */
public final class OcapSystem
{
    /**
     * Do not use. This is to prevent a public constructor from being generated.
     */
    private OcapSystem()
    {
    }

    /**
     * Called by the monitor application to inform the OCAP implementation that
     * it is configuring and the boot process may resume after it calls the
     * <code>monitorConfiguredSignal</code> method, see Section 20.2.2.3 Boot
     * Process while connected to the cable network � CableCARD device present. <br>
     * On invocation of this method, the APIs used for conformance testing,
     * specifically, <code>org.ocap.test.OCAPTest</code> SHALL be initialized
     * for use. This means that the implementation SHALL perform the following
     * actions:
     * <ul>
     * <li>a. Open a socket for receiving UDP datagrams on a port, the value of
     * which is specified in the <code>port</code> parameter passed to this
     * method.</li>
     * <li>b. Wait to receive a datagram that contains a string formatted thus:
     * <code>ate:a.b.c.d:xxxx:ppp</code> (string may be null-terminated), where
     * 'a.b.c.d' represents an IPv4 address, and 'xxxx' represents an IP port
     * number, and 'ppp' represents protocol type ('TCP' for TCP/IP and 'UDP'
     * for UDP/IP). Any received datagrams which do not contain a properly
     * formatted payload string SHALL be ignored. Once a datagram with a
     * properly formatted string has been received, the datagram socket SHALL be
     * closed.</li>
     * <li>c. Attempt to establish a TCP or UDP socket connection to the test
     * system using the IPv4 address and port number obtained in b. The protocol
     * type for the socket connection is specified by 'ppp' string in b. This
     * connected socket SHALL be used solely to transmit and receive data
     * originating from the <code>org.ocap.test.OCAPTest</code> APIs and SHALL
     * NOT be accessible to applications through other APIs. The TCP or UDP
     * socket connection shall have a timeout of 'infinite'. If this method does
     * not complete within the specified timeout period, an
     * <code>IOException</code> SHALL be thrown.</li>
     * <li>d. Return control to the caller.</li>
     * </ul>
     * <br>
     * If this method is called with both the <code>port</code> and
     * <code>timeout</code> parameters set to 0, then the OCAP implementation
     * SHALL not enable the conformance testing APIs, which SHALL just return
     * silently, without performing any action. <br>
     * If the monitor application does not call this method in the time
     * specified in section 20.2.2.3 Boot Process while connected to the cable
     * network - CableCARD device present, then the implementation SHALL
     * behave the same as if this method had been called with 0 specified for
     * both the <code>port</code> and <code>timeout</code> parameters.
     *
     * @param port
     *            the IP port number to listen for datagrams from the test
     *            system on.
     *
     * @param timeout
     *            the time, in seconds to allow for a communications channel to
     *            be established with the test system.
     *
     * @throws SecurityException
     *             if application does not have
     *             MonitorAppPermission("signal configured").
     *
     * @throws IOException
     *             if a communications channel cannot be established with the
     *             test system within the amount of time specified by the
     *             <code>timeout</code> parameter.
     */
    public static void monitorConfiguringSignal(int port, int timeout) throws IOException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("signal.configured"));

        // signal that monitor app is being configured
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        sm.getServicesDatabase().notifyMonAppConfiguring();

        // only setup conformance test socket if port & timeout aren't both 0
        if ((port != 0) || (timeout != 0))
        {
            OcapTestManager oti = (OcapTestManager) ManagerManager.getInstance(OcapTestManager.class);
            oti.setup(port, timeout);
        }
    }

    /**
     * Called by the Initial Monitor Application to inform the OCAP
     * implementation it has completed its configuration process and that the
     * boot processing may resume. It is recommended that the monitor call this
     * method as soon as possible after the
     * <code>monitorConfiguringSignal</code> method has been called.
     */
    public static void monitorConfiguredSignal()
    {
        // signal that monitor app is configured
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        sm.getServicesDatabase().notifyMonAppConfigured();
    }

}
