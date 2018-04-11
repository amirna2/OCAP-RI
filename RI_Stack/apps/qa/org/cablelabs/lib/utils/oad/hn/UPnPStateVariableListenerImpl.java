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

import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;

//
// UPnPStateVariableListenerImpl
//
class UPnPStateVariableListenerImpl implements UPnPStateVariableListener
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(UPnPStateVariableListenerImpl.class);
    private static final long TIMEOUT = 15000;
    private Object m_signal;
    private String m_varName;



    UPnPStateVariableListenerImpl()
    {
         m_varName = "";
         m_signal = new Object();
    }
    
    /**
     * {@inheritDoc}
     */
    public void notifyValueChanged(UPnPClientStateVariable var) 
    {

        if (log.isInfoEnabled())
        {
            log.info("notifyValueChanged called with var = " + var.getName());
        }
 
        // signal that value is ready
        try
        {
            synchronized (m_signal)
            {
                m_varName = var.getName();
                m_signal.notifyAll();
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Exception in notifyValueChanged", e);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public void notifySubscribed(UPnPClientService service)
    {
        // not needed right now
    }

    /**
     * {@inheritDoc}
     */
    public void notifyUnsubscribed(UPnPClientService service)
    {
        // not needed right now
    }

    /**
     * Gets the last StateVariable event received 
     * 
     * @returns name of last event received
     */

    public String getStateVariableEvent()
    {
   
        // if value received already.. return it
        if (m_varName.length() != 0)
        {
            return m_varName;
        }
   
        // otherwise wait for event 
        try
        {
            synchronized (m_signal)
            {
                m_signal.wait(TIMEOUT);
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Exception in getStateVariableEvent", e);
            }
        }
        return m_varName;
    }

    /**
     * Sets UPnPStateVariableListener on a service 
     *
     * @param device    String representing friendly name of root device
     * @param subDevice String representing model name of embedded device
     * @param service   String representing service type of service 
     *
     * @returns true if listener set.. false otherwise 
     */

    public boolean setUPnPStateVariableListener(String device, String subDevice,
        String service)
    {

        if ((device == null) || (subDevice == null) || (service == null))
        {
            return false;
        }

        if ((device.length() == 0) || (subDevice.length() == 0) || (service.length() == 0))
        {
            return false;
        }

        // find the device
        UPnPClientDevice[] devices = UPnPControlPoint.getInstance().getDevices();
        if (devices.length == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("No devices found");
            }
            return false;
        }

        UPnPClientDevice rootDevice = null;
        for (int i = 0; i < devices.length; i++)
        {
            if (devices[i].isRootDevice())
            {
                if (device.equals(devices[i].getFriendlyName()))
                {
                    rootDevice = devices[i];
                    break;
                }
            }
        }
        if (rootDevice == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("root device " + device + " not found");
            }
            return false;
        }

        // find the subDevice
        UPnPClientDevice embeddedDev = null;
        UPnPClientDevice[] embeddedDevs = rootDevice.getEmbeddedDevices();
        for (int i = 0; i < embeddedDevs.length; i++)
        {
            if (subDevice.equals(embeddedDevs[i].getModelName()))
            {
                embeddedDev = embeddedDevs[i];
                break;
            }
        }
        if (embeddedDev == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("sub device " + subDevice + " not found");
            }
            return false;
        }

        // find service
        UPnPClientService[] services = embeddedDev.getServices();
        if (services == null)
        {
           return false;
        }

        UPnPClientService targetService = null;
        for (int i = 0; i < services.length; i++)
        {
            String serviceType = services[i].getServiceType();
            if (service.equals(serviceType))
            {
                targetService = services[i];
                break;
            }
        }
        if (targetService == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("service " + service + " not found");
            }
            return false;
        }
        
        // Register this app to listen for evented state variable changes
        try
        {
            targetService.addStateVariableListener(this);
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failed to add state variable listener", e);
            }
            return false;
        }

        // listener added 
        return true;
    }

}

