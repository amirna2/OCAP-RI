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

/**
 * Abstract interface for all DVB providers.
 * 
 * @since MHP 1.1.3
 **/

public interface Provider
{

    /**
     * Returns the name of this provider. This can be used for debugging
     * purposes, e.g. in a crash log. The name shall be encoded as defined for
     * the permission request file in the main body of the present document. For
     * example "0x0000000B.EMV_PK11.VISA_REVOLVER".
     * 
     * @return the name
     **/
    public String getName();

    /**
     * Return the version of this provider. The format of this string is not
     * specified.
     * 
     * @return the version of this provider.
     **/
    public String getVersion();

    /**
     * Gives a list of the SPI's implemented by this provider. The list shall be
     * at least one element long, and shall contain only valid SPI interfaces
     * defined by the terminal specification. Unknown interfaces (e.g.
     * application-defined interfaces) shall be rejected, as documented in
     * ProviderRegistry.
     * 
     * @return a list of SPIs
     * @see org.dvb.spi.ProviderRegistry#registerXletBound(org.dvb.spi.XletBoundProvider)
     * @see org.dvb.spi.ProviderRegistry#registerSystemBound(org.dvb.spi.SystemBoundProvider)
     **/
    public Class[] getServiceProviderInterfaces();

    /**
     * Called by the system when this provider is registered.
     **/
    public abstract void providerRegistered();

    /**
     * Called by the system when this provider is unregistered.
     **/
    public abstract void providerUnregistered();
}
