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

/**
 * This class models a return channel network interface for use in receiving and
 * transmitting IP packets over a logical return channel. This can include real
 * analog modems, cable return channel and all the other options allowed by the
 * relevant DVB specification. This class does not model any concept of
 * connection. Hence interfaces represented by this class and not by a sub-class
 * of it are permanently connected.
 *
 * @ocap TYPE_CATV is the only supported return channel type in OCAP.
 */
public class RCInterface
{
    /**
     * Constant to indicate a PSTN return channel.
     */
    public static final int TYPE_PSTN = 1;

    /**
     * Constant to indicate an ISDN return channel.
     */
    public static final int TYPE_ISDN = 2;

    /**
     * Constant to indicate a DECT return channel.
     */
    public static final int TYPE_DECT = 3;

    /**
     * Constant to indicate a CATV return channel.
     */
    public static final int TYPE_CATV = 4;

    /**
     * Constant to indicate a LMDS return channel.
     */
    public static final int TYPE_LMDS = 5;

    /**
     * Constant to indicate a MATV return channel.
     */
    public static final int TYPE_MATV = 6;

    /**
     * Constant to indicate a DVB-RCS return channel.
     */
    public static final int TYPE_RCS = 7;

    /**
     * Constant to indicate an unknown return channel technology. There is an
     * intermediate physical interface between the MHP terminal and the return
     * channel device. This return value gives no information about whether the
     * return channel is connection oriented or connectionless.
     */
    public static final int TYPE_UNKNOWN = 8;

    /**
     * Constant to indicate all other return channel technologies not having a
     * suitable defined constant in this class.
     * <p>
     * NOTE: DVB does not intend to add future constants to this list for future
     * return channel technologies. These should be represented as TYPE_OTHER.
     */
    public static final int TYPE_OTHER = 9;

    /**
     * Constructor for instances of this class. This constructor is provided for
     * the use of implementations and specifications which extend this
     * specification. Applications shall not define sub-classes of this class.
     * Implementations are not required to behave correctly if any such
     * application defined sub-classes are used.
     */
    protected RCInterface()
    {
    }

    /**
     * Return the maximum data rate of the connection over the immediate access
     * network to which this network interface is connected. For asymmetric
     * connections, the data rate coming into the MHP terminal shall be
     * returned. For connection oriented interfaces which are not currently
     * connected, the value returned shall be that of the last connection
     * established where that information is available. Where that information
     * is not available, (e.g. where no connection has been established since an
     * MHP terminal was power cycled), -1 shall be returned.
     *
     * @return a data rate in KBaud or -1 where this is not available
     * @since MHP 1.0.1
     */
    public int getDataRate()
    {
        // this method should not be directly called - the OCRCInterfaceImpl
        // subclassed version should be called instead
        return -1;
    }

    /**
     * Return the type of return channel represented by this object. Note,
     * applications wishing to discover whether a return channel interface is
     * connection oriented or not are recommended to test whether an object is
     * an instance of <code>ConnectionRCInterface</code> or not. A
     * non-connection oriented interface really means a permanently connected
     * return channel.
     *
     * @return the type of return channel represented by this object encoded as
     *         one of the constants defined in this class
     */
    public int getType()
    {
        // this method should not be directly called - the OCRCInterfaceImpl
        // subclassed version should be called instead
        return -1;
    }
}
