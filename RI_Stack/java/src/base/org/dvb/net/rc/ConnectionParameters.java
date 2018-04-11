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

package org.dvb.net.rc;

import java.net.InetAddress;

/**
 * This class encapsulates the parameters needed to specify the target of a
 * connection.
 * 
 * @ocap The only RCInterface type supported by OCAP is TYPE_CATV. No connection
 *       oriented interfaces (e.g. ConnectionRCInterface) are supported.
 *       Therefore, this class will never be used within the implementation.
 */

public class ConnectionParameters
{
    /**
     * Construct a set of connection parameters. Details of the DNS server to
     * use are supplied by the server.
     * 
     * @param number
     *            the target of the connection, e.g. a phone number
     * @param username
     *            the username to use in connection setup
     * @param password
     *            the password to use in connection setup
     */
    public ConnectionParameters(String number, String username, String password)
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, no default implementation within this OCAP
         * stack.
         */
    }

    /**
     * Construct a set of connection parameters.
     * 
     * @param number
     *            the target of the connection, e.g. a phone number
     * @param username
     *            the username to use in connection setup
     * @param password
     *            the password to use in connection setup
     * @param dns
     *            the list of DNS servers to try before reporting failure. The
     *            order in which they are interrogated is not specified. Once
     *            one result has been obtained, there is no requirement to try
     *            others.
     */
    public ConnectionParameters(String number, String username, String password, InetAddress[] dns)
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, no default implementation within this OCAP
         * stack.
         */
    }

    /**
     * Return the target of this connection for example a phone number. The
     * value returned shall be the one passed into the constructor of this
     * instance.
     * 
     * @return the target of the connection
     */
    public String getTarget()
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, this class will never be used within the
         * implementation.
         */
        return null;
    }

    /**
     * Return the username used in establishing this connection The value
     * returned shall be the one passed into the constructor of this instance.
     * 
     * @return the username used in establishing the connection
     */
    public String getUsername()
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, this class will never be used within the
         * implementation.
         */
        return null;
    }

    /**
     * Return the password used in establishing this connection The value
     * returned shall be the one passed into the constructor of this instance.
     * 
     * @return the password used in establishing this connection
     */
    public String getPassword()
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, this class will never be used within the
         * implementation.
         */
        return null;
    }

    /**
     * Return the addresses of the DNS servers to use for the connection
     * 
     * @return return the addresses of the DNS servers passed into the
     *         constructor of the instance or null if none was provided.
     */
    public InetAddress[] getDNSServer()
    {
        /*
         * The only RCInterface type supported by OCAP is TYPE_CATV. No
         * connection oriented interfaces (e.g. ConnectionRCInterface) are
         * supported. Therefore, this class will never be used within the
         * implementation.
         */
        return null;
    }
}
