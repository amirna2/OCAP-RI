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

package org.cablelabs.impl.ocap.hn.upnp.common;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;

import org.cybergarage.upnp.diag.Interceptor;
import org.cybergarage.upnp.diag.Message;

import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPMessage;

/**
 * The CyberLink <code>Interceptor</code> used by {@link UPnPControlPoint}
 * and {@link UPnPDeviceManagerImpl} for incoming messages.
 */
public class UPnPInputInterceptor implements Interceptor
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(UPnPInputInterceptor.class);

    /**
     * The default {@link UPnPIncomingMessageHandler} passed to application
     * {@link UPnPIncomingMessageHandler}s.
     */
    private static final UPnPIncomingMessageHandler DEFAULT_IMH = new UPnPIncomingMessageHandler()
    {
        public UPnPMessage handleIncomingMessage(InetSocketAddress isa, byte[] ba, UPnPIncomingMessageHandler imh)
        {
            return XMLUtil.toUPnPMessage(ba);
        }
    };

    /**
     * The application {@link UPnPIncomingMessageHandler}.
     */
    private final UPnPIncomingMessageHandler inHandler;

    /**
     * Construct an object of this class, using the application
     * {@link UPnPIncomingMessageHandler}.
     *
     * @param inHandler  The application {@link UPnPIncomingMessageHandler};
     *                   is never <code>null</code>.
     */
    public UPnPInputInterceptor(UPnPIncomingMessageHandler inHandler)
    {
        assert inHandler != null;

        this.inHandler = inHandler;
    }

    //  This javadoc is copied from Interceptor.
    /**
     * Intercept an incoming message.
     *
     * @param host      The host from which the message was received.
     * @param port      The port from which the message was received.
     * @param message   The incoming message.
     *
     * @return A reference to the message that is to be substituted for the
     *         incoming message, or DISCARD if the incoming message is to be
     *         discarded, or DEFAULT if the incoming message is to be
     *         processed in the default manner.
     */
    public Message intercept(String host, int port, Message message)
    {
        if (log.isDebugEnabled())
        {
            log.debug("intercept: message in is " + message);
        }

        UPnPMessage upnpMessage;

        try
        {
            InetSocketAddress address = new InetSocketAddress(host, port);

            upnpMessage = inHandler.handleIncomingMessage(address, message.toByteArray(), DEFAULT_IMH);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("intercept: interception failed", e);
            }

            if (log.isDebugEnabled())
            {
                log.debug("intercept: message out is DEFAULT");
            }

            return Interceptor.DEFAULT;
        }

        if (upnpMessage == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("intercept: message out is DISCARD");
            }

            return Interceptor.DISCARD;
        }

        Message result = XMLUtil.toMessage(upnpMessage);

        if (log.isDebugEnabled())
        {
            log.debug("intercept: message out is " + result);
        }

        return result;
    }
}
