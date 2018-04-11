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

import javax.tv.service.Service;

import org.cablelabs.impl.manager.service.ServiceCollection;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.davic.net.Locator;
import org.dvb.spi.Provider;
import org.dvb.spi.ProviderFailedInstallationException;
import org.dvb.spi.selection.SelectionSession;
import org.dvb.spi.selection.ServiceDescription;
import org.dvb.spi.selection.ServiceReference;
import org.ocap.net.OcapLocator;

/**
 * An instance of {@link ProviderInstance} is used to wrap and manage an
 * instance of {@link Provider}.
 * 
 * @author Todd Earles
 */
public abstract class ProviderInstance 
{       
    public ProviderInstance()
    {

    }
    
    /**
     * Initialize this instance. This method is called while executing in the
     * provider's caller context. Therefore, the implementation of this method
     * does not need to switch the caller context in order to call the provider.
     * Will be called explicitly by the ProviderRegistry at the time a 
     * system bound SelectionProvider is registered.
     */
    public abstract void init() throws ProviderFailedInstallationException;

    /**
     * Dispose of this instance.
     */
    public abstract void dispose();

    /**
     * create session given Service Reference and associated SPI Service
     */
    public abstract SelectionSession newSession(ServiceReference ref, final SPIService service);

    /**
     * get session 
     */
    public abstract SelectionSession getSelectionSession(SPIService service);
    
    public abstract String getLongName(final ServiceDescription description, final String preferredLanguage);
    
    public abstract Service getService(String scheme, int sourceID, String language);
    
    public abstract Service getService(String scheme, String name, String language);
    
    public abstract void getAllServices(ServiceCollection collection, boolean providerFirst, String language);
    
    public abstract Service getServiceByLocator(String scheme, OcapLocator locator, String language);
    
    public abstract void getAllProviderServices(ServiceCollection collection, boolean providerFirst, String language);
    
    // SelectionSession wrapper
    public abstract class SelectionSessionWrapper implements SelectionSession
    {
        // Description copied from SelectionSession
        public abstract Locator select();
        
        // Description copied from SelectionSession
        public abstract void destroy();

        // Description copied from SelectionSession
        public abstract void selectionReady();

        // Description copied from SelectionSession
        public abstract float setRate(final float newRate);

        // Description copied from SelectionSession
        public abstract long setPosition(final long newPosition);
        
        public abstract SPIService getSPIService();
        
        public abstract org.davic.net.Locator getMappedLocator();
        
        public abstract ServiceExt getMappedService();
        
        public abstract ServiceDetailsExt getMappedDetails();          
    }
}
