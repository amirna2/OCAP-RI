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
 * This interface represents an incoming message handler that can
 * monitor and modify any incoming messages to the UPnP stack.
 * All messages targeting the UPnP stack go through the handler
 * (if the handler is registered). This includes advertisements,
 * action responses, device, service and icon retrieval
 * responses on a client, UPnP action invocations, subscription
 * requests, device searches, and device, service and icon
 * retrieval requests on a server.
 */

public interface UPnPIncomingMessageHandler
{
    /**
     * Handles an incoming message. The primary responsibility is
     * to parse the incoming byte array and produce an XML document
     * representing the incoming content.
     * An application-provided {@code UPnPIncomingMessageHandler} may invoke
     * the default, stack-provided message handler via the specified
     * {@code defaultHandler}.
     * The handler may also cause the incoming message to be discarded by
     * returning {@code null}; subsequent
     * processing SHALL continue as if the message had never been received.
     * <p>
     * Note that if the {@code UPnPMessage} returned by this method contains an
     * HTTP {@code CONTENT-LENGTH} header, its value should describe the
     * length of the raw XML data <i>before</i> it is parsed
     * into the XML document reported by {@link UPnPMessage#getXML()}.  See
     * {@link UPnPMessage#getHeaders()}.
     *
     * @param address InetSocketAddress representing the network interface
     * and port on which the message was received.
     *  
     * @param incomingMessage  The incoming UPnP message data,
     *                         including any HTTP headers.
     *  
     * @param defaultHandler The default stack-provided incoming
     *                       message handler.
     *                       If this {@code UPnPIncomingMessageHandler} is
     *                       the default incoming message handler, this
     *                       parameter SHALL be ignored.
     *  
     * @return The UPnP message to be passed up the stack.
     *                     The handler can cause the incoming message to be
     *                     discarded by returning null.
     */
    public UPnPMessage handleIncomingMessage(InetSocketAddress address,
            byte[] incomingMessage,
            UPnPIncomingMessageHandler defaultHandler);
}

