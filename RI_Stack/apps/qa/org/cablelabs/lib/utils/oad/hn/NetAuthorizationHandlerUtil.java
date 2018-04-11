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
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.security.NetAuthorizationHandler;
import org.ocap.hn.security.NetAuthorizationHandler2;
import org.ocap.hn.security.NetSecurityManager;

/**
 * Purpose: This class contains methods related to support Net Authorization
 * handling HN functionality
*/
public class NetAuthorizationHandlerUtil
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(NetAuthorizationHandlerUtil.class);

    // A boolean indicating the NetAuthorizationHandler return policy
    private boolean m_nahReturnPolicy = true;
    
    // A boolean indicating whether only the first NAH message should be logged
    private boolean m_firstNAHMessageOnly = false;
    private int m_activityID = -1;
    
    /**
     * A Vector to contain notify messages
     */
    private Vector m_notifyMessages;
    
    NetAuthorizationHandlerUtil()
    {
        m_notifyMessages = new Vector();
    }
    
    protected boolean registerNAH()
    {
        NetAuthorizationHandler myNAH = new NetAuthorizationHandler()
        {
            /**
             * {@inheritDoc}
             */
            public boolean notifyAction(String actionName, InetAddress inetAddress, String macAddress, int activityID)
            {
                if (log.isInfoEnabled())
                {
                    log.info( "NAH:notifyAction(actionName " + actionName 
                                      + ",inetAddr " + inetAddress 
                                      + ",activityID " + activityID );
                }
                return m_nahReturnPolicy;
            }

            /**
             * {@inheritDoc}
             */
            public boolean notifyActivityStart(InetAddress inetAddress, String macAddress, URL url, int activityID)
            {
                if (log.isInfoEnabled())
                {
                    log.info( "NAH:notifyActivityStart(inetAddr " + inetAddress 
                                      + ",url " + url
                                      + ",activityID " + activityID );
                }
                return m_nahReturnPolicy;
            }
            
            /**
             * {@inheritDoc}
             */
            public void notifyActivityEnd(int activityID)
            {
                if (log.isInfoEnabled())
                {
                    log.info( "NAH:notifyActivityEnd(activityID " + activityID );
                }
            }
        };
        
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(myNAH, new String[] {"*:*"}, m_firstNAHMessageOnly);
        return true;
    }
    
    protected boolean registerNAH2()
    {
        NetAuthorizationHandler2 myNAH2 = new NetAuthorizationHandler2()
        {
            /**
             * {@inheritDoc}
             */
            public boolean notifyAction(String actionName, InetAddress inetAddress, int activityID, 
                                        String[] request, NetworkInterface networkInterface)
            {
                Vector tempMessages = new Vector();
                tempMessages.add( "NAH:notifyAction(actionName " + actionName 
                        + ",inetAddr " + inetAddress 
                        + ",activityID " + activityID );
                tempMessages.add( "  NI inet: " + networkInterface.getInetAddress()); 
                tempMessages.add( "  Request: " + request[0]);
                for (int i=1; i<request.length; i++)
                {
                    tempMessages.add( "  Header " + i + ": " + request[i]);
                }
                if (log.isInfoEnabled())
                {
                    for (int i = 0; i < tempMessages.size(); i++)
                    {
                        log.info(tempMessages.get(i));
                    }
                }
                m_notifyMessages.addAll(tempMessages);
                m_activityID = activityID;
                return m_nahReturnPolicy;
            }

            /**
             * {@inheritDoc}
             */
            public void notifyActivityEnd(int activityID, int resultCode)
            {
                String message =  "NAH:notifyActivityEnd(activityID " + activityID;
                m_notifyMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean notifyActivityStart( InetAddress inetAddress, URL url, int activityID, 
                                                ContentEntry entry, String[] request, 
                                                NetworkInterface networkInterface )
            {
                Vector tempMessages = new Vector();
                tempMessages.add( "NAH:notifyActivityStart(inetAddr " + inetAddress 
                        + ",url " + url
                        + ",activityID " + activityID );
                tempMessages.add( "  ContentEntry: " + entry); 
                tempMessages.add( "  NI: " + networkInterface); 
                tempMessages.add( "  Request: " + request[0]);
                for (int i=1; i<request.length; i++)
                {
                    tempMessages.add( "  Header " + i + ": " + request[i]);
                }
                if (log.isInfoEnabled())
                {
                    for (int i = 0; i < tempMessages.size(); i++)
                    {
                        log.info(tempMessages.get(i));
                    }
                }
                m_notifyMessages.addAll(tempMessages);
                m_activityID = activityID;
                return m_nahReturnPolicy;
            }
        };
        
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(myNAH2, new String[] {"*:*"}, m_firstNAHMessageOnly);

        return true;
    }
    
    protected void unregisterNAH()
    {
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        nsm.setAuthorizationHandler(null);
    }
    
    /**
     * A method to toggle the current NetAuthorizationHandler return policy
     */
    protected void toggleNAHReturnPolicy()
    {
        m_nahReturnPolicy = !m_nahReturnPolicy;
    }
    
    protected boolean getNAHReturnPolicy()
    {
        return m_nahReturnPolicy;
    }
    
    protected void toggleNAHFirstMessagePolicy()
    {
        m_firstNAHMessageOnly = !m_firstNAHMessageOnly;
    }
    
    protected boolean getNAHFirstMessagePolicy()
    {
        return m_firstNAHMessageOnly;
    }
    
    protected void revokeActivity() 
    {
        final NetSecurityManager nsm = NetSecurityManager.getInstance();
        String message = "  NAH:revokeAuthorization: " + m_activityID;
        m_notifyMessages.add(message);
        if (log.isInfoEnabled())
        {
            log.info(message);
        }
        nsm.revokeAuthorization(m_activityID);
    }
    
    protected int getNumNotifyMessages()
    {
        return m_notifyMessages.size();
    }
    
    protected String getNotifyMessage(int index)
    {
        if (index >= 0 && index < m_notifyMessages.size())
        {
            return m_notifyMessages.remove(index).toString();
        }
        else
        {
            return null;
        }
    }
}

