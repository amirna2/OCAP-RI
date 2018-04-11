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

package org.cablelabs.impl.signalling;

import java.util.Properties;

import org.dvb.application.AppID;

/**
 * An implementation of this interface represents an Application Information
 * Table describing bound applications. This application signalling will have
 * been acquired via in-band signalling.
 * 
 * @see "OCAP-1.0: 11 Application Signalling"
 * @author Aaron Kamienski
 */
public interface Ait
{
    /**
     * Returns the applications that are available to the implementation
     * 
     * @return set of applications available to this implementation.
     */
    public AppEntry[] getApps();

    /**
     * Retrieves a representation of the external application authorization
     * descriptors specified in the AIT. The external application authorization
     * descriptors are represented as an array of
     * <code>ExternalAuthorization</code> objects.
     * 
     * @return an array of <code>ExternalAuthorization</code> objects
     *         representing the external authorization for the associated
     *         service
     */
    public ExternalAuthorization[] getExternalAuthorization();

    /**
     * Returns the version number of this Ait between 0 and 31
     * 
     * @return the Ait version
     */
    public int getVersion();
    
    /**
     * Filter the signaled applications using the addressable X/AIT rules
     * defined in OCAP Section 11.2.2.5.  Also filters duplicate AppIDs
     * as defined in OCAP Section 11.2.1.10
     * 
     * @param securityProps the current list of security (CableCARD) properties
     *        used for addressable X/AIT filtering
     * @param registeredProps the current list of properties registered by
     *        privileged applications used for addressable X/AIT filtering
     * @return true if the set of filtered apps has changed since the last time
     *         this method was called, false if no change was made
     */
    public boolean filterApps(Properties securityProps, Properties registeredProps);

    /**
     * A representation of a single entry in the
     * <i>external_authorization_descriptor</i>.
     * 
     * @author Aaron Kamienski
     */
    public static class ExternalAuthorization
    {
        public AppID id;

        public int priority;
    }
}
