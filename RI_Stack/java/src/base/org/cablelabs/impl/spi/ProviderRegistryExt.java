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

package org.cablelabs.impl.spi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.service.ServiceCollection;
import org.dvb.spi.ProviderRegistry;
import org.dvb.spi.selection.LocatorScheme;
import org.ocap.net.OcapLocator;

public class ProviderRegistryExt extends ProviderRegistry
{       
    // used for log messages
    private static final Logger log = Logger.getLogger(ProviderRegistryExt.class);
    
    /**
     * Maps each locator scheme to the providers which handle the scheme. The
     * key is the scheme name and the value is a vector of providers which
     * support the scheme.
     */
    private final Hashtable providersByScheme = new Hashtable();
    
    // Singleton ProviderRegistryExt
    private static final ProviderRegistryExt registryExt = new ProviderRegistryExt();
    
    private ProviderRegistryExt()
    {
        super();
    }
    
    public static ProviderRegistry getInstance()
    {        
        return registryExt;
    }
    
    // Check if the given scheme is registered by any SelectionProvider
    public boolean isLocatorSchemeRegistered(String scheme)
    {
        // Look up the scheme in the list of providers registered
        synchronized (providersByScheme)
        {
            // Get the vector of providers 
            Vector v = (Vector) providersByScheme.get(scheme);
            if(v == null)
            {
                return false;
            }
            Enumeration e = v.elements();
            while (e.hasMoreElements())
            {                
                ProviderInstance provider = (ProviderInstance) e.nextElement();
                // For now this method is only called when a HN ServiceResolutionHandler
                // tries to register and "ocap" scheme is already registered by a
                // SPI provider, the ServiceResolutionHandler is prevented from
                // registering this scheme
                if(provider instanceof SelectionProviderInstance)
                {
                    // If a SelectionProvider registered this scheme return true
                    return true;                  
                }
            }    
        }
        return false;
    }
    
    // Register a provider and the locator schemes it supports
    public void registerProviderInstance(ProviderInstance provider, LocatorScheme[] schemes)
    {
        // Register the schemes supported by this provider instance
        synchronized (providersByScheme)
        {
            for (int i = 0; i < schemes.length; i++)
            {
                // Get the scheme name and verify it is valid for this platform
                String name = schemes[i].getScheme();
                if (log.isDebugEnabled())
                {
                    log.debug("ProviderRegistryExt locatorScheme: " + name);
                }

                // Get the vector of providers for this scheme name; creating
                // one if necessary.
                Vector v = (Vector) providersByScheme.get(name);
                if (v == null)
                {
                    v = new Vector();
                    providersByScheme.put(name, v);
                }

                // Add the provider to the list of providers supporting this scheme name
                if(provider instanceof SelectionProviderInstance && name.equals("ocap"))
                {
                    // add at index 0 if this is SPI provider for "ocap" scheme (SPI provider should be
                    // above SRH in the provider list)
                    v.add(0, provider);
                }
                else
                {
                    v.add(provider);                    
                }
            }
        }        
    }
    
    // Unregister the provider and the locator schemes it supports
    public void unregisterProviderInstance(ProviderInstance provider, LocatorScheme[] schemes)
    {
        synchronized (providersByScheme)
        {
            // Unregister the schemes supported by this provider
            for (int i = 0; i < schemes.length; i++)
            {
                // Get the vector of providers for this scheme name.
                String name = schemes[i].getScheme();
                Vector v = (Vector) providersByScheme.get(name);
                if (v == null) continue;

                // Remove this provider from the list of providers supporting
                // this scheme name
                v.remove(provider);

                // Discard the vector if no remaining providers for this scheme
                // name
                if (v.size() == 0) providersByScheme.remove(name);
            }
        }  
    }
    
    // This method is called by SIManager getService()
    public Service getServiceByLocator(String scheme, OcapLocator locator, String language)
    {
        // Check parameters
        if (scheme == null) 
        {
            throw new IllegalArgumentException("Invalid scheme - null");
        }

        synchronized (providersByScheme)
        {
            // Get the list of providers which support the locator scheme. If no
            // providers support
            // the scheme then there are no service lists to check.
            Vector v = (Vector) providersByScheme.get(scheme);
            if (v == null || v.size() == 0) 
            {
                return null;
            }

            // Check each provider for the service
            Enumeration e = v.elements();
            while (e.hasMoreElements())
            {
                ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
                if (log.isDebugEnabled())
                {
                    log.debug("ProviderRegistryExt calling getServiceByLocator on providerInstance: " + providerInstance);
                }
                return providerInstance.getServiceByLocator(scheme, locator, language);
            }
        }
        return null;
    }

    // This method is called by SIManager getService()
    public Service getService(String scheme, int sourceID, String language)
    {
        // Check parameters
        if (scheme == null) 
        {
            throw new IllegalArgumentException("Invalid scheme - null");
        }

        synchronized (providersByScheme)
        {
            // Get the list of providers which support the locator scheme. If no
            // providers support
            // the scheme then there are no service lists to check.
            Vector v = (Vector) providersByScheme.get(scheme);
            if (v == null || v.size() == 0) 
            {
                return null;
            }

            // Check each provider for the service
            Enumeration e = v.elements();
            while (e.hasMoreElements())
            {
                ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
                if (log.isDebugEnabled())
                {
                    log.debug("ProviderRegistryExt calling getService on providerInstance: " + providerInstance);
                }
                return providerInstance.getService(scheme, sourceID, language);
            }
        }
        return null;
    }

    // This method is called by SIManager getService()
    public Service getService(String scheme, String name, String language)
    {
        // Check parameters
        if (scheme == null) 
        {
            throw new IllegalArgumentException("Invalid scheme - null");
        }

        synchronized (providersByScheme)
        {
            // Get the list of providers which support the locator scheme. If no
            // providers support
            // the scheme then there are no service lists to check.
            Vector v = (Vector) providersByScheme.get(scheme);
            if (v == null || v.size() == 0) 
            {
                return null;
            }

            // Check each provider for the service
            Enumeration e = v.elements();
            while (e.hasMoreElements())
            {
                ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
                if (log.isDebugEnabled())
                {
                    log.debug("ProviderRegistryExt calling getService on providerInstance: " + providerInstance);
                }
                return providerInstance.getService(scheme, name, language);
            }
        }
        return null;
    }
    
    // This method is called by SIManager filterServices()
    public void getAllServices(ServiceCollection collection, boolean providerFirst, String language)
    {
        synchronized (providersByScheme)
        {
            // Process each scheme
            Enumeration e = providersByScheme.elements();
            while (e.hasMoreElements())
            {
                // Process each provider within the scheme
                Vector v = (Vector) e.nextElement();
                Enumeration e2 = v.elements();
                while (e2.hasMoreElements())
                {
                    // Add each service provided by this provider
                    ProviderInstance providerInstance = (ProviderInstance) e2.nextElement();
                    
                    providerInstance.getAllServices(collection, providerFirst, language);
                }
            }
        }
    }
}
