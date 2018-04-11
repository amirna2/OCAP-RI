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
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.navigation.ContentList;

//
/// NetActionHandlerImpl
//
/// This inner class is used in multiple places to wait for responses to
/// network-based requests (i.e. client/server); NetActions
//
public class NetActionHandlerImpl implements NetActionHandler
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(NetActionHandlerImpl.class);
    protected NetActionRequest m_netActionRequest;

    // responses from received NetActionEvent
    private NetActionEvent m_netActionEvent = null;
    
    private String m_failReason;

    // signals and flags for received NetActionHandler NetActionEvent
    private boolean m_receivedNetActionEvent = false;

    // use for signaling completion of async calls
    private Object m_signal = new Object();

    public NetActionHandlerImpl()
    {
        m_netActionRequest = null;
    }
    
    public void setNetActionRequest(NetActionRequest netActionRequest)
    {
        m_netActionRequest = netActionRequest;
    }
    
    /**
     * NetActionHandler callback
     */
    public void notify(NetActionEvent event)
    {
        m_netActionEvent = event;

        // if we've received an in-progress event, wait for another event
        if (m_netActionEvent.getActionStatus() != NetActionEvent.ACTION_IN_PROGRESS)
        {
            m_receivedNetActionEvent = true;

            try
            {
                synchronized (m_signal)
                {
                    m_signal.notifyAll();
                }
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ERROR notifying, exception: " + e.toString());
                }
            }
        }
    }

    /**
     * Waits for async response from a NetActionRequest method If
     * successful, the ActionEvent will be in this.netActionEvent
     *
     * @return true if got the response, false if not
     */
    public boolean waitRequestResponse(long timeoutMS)
    {
        if (!m_receivedNetActionEvent)
        {
            try
            {
                synchronized (m_signal)
                {
                    m_signal.wait(timeoutMS);
                }
            }
            catch (InterruptedException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ERROR in wait, exception: " + e.toString());
                }
            }
        }

        if (!m_receivedNetActionEvent)
        {
            m_failReason = "Failed to get event in " + timeoutMS + "ms";
            if (log.isInfoEnabled())
            {
                log.info(m_failReason);
            }

            return false;
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("waitRequestResponse(): received " +
                         getActionStatusString(m_netActionEvent));
            }

            m_receivedNetActionEvent = false;
        }

        return true;
    }

    protected ContentList waitForContentList(long timeoutMS)
    {
        if (waitRequestResponse(timeoutMS))
        {
            // assuming we have a content item we can now return
            Object response = m_netActionEvent.getResponse();

            if (response == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Request response is null");
                }
            }
            else if (!(response instanceof ContentList))
            {
                if (log.isInfoEnabled())
                {
                    log.info("Request response is not a ContentList: " +
                             response);
                }
            }
            else
            {
                return (ContentList) response;
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Request failed");
            }
        }

        return null;
    }
    
    protected String getActionStatusString(NetActionEvent event)
    {
        if (event == null)
        {
            return "NULL";
        }

        switch (event.getActionStatus())
        {
            case NetActionEvent.ACTION_COMPLETED:
                return "ACTION_COMPLETED";
            case NetActionEvent.ACTION_CANCELED:
                return "ACTION_CANCELED ";
            case NetActionEvent.ACTION_FAILED:
                return "ACTION_FAILED ";
            case NetActionEvent.ACTION_IN_PROGRESS:
                return "ACTION_IN_PROGRESS ";
            case NetActionEvent.ACTION_STATUS_NOT_AVAILABLE:
                return "ACTION_STATUS_NOT_AVAILABLE ";
            default:
                return "UNKNOWN";
        }
    }

    public Object getEventResponse(NetActionEvent event)
    {
        if (event == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("event is null!");
            }
            return null;
        }

        if (event.getActionRequest() != m_netActionRequest)
        {
            if (log.isInfoEnabled())
            {
                log.info("request in event is not the same as our request!");
            }
            return null;
        }

        if (event.getError() != -1)
        {
            if (log.isInfoEnabled())
            {
                log.info("NetActionEvent had error " + event.getError());
            }
        }

        return event.getResponse();
    }

    /**
     * Gets the NetActionEvent received by this NetActionHandler
     *
     * @return netActionEvent, null if not received, non-null if valid event
     */
    public NetActionEvent getNetActionEvent()
    {
        return m_netActionEvent;
    }
    
    public String getFailReason()
    {
        return m_failReason;
    }
}

