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

package org.cablelabs.impl.ocap.hn.upnp.client;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPStateVariableImpl;
import org.cybergarage.upnp.AllowedValue;
import org.cybergarage.upnp.AllowedValueList;
import org.cybergarage.upnp.AllowedValueRange;
import org.cybergarage.upnp.StateVariable;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPClientService;

/**
 * This class is the client representation of a UPnP state 
 * variable. 
 */
public class UPnPClientStateVariableImpl extends UPnPStateVariableImpl implements UPnPClientStateVariable
{
    // Service this action is associated with
    private final UPnPClientServiceImpl m_service;

    private static final Logger log = Logger.getLogger(UPnPClientStateVariableImpl.class);

    /**
     * Constructs an object of this class.
     *
     * @param name The name of the state variable.
     */
    public UPnPClientStateVariableImpl(StateVariable stateVariable, UPnPClientServiceImpl service)
    {
        super(stateVariable);
        assert stateVariable != null;
        m_stateVariable = stateVariable;
        assert service != null;
        m_service = service;
    }

    /**
     * Gets the value of the UPnP state variable corresponding to this
     * <code>UPnPStateVariable</code> object.
     *
     * @return The most recently received evented value of the state
     *         variable.
     *
     * @throws UnsupportedOperationException if the UPnP state variable
     *      corresponding to this <code>UPnPStateVariable</code> object is not
     *      evented as can be determined by calling the
     *      <code>isEvented()</code> method.
     */
    public String getEventedValue()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getEventedValue() - called for: " + m_stateVariable.getName());
        }                            
        if (m_stateVariable.isSendEvents())
        {
            if (log.isDebugEnabled())
            {
                log.debug("getEventedValue() - returning: " + m_stateVariable.getValue());
            }                            
            return m_stateVariable.getValue();
        }
        else
        {
            throw new UnsupportedOperationException("State Variable is not evented: " + 
                    m_stateVariable.getName());
        }
    }

    /**
     * Gets the <code>UPnPService</code> that this state variable is
     * a member of. 
     *
     * @return The <code>UPnPService</code> that this state variable 
     *         is a member of.
     */
    public UPnPClientService getService()
    {
        return m_service;
    }
    
    /**
     * Sets the value of this evented variable based on supplied value from event listener.
     * 
     * @param value set value of this state variable
     */
    public void setEventedValue(String value)
    {
        m_stateVariable.setValue(value);
    }
}
