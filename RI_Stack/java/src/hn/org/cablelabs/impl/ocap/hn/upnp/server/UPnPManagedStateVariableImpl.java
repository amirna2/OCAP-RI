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

package org.cablelabs.impl.ocap.hn.upnp.server;

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPStateVariableImpl;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.UPnPStatus;
import org.cybergarage.upnp.control.QueryListener;
import org.ocap.hn.upnp.common.UPnPAdvertisedStateVariable;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;
import org.ocap.hn.upnp.server.UPnPStateVariableHandler;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.system.MonitorAppPermission;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContextManager;

public class UPnPManagedStateVariableImpl extends UPnPStateVariableImpl implements UPnPManagedStateVariable, QueryListener
{
    // Log4J logging facility
    private static final Logger log = Logger.getLogger(UPnPManagedStateVariableImpl.class);
    
    private final UPnPManagedServiceImpl m_service;
    private String m_unHandledValue = null;
    private int m_moderationInterval = 0;
    private int m_moderationDelta = 0;
    private boolean m_respondToQueries = false;
    
    public UPnPManagedStateVariableImpl (StateVariable stateVariable, UPnPManagedServiceImpl service)
    {
        super(stateVariable);
        
        assert service != null;

        m_service = service;
        
        if(getStateVariable() != null)
        {
            getStateVariable().setQueryListener(this);
        }
    }

    public void setValue(final String value) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        
        boolean isValid = false;
        if (Utils.isUDAFloat(getDataType()) || Utils.isUDAInt(getDataType())) 
        {
            isValid = Utils.validateUDANumericValue(getDataType(), value, getMinimumValue(), getMaximumValue());
        }
        else if ("string".equals(getDataType())) 
        {
            isValid = Utils.validateUDAStringValue(value, getAllowedValues());
        }
        else
        {
            isValid = Utils.validateUDAValue(getDataType(), value);
        }
        if (!isValid)
        {
            throw new IllegalArgumentException();
        }
        if(isEvented())
        {
            // allow cybergarage to send event 
            CallerContextManager ccm = (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        getStateVariable().setValue(value);
                    }
                } );
        }
        m_unHandledValue = value;
    }

    public String getValue()
    {
        if(m_unHandledValue != null)
        {
            return m_unHandledValue;
        }
        
        return getDefaultValue();
    }

    public void setModerationInterval(int interval) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        if(!isEvented())
        {
            throw new UnsupportedOperationException();
        } 
        
        // TODO : set this in cybergarage?
        m_moderationInterval = interval;
    }

    public int getModerationInterval()
    {
        if(!isEvented())
        {
            throw new UnsupportedOperationException();
        }        
        return m_moderationInterval;
    }

    public void setModerationDelta(int delta) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        if(!isEvented() || !(Utils.isUDAInt(getDataType()) || Utils.isUDAFloat(getDataType())))
        {
            throw new UnsupportedOperationException();
        }
      
        if(delta < 0)
        {
            throw new IllegalArgumentException();
        } 
        // TODO : set this in cybergarage?
        m_moderationDelta = delta;
    }

    public int getModerationDelta()
    {
        if(!isEvented())
        {
            throw new UnsupportedOperationException();
        }         
        return m_moderationDelta;
    }

    public UPnPManagedService getManagedService()
    {
        return m_service;
    }

    // End of Published APIs
    
    public void setRespondToQueries(boolean respond)
    {
        m_respondToQueries = respond;
    }
    
    /**
     * Implements a Cybergarage callback.
     */
    public boolean queryControlReceived(StateVariable stateVariable)
    {
        if(m_respondToQueries)
        {
            UPnPStateVariableHandler handler = ((UPnPManagedServiceImpl)getService()).getHandler();
            
            if(handler != null)
            {
                String returnedState = handler.getValue(this);
                stateVariable.setValue(returnedState);
                return true;
            }
            else
            {
                if(m_unHandledValue != null)
                {
                    stateVariable.setValue(m_unHandledValue);
                }
                else
                {
                    stateVariable.setValue(getDefaultValue());
                }
            }
            return true;
        }
        else
        {
            stateVariable.setStatus(UPnPStatus.INVALID_ACTION);
        }
        // Returns a 401 Invalid Action
        return false;
    }

    public UPnPAdvertisedStateVariable[] getAdvertisedStateVariables()
    {
        // If it is not advertised, return empty array.
        if(!m_service.getDevice().isAlive())
        {
            return new UPnPAdvertisedStateVariable[0];
        }
        
        // TODO : creating new UPnPAdvertisedStateVariables every time should we attempt to cache?
        // If caching need to consider lifecycle of state variable in this class.
        
        InetAddress[] inets = m_service.getDevice().getInetAddresses();
        UPnPAdvertisedStateVariable[] asvs = new UPnPAdvertisedStateVariable[inets.length];
        
        for(int i = 0; i < inets.length; ++i)
        {
            asvs[i] = new UPnPAdvertisedStateVariableImpl(getStateVariable());
        }
        return asvs;
    }

    public UPnPManagedService getService()
    {
        return m_service;
    }
}
