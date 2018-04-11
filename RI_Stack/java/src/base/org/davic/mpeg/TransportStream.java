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

package org.davic.mpeg;

/**
 * This class represents an MPEG-2 Transport Stream with its associated Service
 * Information (SI) as known to a decoder.
 * <p>
 * A TransportStream object models a specific transport stream that can be
 * accessed through a specific network interface. This implies that a Transport
 * Stream object has implicitly a connection to a specific network interface.
 * Thus, if two or more network interfaces can deliver the same transport
 * stream, this will be modeled by two or more separate TransportStream objects.
 * A network interface does not need to be tuned to a transport stream if an
 * application wants to use the corresponding TransportStream object.
 * <p>
 * If the corresponding network interface is currently tuned to the
 * TransportStream, then an application can query the TransportStream for the
 * services it contains, and the services for which elementary streams it
 * contains. If the corresponding network interface is not currently tuned to
 * the TransportStream, then the application can query the TransportStream for
 * the services it contains. If the STB has cached the required information it
 * can return it, otherwise it should return null. If an application queries a
 * Service object for elementary stream information and the corresponding
 * TransportStream is not currently tuned to, then the Service object should
 * return null.
 * <p>
 * If an application has two references to a TransportStream object and those
 * TransportStream objects model the same transport stream coming from the same
 * network interface, then the equals() method (inherited from java.lang.Object)
 * return true when comparing both TransportStream objects. The references
 * themselves are not necessarily the same, although they may be.
 * <p>
 * Note: If an application wants to know to which network interface a
 * TransportStream object is connected to, it should use the Tuning API if it is
 * available.
 * <p>
 * 
 * @version updated to DAVIC 1.3.1
 */

public abstract class TransportStream
{

    protected TransportStream()
    {
    }

    /**
     * @return the transport_stream_id of this transport stream.
     */
    public int getTransportStreamId()
    {
        return 0;
    }

    /**
     * @param serviceId
     *            the id of the requested service within this transport stream
     * @return a reference to the service object that represents the service
     *         from which this MPEG-2 TS is accessed. If the required
     *         information is not available or the indicated service does not
     *         exist, null is returned.
     * 
     * @version updated to DAVIC 1.3.1
     */
    public Service retrieveService(int serviceId)
    {
        return null;
    }

    /**
     * @return the array of all service objects belonging to this transport
     *         stream. When the required information is not available null is
     *         returned.
     * @version updated to DAVIC 1.3.1
     */
    public Service[] retrieveServices()
    {
        return new Service[1];
    }

    /*
     * @return the time the information contained in the transport stream object
     * was last updated from a live transport stream. If the transport stream is
     * currently live, it returns null.
     */
    /*
     * public java.util.Date getUpdateTime() { return new java.util.Date(); }
     */
}
