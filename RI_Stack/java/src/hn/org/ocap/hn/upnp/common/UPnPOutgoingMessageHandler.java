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

package org.ocap.hn.upnp.common;

import java.net.InetSocketAddress;


/**
 * This interface represents an outgoing message handler that can
 * monitor and modify any outgoing messages from the UPnP stack.
 * All messages originating from the UPnP stack go through the
 * handler (if the handler is registered). This includes
 * advertisements, action responses and device, service and icon
 * retrieval responses on a server, UPnP action invocations,
 * subscription requests, device searches and device, service
 * and icon retrieval requests on a client.
 */
public interface UPnPOutgoingMessageHandler
{
    /**
     * Handles an outgoing message.  The primary responsibility is
     * to process the provided {@code UPnPMessage} object and produce a
     * composite byte array for the outbound message that complies with
     * the UPnP Device Architecture specification.
     * An application-provided {@code UPnPOutgoingMessageHandler} may invoke
     * the default, stack-provided message handler via the specified
     * {@code defaultHandler}.
     * The handler may also cause the outgoing message to be discarded by
     * returning {@code null}; the message SHALL NOT be sent,
     * and subsequent processing SHALL continue as if any expected response
     * to the message had never been received.
     * <p>
     * Note that if the {@code UPnPMessage} provided to this method contains
     * an HTTP {@code CONTENT-LENGTH} header, its value is undefined.
     * The handler must supply the correct value after "stringifying"
     * the XML document provided by {@link UPnPMessage#getXML()} into
     * XML data to be carried in returned byte array.
     * See {@link UPnPMessage#getHeaders()}.
     *
     * @param address The InetSocketAddress to which the message is to be sent.
     *  
     * @param message The UPnP message that is to be sent.
     *  
     * @param defaultHandler The default stack-provided outgoing
     *                       message handler.
     *                       If this {@code UPnPOutgoingMessageHandler} is
     *                       the default outgoing message handler, this
     *                       parameter SHALL be ignored.
     *  
     * @return Composite output byte array containing HTTP start line,
     *         headers, and body of the message. No further
     *         processing or parsing is performed by the stack
     *         on the output byte array prior to transmission. The
     *         handler can cause the outgoing message to be
     *         discarded by returning null.
     *
     */
     public byte[] handleOutgoingMessage(InetSocketAddress address,
                                        UPnPMessage message,
                                        UPnPOutgoingMessageHandler defaultHandler);
}

