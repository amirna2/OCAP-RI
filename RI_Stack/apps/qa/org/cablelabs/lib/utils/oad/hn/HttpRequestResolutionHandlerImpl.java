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

package org.cablelabs.lib.utils.oad.hn;

import java.net.InetAddress;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.service.HttpRequestResolutionHandler;

/**
 * Purpose: This class contains methods related to support http request resolution
 * HN functionality. 
*/
public class HttpRequestResolutionHandlerImpl implements HttpRequestResolutionHandler
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(HttpRequestResolutionHandlerImpl.class);
    private String m_newURLPath; 
    private OcapAppDriverHN m_oadHN;
    private OcapAppDriverCore m_oadCore;
    private boolean m_wasInvoked;


    public HttpRequestResolutionHandlerImpl(OcapAppDriverHN oadHN, OcapAppDriverCore oadCore)
    {
        m_oadHN = oadHN;
        m_oadCore = oadCore;
        m_newURLPath = "";
        m_wasInvoked = false;
    }
   
    /**
     * Http request resolution handler.
     *
     * @param netAddress        InetAddress of incoming request.
     * @param url               URL of incoming request.
     * @param request           String of incoming request
     * @param networkInterface  NetworkInterface incoming request received on
     */
 
    public URL resolveHttpRequest(InetAddress inetAddress,
                                 URL url,
                                 String[] request,
                                 org.ocap.hn.NetworkInterface networkInterface)
    {

        m_wasInvoked = true;

        URL retURL = null;

        if (log.isInfoEnabled())
        {
            log.info("resolveHttpRequest called with " + url.toString());
        }

        // no change just return what came in
        if ("".equals(m_newURLPath))
        {
            if (log.isInfoEnabled())
            {
                log.info("resolveHttpRequest returning " + url.toString());
            }
            return url;
        }

        // otherwise modify return path
        try
        {
            retURL = new URL(url.getProtocol(), url.getHost(),
                url.getPort(), m_newURLPath);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception creating URL", e);
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("resolveHttpRequest returning " + retURL.toString());
        }

        return retURL;

    }

    /**
     * Sets URL path in URL returned by HttpRequestResolutionHandler.
     *
     * @param retURLPath  new path component.
     */

    public void setReturnURLPath(String retURLPath)
    {
        m_newURLPath = retURLPath;
    }

    /**
     * Returns URL path set by setReturnURLPath 
     *
     */

    public String getReturnURLPath()
    {
        return m_newURLPath;
    }

    /**
     * Returns boolean indicating whether HttpRequestResolutionHandler was invoked 
     *
     */
    public boolean wasInvoked()
    {
        return m_wasInvoked;
    }
}


