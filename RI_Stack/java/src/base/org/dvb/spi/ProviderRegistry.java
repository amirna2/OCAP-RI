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

package org.dvb.spi;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.spi.selection.SelectionProvider;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.service.SICacheImpl;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.SelectionProviderInstance;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * Registry of providers.
 * 
 * @since MHP 1.1.3
 * @author Todd Earles
 **/
public class ProviderRegistry
{

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(ProviderRegistry.class);

    /**
     * This constructor is provided for use by implementations and by other
     * specifications that extend this class. It is not to be used by normal
     * applications.
     **/
    protected ProviderRegistry()
    {
    }

    /** The singleton instance of the ProviderRegistry */
    private static ProviderRegistry registry = null;

    /**
     * Return the singleton provider registry as seen by the calling
     * application.
     * 
     * @return the provider registry
     */
    public static synchronized ProviderRegistry getInstance()
    {
        // Create the singleton instance if not yet created
        if (registry == null)
        {
            registry = new ProviderRegistry();
        }

        return registry;
    }

    /**
     * The current set of started provider instances
     */
    private static Vector startedProviders = new Vector();

    /**
     * Registers a provider. Note that providers might be installed
     * "automatically" by the terminal, e.g. due to signalling.
     * 
     * @param p
     *            the provider to register
     * @throws IllegalArgumentException
     *             if Provider does not export a valid set of services as
     *             determined by Provider.getServiceProviderInterfaces(), or if
     *             the provider does not have have a non-null Xlet context.
     * @throws ProviderFailedInstallationException
     *             if the organisation_id in the name of the provider does not
     *             match the organisation_id in a certificate which can
     *             authenticate the provider class.
     * @throws SecurityException
     *             if the caller, for all of the SPIs implemented by the
     *             provider, does not have a ProviderPermission whose name is
     *             the fully qualified name of the class returned by Provider
     *             getServiceProviderInterfaces and whose action is "xlet".
     * 
     * @see org.dvb.spi.Provider#getServiceProviderInterfaces()
     * @see org.dvb.spi.XletBoundProvider#getBoundXletContext()
     * @see org.dvb.spi.XletBoundProvider#getBoundPBPXletContext()
     **/
    public synchronized void registerXletBound(XletBoundProvider p) throws ProviderFailedInstallationException
    {
        throw new ProviderFailedInstallationException("No XletBoundProviders supported by this platform");
    }

    /**
     * Registers a provider. Note that providers might be installed
     * "automatically" by the terminal, e.g. due to signalling.
     * 
     * @param p
     *            the provider to register
     * @throws IllegalArgumentException
     *             if Provider does not expoort a valid set of services as
     *             determined by Provider.getServiceProviderInterfaces()
     * @throws ProviderFailedInstallationException
     *             if the organisation_id in the name of the provider does not
     *             match the organisation_id in a certificate which can
     *             authenticate the provider class.
     * 
     * @throws SecurityException
     *             if the caller, for all of the SPIs implemented by the
     *             provider, does not have a ProviderPermission whose name is
     *             the fully qualified name of the class returned by Provider
     *             getServiceProviderInterfaces and whose action is "system".
     * 
     * @see Provider#getServiceProviderInterfaces()
     **/
    public synchronized void registerSystemBound(SystemBoundProvider p) throws ProviderFailedInstallationException
    {
        if (log.isDebugEnabled())
        {
            log.debug(" ProviderRegistry::registerSystemBound: " + p);
        }

        // Verify the interfaces provided are allowed on this platform and the
        // calling app
        // has the corresponding provider permission.
        Class[] interfaces = p.getServiceProviderInterfaces();
        if (interfaces == null || interfaces.length < 1)
        {
            throw new IllegalArgumentException("No provider interfaces implemented by provider: " + p);
        }

        boolean bImplementInterface = false;

        for (int i = 0; i < interfaces.length; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug(" ProviderRegistry::registerSystemBound::interfaces.[" + i + "]");
            }

            if (!bImplementInterface && (interfaces[i] == SelectionProvider.class))
            {
                bImplementInterface = true;
            }

            // Check that the calling app has permission
            String name = interfaces[i].getName();

            if (log.isDebugEnabled())
            {
                log.debug(" ProviderRegistry::registerSystemBound::interfaces[i].getName(): " + name);
            }

            SecurityUtil.checkPermission(new ProviderPermission(name, "system"));
        }

        if (!bImplementInterface)
        {
            throw new IllegalArgumentException("SelectionProvider Interfaces Not Implemented ");
        }

        // Get the OID from the provider name
        String name = p.getName();
        if (log.isDebugEnabled())
        {
            log.debug(" ProviderRegistry::registerSystemBound::provider.getName(): " + name.toString());
        }

        int oid;
        if (!name.startsWith("0x"))
        {
            throw new ProviderFailedInstallationException("Malformed OID in provider name for provider:" + p);
        }
        
        try
        {
            oid = Integer.parseInt(name.substring(2, name.indexOf(".")), 16);
        }
        catch (Exception x)
        {
            throw new ProviderFailedInstallationException("Cannot parse OID for provider: " + p + " due to " + x);
        }

        // Verify OID
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        AppID appID = (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);
        if (appID != null && appID.getOID() != oid)
        {
            // TODO(Todd): Check other certificates for one with the expected
            // OID.
            throw new ProviderFailedInstallationException("Invalid OID in provider name for provider: " + p);
        }

        // Create a provider instance to manage this provider
        ProviderInstance providerInstance = null;
        if (p instanceof SelectionProvider) 
        {
            providerInstance = new SelectionProviderInstance((SelectionProvider) p);
        }

        // Check if this provider is already registered
        if (startedProviders.contains(providerInstance))
        {
            throw new ProviderFailedInstallationException("Provider already registered: " + p);
        }

        // Notify provider that it is now registered
        p.providerRegistered();

        // Initialize the provider instance
        providerInstance.init();

        // Add the provider instance (if one was created) to the list of started
        // providers.
        if (providerInstance != null) 
        {
            startedProviders.add(providerInstance);
        }
    }

    /**
     * Unregister a provider. Xlets that "manually" register a provider using
     * one of the register methods of this class shall unregister that provider
     * before returning from a successful destroyXlet call.
     * 
     * @param p
     *            the provider to unregister
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     **/
    public synchronized void unregister(Provider p)
    {
        // Find the provider instance with the application supplied provider
        Enumeration e = startedProviders.elements();
        while (e.hasMoreElements())
        {
            ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
            if(providerInstance instanceof SelectionProviderInstance)
            {
                SelectionProviderInstance spi = (SelectionProviderInstance)providerInstance;
                if(spi.getProvider() == p)
                {
                    // Remove the provider instance from the list of started
                    // providers
                    startedProviders.remove(providerInstance);

                    // Notify provider that it is now un-registered
                    p.providerUnregistered();

                    // Dispose of the provider instance
                    providerInstance.dispose();
                }                
            }
        }
    }

    /**
     * Return the names of all installed providers. These are the names returned
     * by the getName methods on those Providers. Provider names shall be
     * encoded as defined for permission request file in the main body of the
     * present document. For example "0x0000000B.EMV_PK11.VISA_REVOLVER".
     * 
     * @see org.dvb.spi.Provider#getName()
     * 
     * @return the names of all installed providers
     */
    public synchronized String[] getInstalledProviders()
    {
        String[] s = new String[startedProviders.size()];
        int index = 0;
        Enumeration e = startedProviders.elements();
        while (e.hasMoreElements())
        {
            ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
            if(providerInstance instanceof SelectionProviderInstance)
            {
                SelectionProviderInstance spi = (SelectionProviderInstance)providerInstance;
                s[index] = spi.getProviderName();
            }
        }
        return s;
    }

    /**
     * Return the version of an installed provider.
     * 
     * @see org.dvb.spi.Provider#getVersion()
     * 
     * @param provider
     *            the name of a provider as returned by the method
     *            getInstalledProviders
     * @return the version of the specified provider
     * @throws IllegalArgumentException
     *             if the provider name is not one of those installed, i.e. is
     *             not one returned from a call to getInstalledProviders
     **/
    public String getProviderVersion(String provider)
    {
        Enumeration e = startedProviders.elements();
        while (e.hasMoreElements())
        {
            ProviderInstance providerInstance = (ProviderInstance) e.nextElement();
            if(providerInstance instanceof SelectionProviderInstance)
            {
                SelectionProviderInstance spi = (SelectionProviderInstance)providerInstance;
                if (spi.getProviderName().equals(provider)) 
                {
                    return spi.getProviderVersion();
                }
            }
            
        }
        throw new IllegalArgumentException("Provider [" + provider + "] not registered.");
    }
    
}
