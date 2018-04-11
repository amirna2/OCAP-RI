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

import org.apache.log4j.Logger;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPGeneralErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
  * This class is used to query media servers to determine if they support live streaming.  
 */
public class LiveStreamingUPnPActionResponseHandler implements UPnPActionResponseHandler
{
    private static final Logger log = Logger.getLogger(LiveStreamingUPnPActionResponseHandler.class);

    private Object m_sync;
    protected boolean m_isTuner;
    protected boolean m_responseReceived;
    
    public LiveStreamingUPnPActionResponseHandler(Object sync, boolean isTuner)
    {
        m_sync = sync;
        m_isTuner = isTuner;
        m_responseReceived = false;
    }
    
    public void notifyUPnPActionResponse(UPnPResponse response)
    {
        synchronized(m_sync) 
        {
            if (response instanceof UPnPGeneralErrorResponse || response instanceof UPnPGeneralErrorResponse)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Action: " + response.getActionInvocation().getName() + " returned HTTPResponseCode: " + response.getHTTPResponseCode());
                }
                m_responseReceived = true;
                m_sync.notifyAll();
                return;
            }
            String value = null;
            try 
            {
                if (response instanceof UPnPActionResponse)
                {
                    value = ((UPnPActionResponse) response).getArgumentValue("FeatureList");
                }
                else 
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Action: " + response.getActionInvocation().getName() + " returned unexpected response: " + 
                                response.getClass().getName());
                    }
                    m_responseReceived = true;
                    m_sync.notifyAll();
                    return;                            
                }
            }
            catch (IllegalArgumentException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Action: " + response.getActionInvocation().getName() + " did not have a FeatureList out argument.");
                }
                m_responseReceived = true;
                m_sync.notifyAll();
                return;
            }
            if (log.isInfoEnabled())
            {
                log.info("Action: " + response.getActionInvocation().getName() + " return value:\n" + value);
            }
            if (value.indexOf("TUNER") > 0) 
            {
                if (log.isInfoEnabled())
                {
                    log.info("Action: " + response.getActionInvocation().getName() + " found a live Streaming MediaServer");
                }
                m_isTuner = true;
            }
            m_responseReceived = true;
            m_sync.notifyAll();
        }
        return;
    }
}


