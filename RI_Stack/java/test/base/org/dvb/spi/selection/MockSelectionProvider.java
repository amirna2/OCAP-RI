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
package org.dvb.spi.selection;

import java.util.HashMap;

import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;

import org.dvb.spi.util.MultilingualString;

/**
 * CannedSelectionProvider
 * 
 * @author Joshua Keplinger
 * 
 */
public class MockSelectionProvider implements SelectionProvider
{

    SelectionProviderContext spc;

    HashMap serviceList = new HashMap();

    public boolean inited = false;

    public boolean registered = false;

    public LocatorScheme scheme;

    public MockSelectionProvider()
    {
        for (int i = 1; i <= 4; i++)
        {
            String hexId = Integer.toHexString(i);
            String loc = "ocap://0x" + hexId;
            ServiceReference sr = new MockServiceReference(loc, loc);
            ServiceDescription sd = new MockServiceDescription(new MultilingualString[] { new MultilingualString(
                    "Service 0x" + hexId, "eng") });
            serviceList.put(sr, sd);
        }
        scheme = new LocatorScheme("ocap", true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dvb.spi.selection.SelectionProvider#getServiceDescriptions(org.dvb
     * .spi.selection.ServiceReference[])
     */
    public ServiceDescription[] getServiceDescriptions(ServiceReference[] services)
    {
        ServiceDescription[] descs = new ServiceDescription[services.length];
        for (int i = 0; i < descs.length; i++)
        {
            descs[i] = (ServiceDescription) serviceList.get(services[i]);
        }
        return descs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionProvider#getServiceList()
     */
    public ServiceReference[] getServiceList()
    {
        return (ServiceReference[]) serviceList.keySet().toArray(new ServiceReference[] {});
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionProvider#getSupportedLocatorSchemes()
     */
    public LocatorScheme[] getSupportedLocatorSchemes()
    {
        return new LocatorScheme[] { scheme };
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.dvb.spi.selection.SelectionProvider#init(org.dvb.spi.selection.
     * SelectionProviderContext)
     */
    public void init(SelectionProviderContext c)
    {
        if (inited) throw new IllegalStateException("MockSelectionProvider is already inited");

        spc = c;
        inited = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dvb.spi.selection.SelectionProvider#newSession(org.dvb.spi.selection
     * .ServiceReference)
     */
    public SelectionSession newSession(ServiceReference service)
    {
        if (service == null) return null;

        MockSelectionSession session = null;
        if (serviceList.containsKey(service)) session = new MockSelectionSession(this, service);

        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.Provider#getName()
     */
    public String getName()
    {
        return "0x00000001.MockSelectionProvider.NoCard";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.Provider#getServiceProviderInterfaces()
     */
    public Class[] getServiceProviderInterfaces()
    {
        return new Class[] { SelectionProvider.class };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.Provider#getVersion()
     */
    public String getVersion()
    {
        return "1.0";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.Provider#providerRegistered()
     */
    public void providerRegistered()
    {
        if (registered) throw new IllegalStateException("MockSelectionProvider is already registered");

        registered = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.Provider#providerUnregistered()
     */
    public void providerUnregistered()
    {
        if (!registered) throw new IllegalStateException("MockSelectionProvider is already unregistered");

        registered = false;
    }

    public void cannedAddServiceReference(ServiceReference ref, ServiceDescription sd)
    {
        serviceList.put(ref, sd);
        if (spc != null) spc.serviceListChanged(getServiceList());
    }

    public void cannedRemoveServiceReference(ServiceReference ref)
    {
        serviceList.remove(ref);
        if (spc != null) spc.serviceListChanged(getServiceList());
    }

    public void cannedUpdateServiceReference(ServiceReference oldRef, ServiceReference newRef, ServiceDescription newSd)
    {
        serviceList.remove(oldRef);
        if (serviceList.containsKey(oldRef)) throw new IllegalStateException("Old reference was not removed");
        serviceList.put(newRef, newSd);
        if (spc != null) spc.updateService(newRef);
    }

    public ServiceDescription cannedGetServiceDescription(ServiceReference ref)
    {
        return (ServiceDescription) serviceList.get(ref);
    }

    public static class MockServiceType extends ServiceType
    {
        public static final ServiceType MOCKSERVICETYPE = new MockServiceType();

        public MockServiceType()
        {
            super("MockServiceType");
        }
    }

    public static class MockServiceDescription implements ServiceDescription
    {

        private MultilingualString[] names;

        public MockServiceDescription(MultilingualString[] names)
        {
            this.names = names;
        }

        public DeliverySystemType getDeliverySystemType()
        {
            return DeliverySystemType.CABLE;
        }

        public MultilingualString getLongName(String preferredLanguage)
        {
            MultilingualString result = names[0];
            for (int i = 0; i < names.length; i++)
            {
                if (names[i].getLanguageCode().equals(preferredLanguage)) result = names[i];
            }
            return result;
        }

        public ServiceType getServiceType()
        {
            return MockServiceType.MOCKSERVICETYPE;
        }

    }
}
