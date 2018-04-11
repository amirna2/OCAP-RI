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
package org.cablelabs.xlet.providerregistry;

import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.navigation.DeliverySystemType;

import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.dvb.spi.selection.KnownServiceReference;
import org.dvb.spi.selection.LocatorScheme;
import org.dvb.spi.selection.SelectionProvider;
import org.dvb.spi.selection.SelectionProviderContext;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.dvb.spi.util.MultilingualString;
import org.ocap.net.OcapLocator;

import org.cablelabs.test.autoxlet.Test;

public class TestSelectionProvider implements SelectionProvider
{
    static class ServiceInformation
    {
        Service service;

        ServiceReference serviceReference;

        ServiceInformation(Service s, ServiceReference sRef)
        {
            serviceReference = sRef;
            service = s;
        }
    }

    private String locatorPrefix = "ocap";
    private String locatorPrefix2 = "locatorScheme";

    private ServiceInformation[] serviceInformation;

    private SelectionProviderContext selectionProviderContext;

    private boolean providerFirst;

    private Test test;

    private boolean newSessionCalled;

    private boolean initCalled;

    TestSelectionProvider(Test t, ServiceInformation[] sInfo, boolean first)
    {
        test = t;
        serviceInformation = sInfo;
        providerFirst = first;
    }

    public void setServiceInformation(ServiceInformation[] sInfo)
    {
        serviceInformation = sInfo;
        if (initCalled)
        {
            selectionProviderContext.serviceListChanged(getServiceList());
        }
    }

    public void updateService(ServiceReference sRef, Service service)
    {
        //System.out.println("Selection provider updateService called.. initCalled:" + initCalled);
        for (int i = 0; i < serviceInformation.length; i++)
        {
            // Match the transport independent and dependent forms of locator
            if ((serviceInformation[i].serviceReference.getServiceIdentifier().equals(sRef.getServiceIdentifier()))
                    && (serviceInformation[i].serviceReference.getLocator().equals(sRef.getLocator())))
            {
                serviceInformation[i].serviceReference = sRef;
                serviceInformation[i].service = service;
                System.out.println("Selection provider updateService called.. service.getLocator():" + service.getLocator());
                break;
            }
        }
        if (initCalled)
        {
            selectionProviderContext.updateService(sRef);
        }
    }

    public int getNumberOfServices()
    {
        return serviceInformation.length;
    }

    public ServiceDescription[] getServiceDescriptions(ServiceReference[] services)
    {
        ServiceDescription[] sDesc = new ServiceDescription[serviceInformation.length];
        for (int i = 0; i < sDesc.length; i++)
        {
            sDesc[i] = new TestServiceDescription(serviceInformation[i].service);
        }
        return sDesc;
    }

    public ServiceReference[] getServiceList()
    {
        ServiceReference[] sRef = new ServiceReference[serviceInformation.length];
        for (int i = 0; i < sRef.length; i++)
        {
            sRef[i] = serviceInformation[i].serviceReference;
        }
        return sRef;
    }

    public LocatorScheme[] getSupportedLocatorSchemes()
    {
        return new LocatorScheme[] { new LocatorScheme(locatorPrefix, providerFirst),
                                     new LocatorScheme(locatorPrefix2, providerFirst),};
    }

    public void init(SelectionProviderContext c)
    {
        if (initCalled)
        {
            throw new RuntimeException("SelectionProvider.init() was called more than once");
        }

        //System.out.println("Selection provider init called..");
        selectionProviderContext = c;
        initCalled = true;

    }

    public boolean wasNewSessionCalled()
    {
        return newSessionCalled;
    }

    public SelectionSession newSession(ServiceReference service)
    {
        SelectionSession session = null;
        newSessionCalled = true;
        //System.out.println("Selection provider newSession called..");
        try
        {
            ServiceInformation sInfo = null;
            for (int i = 0; i < serviceInformation.length; i++)
            {
                if (serviceInformation[i].serviceReference.equals(service))
                {
                    sInfo = serviceInformation[i];
                    break;
                }
            }

            if (sInfo != null)
            {
                System.out.println("Selection provider newSession sInfo.service.getLocator().toString(): " + sInfo.service.getLocator().toString());
                session = new TestSelectionSession(sInfo.service.getLocator().toString());
            }           
        }
        catch (InvalidLocatorException exc)
        {
            test.fail("Could not construct SelectionSession");
        }
        
        //System.out.println("TestSelectionProvider session: " + session);
        
        return session;
    }

    public String getName()
    {
        return "0x000001.TestSelectionProvider.NoCard";
    }

    public Class[] getServiceProviderInterfaces()
    {
        return new Class[] { SelectionProvider.class };
    }

    public String getVersion()
    {
        return "Version";
    }

    public void providerRegistered()
    {
        //System.out.println("Selection provider registered..");
    }

    public void providerUnregistered()
    {
    }

    private static class TestSelectionSession implements SelectionSession
    {
        Locator locator;

        TestSelectionSession(String locatorStr) throws InvalidLocatorException
        {
            locator = new OcapLocator(locatorStr);
        }

        public void destroy()
        {
            //System.out.println("TestSelectionSession destroy..");
        }

        public Locator select()
        {
            //System.out.println("TestSelectionSession select..locator: " + locator);
            return locator;
        }

        public void selectionReady()
        {
            //System.out.println("TestSelectionSession selectionReady..");
        }

        public long setPosition(long position)
        {
            return 0;
        }

        public float setRate(float newRate)
        {
            return 0;
        }

    }

    static class TestServiceDescription implements ServiceDescription
    {
        private Service service;

        TestServiceDescription(Service s)
        {
            service = s;
        }

        public DeliverySystemType getDeliverySystemType()
        {
            return DeliverySystemType.UNKNOWN;
        }

        public MultilingualString getLongName(String perferredLanguage)
        {
            return new MultilingualString(service.getName(), "eng");
        }

        public ServiceType getServiceType()
        {
            return service.getServiceType();
        }

        public Service getRealService()
        {
            return service;
        }
    }
}
