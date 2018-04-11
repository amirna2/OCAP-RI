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

import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.w3c.dom.Document;

/**
 * The class represents a UPnP message comprising an HTTP start line,
 * zero or more headers and an optional XML document.
 */
public class UPnPMessage
{
    /**
     * An empty string array.
     */
    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * The HTTP start line.
     */
    private final String startLine;

    /**
     * The HTTP or other headers.
     */
    private final String[] headers;

    /**
     * The XML document, if any.
     */
    private final Document xml;

    /**
     * Public constructor for the message.
     *
     * @param startLine The HTTP start line, excluding trailing CR/LF
     * characters. May be an empty string.
     *
     * @param headers The HTTP or other headers that this instance is
     *                to contain, excluding trailing CF/LF characters and the
     *                blank line that follows HTTP headers.
     *                The contents of the {@code headers} parameter are
     *                copied into the resulting {@code UPnPMessage} object.
     *                May be a zero-length array.

     *
     * @param xml The XML document that this instance is to contain.
     *            May be null.
     *
     * @throws NullPointerException if {@code startLine},
     * {@code headers}, or any of the array elements within {@code headers} is
     * {@code null}.
     */
    public UPnPMessage(String startLine, String[] headers, Document xml)
    {
        // Verify startLine, headers or headers elements not null
        if ((startLine == null) || (headers == null))
        {
            throw new NullPointerException("null argument");
        }
        for (int i = 0; i < headers.length; i++)
        {
            if (headers[i] == null)
            {
               throw new NullPointerException("null headers element");
            }
        }
            
        this.startLine = startLine;
        
        this.headers = headers;
        this.xml = XMLUtil.clone(xml);
    }

    /**
     * Reports the HTTP start line, excluding trailing CR/LF characters.
     *
     * @return The HTTP start line. If the UPnPMessage includes no start line,
     * returns the empty string.
     */
    public String getStartLine()
    {
        return startLine;
    }

    /**
     * Reports the headers from the message, including all HTTP headers but
     * excluding trailing CR/LF characters.
     * The blank line following the last HTTP header is not
     * included in the array.
     * <p>
     * Note that if the message includes an HTTP {@code CONTENT-LENGTH} header,
     * its value describes the length of the raw XML data carried in the
     * UPnP message body, not the size of the XML document provided by
     * {@link #getXML()}.
     *  
     * @return An array containing a copy of the header lines contained
     *         in the {@code UPnPMessage} obejct.
     *         If the {@code UPnPMessage} has no headers,
     *         returns a zero-length array.
     *
     * @see UPnPIncomingMessageHandler
     * @see UPnPOutgoingMessageHandler
     */
    public String[] getHeaders()
    {
        return headers;
    }

    /**
     * Gets the XML from the message.
     *
     * @return The XML document of the message. May be null if the
     *         message contained no XML document.
     */
    public Document getXML()
    {
        return XMLUtil.clone(xml);
    }
}
