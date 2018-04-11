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
package org.cablelabs.impl.service;

import java.net.InetAddress;
import java.net.URL;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.service.HttpRequestResolutionHandler;
import org.ocap.hn.service.MediaServerManager;
import org.ocap.system.MonitorAppPermission;

public class MediaServerManagerImpl extends MediaServerManager
{
    private HttpRequestResolutionHandlerProxy m_requestResolutionHandlerProxy = null;
    
    private class HttpRequestResolutionHandlerProxy implements CallbackData
    {
        private final HttpRequestResolutionHandler m_requestResolutionHandler;
        private final CallerContext m_callerContext;
        
        public HttpRequestResolutionHandlerProxy(final HttpRequestResolutionHandler rrh,
                                                 final CallerContext context)
        {
            m_requestResolutionHandler = rrh;
            m_callerContext = context;
            context.addCallbackData(this, getClass());
        }

        public void active(CallerContext callerContext)
        {
            // Don't do anything - handler stays registered
        }

        public void pause(CallerContext callerContext)
        {
            // Don't do anything - handler stays registered
        }
        
        public void destroy(CallerContext callerContext)
        {
            // App implementing the handler is dead, so there really is no more handler
            //  (in case a new app has snuck in and replaced this handler, pass ourself for 
            //  validation)
            clearHttpRequestResolutionHandlerProxy(this);
        }
        
        public URL invokeHandler(final InetAddress inetAddress,
                                 final URL url,
                                 final String[] request,
                                 final NetworkInterface networkInterface)
        {
            final URL[] returnURL = { null };

            CallerContext.Util.doRunInContextSync(m_callerContext, new Runnable()
            {
                public void run()
                {
                    returnURL[0] = 
                        m_requestResolutionHandler.resolveHttpRequest(inetAddress, url, 
                                                                       request, networkInterface);
                }
            }); // block until complete

            return returnURL[0];
        }

        /**
         * The proxy is being deactivated
         */
        public void dispose()
        {
            m_callerContext.removeCallbackData(getClass());
        }
    } // END class HttpRequestResolutionHandlerProxy

    private void clearHttpRequestResolutionHandlerProxy(HttpRequestResolutionHandlerProxy rrhp)
    {
        synchronized (this)
        {
            if (this.m_requestResolutionHandlerProxy == rrhp)
            { // Only unregister if this is the active proxy (there can be a race between
              //  app death and registration of a new proxy)
                rrhp.dispose();
                this.m_requestResolutionHandlerProxy = null;
            }
        }
    }
    
    public void setHttpRequestResolutionHandler(HttpRequestResolutionHandler newHandler)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        synchronized (this)
        {
            if (m_requestResolutionHandlerProxy != null)
            {
                clearHttpRequestResolutionHandlerProxy(m_requestResolutionHandlerProxy);
            }
            
            if (newHandler != null)
            {
                m_requestResolutionHandlerProxy 
                    = new HttpRequestResolutionHandlerProxy(newHandler, ccm.getCurrentContext());
            }
        }
    }

    public int getHttpMediaPortNumber()
    {
        return MediaServer.getInstance().getConfiguredMediaStreamingPort();
    }

    public URL invokeHTTPRequestResolutionHandler(final InetAddress inetAddress,
                                                  final URL url,
                                                  final String[] request,
                                                  final NetworkInterface networkInterface)
    {
        HttpRequestResolutionHandlerProxy rrhProxy;
        
        synchronized (this)
        {
            rrhProxy = m_requestResolutionHandlerProxy;
        }
        
        if (rrhProxy == null)
        {
            return null;
        }
        else
        {
            return rrhProxy.invokeHandler(inetAddress, url, request, networkInterface);
        }
    }
}
