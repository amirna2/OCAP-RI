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
package org.cablelabs.impl.ocap.hn.upnp.ruihsrc;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.ruihsrc.RemoteUIServerManagerImpl;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.ocap.hn.ruihsrc.RemoteUIServerManager;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.xml.sax.SAXException;

/**
 * RemoteUIService - Provides the state and actions for the RemoteUI Server Service
 */
public class RemoteUIServerService implements UPnPActionHandler
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(RemoteUIServerService.class);

    /** Defines the type for the RemoteUIServerService */
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:RemoteUIServer:1";
    
    public static final String ID = "urn:upnp-org:serviceId:RemoteUIServer";

    // ACTION DEFINITIONS
    public static final String GET_COMPATIBLEUIS = "GetCompatibleUIs";

    /**
     * Handle to the NetSecurityManager. Used to authorize the action.
     */
    private static final NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();
    
    private final UPnPManagedService m_service;

    // STATE VARIABLE DEFINITIONS
    public static final String UI_LISTING_UPDATE = "UIListingUpdate";
    private UIListingUpdateEventer m_uiListingUpdateEventer;

    /**
     * Construct a <code>ContentDirectoryService</code>.
     */
    public RemoteUIServerService(UPnPManagedService service)
    {
        assert service != null;
        
        m_service = service;

        // Initialize state variables based on new service.
        m_uiListingUpdateEventer = new UIListingUpdateEventer(((UPnPManagedServiceImpl)m_service).getManagedStateVariable(UI_LISTING_UPDATE));

        m_uiListingUpdateEventer.set("");
        
        // Register as a listener to actions for this service.
        m_service.setActionHandler(this);
    }


    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        String actionName = action.getAction().getName();
        String[] args = action.getArgumentNames();
        String[] values = new String[args.length];
        
        for(int i = 0; i < args.length; i++)
        {
            values[i] = action.getArgumentValue(args[i]);
            if (log.isDebugEnabled())
            {
                log.debug(actionName + " param = " + args[i] + " = " + values[i]);
            }
        }
        
        UPnPResponse response = null;
        
        if(GET_COMPATIBLEUIS.equals(actionName))
        {
            response = getCompatibleUIsAction(action, values);
        }
        
        // Return response or Invalid Action if not set
        return response != null ? response : new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
    }

    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("Default RemoteUIServer Service action handler being replaced");
        }       
    }
    
    /**
     * Gets the compatible UIs and returns list to caller
     *
     * @param action
     *            The action containing information to match uis.
     *
     * @return UPnPResponse The compatible ui list in RUIHSRC XML format.
     */
    private UPnPResponse getCompatibleUIsAction(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;
        
        // Verify the arguments are valid
        if (values == null || values.length != 2)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved more or less than one argument");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
                
        try
        {
            String responseStr = ((RemoteUIServerManagerImpl)
                RemoteUIServerManager.getInstance()).getCompatibleUIs(values[0], values[1]);
            
            response = new UPnPActionResponse(new String[] { responseStr }, action);
        }
        catch (Exception e)
        {
            if(log.isInfoEnabled())
            {
                log.info("RUI rejecting operation due to exception", e);
            }
            response = new UPnPErrorResponse(ActionStatus.UPNP_RUI_REJECT_OP.getCode(), 
                    ActionStatus.UPNP_RUI_REJECT_OP.getDescription(), action);
        }
        
        return response;
    }

    public void setuiListingUpdate(String value)
    {
        m_uiListingUpdateEventer.set(value);
    }

}
