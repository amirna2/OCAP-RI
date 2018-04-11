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


package org.cablelabs.impl.ocap.hn.upnp.cds;


import java.net.Socket;
import java.net.URL;

import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer.HTTPRequest;
import org.ocap.hn.content.ContentEntry;

/**
 * An <code>HTTPRequestInterceptor</code> intercepts HTTP GET requests as they are
 * about to be processed by the <code>ContentDirectoryService</code>. It may
 * indicate that any particular HTTP GET request is not to be processed further,
 * for instance because it has processed it itself.
 * <p>
 * <code>HTTPRequestInterceptor</code>s are registered with the
 * <code>ContentDirectoryService</code>, and are consulted in the order in
 * which they are registered.
 *
 */
public interface HTTPRequestInterceptor
{
    /**
     * Intercept an HTTP GET request as it is about to be processed by the
     * <code>ContentDirectoryService</code>, and indicate whether or not
     * the request is to be processed further.
     *
     * @param httpRequest The UPnPAction associated with this http request
     * @param contentEntry The ContentEntry associated with the request. If null, 
     *        the request URI didn't match any ContentEntry's res or alternateRes 
     *        property
     * @param requestURL The URL from the HTTP request
     * @param effectiveURL The URL the interceptor should act on (for URL aliasing)
     * @return True if the request is not to be processed further; else false.
     *
     * @throws HNStreamingException if processing of the request by the
     *                              <code>HTTPRequestInterceptor</code> results
     *                              in an <code>HNStreamingException</code>.
     */
    boolean intercept(Socket socket, HTTPRequest httpRequest, ContentEntry contentEntry, URL requestURL, URL effectiveURL)
        throws HNStreamingException;
}
