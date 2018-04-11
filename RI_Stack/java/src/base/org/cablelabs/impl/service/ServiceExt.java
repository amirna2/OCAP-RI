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

package org.cablelabs.impl.service;

import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceMinorNumber;
import javax.tv.service.ServiceNumber;
import javax.tv.service.navigation.ServiceDetails;

import org.cablelabs.impl.util.string.MultiString;

/**
 * Implementation specific extensions to <code>Service</code>
 * 
 * @author Todd Earles
 */
public abstract class ServiceExt implements UniqueIdentifier, Service, ServiceNumber, ServiceMinorNumber,
        LanguageVariant, LocatorVariant
{
    /**
     * Create a snapshot of this <code>Service</code> and associate it with the
     * specified SI cache.
     * 
     * @param siCache
     *            The cache this snapshot is to be associated with
     * @return A copy of this object associated with <code>siCache</code>
     * @throws UnsupportedOperationException
     *             If creation of a snapshot is not supported
     */
    public abstract Service createSnapshot(SICache siCache);

    /**
     * Returns the handle that identifies this <code>Service</code> within the
     * SI database.
     * 
     * @return The service handle or null if not available via the SIDatabase.
     */
    public abstract ServiceHandle getServiceHandle();

    /**
     * Same as {@link Service#getName()} except this method returns all language
     * variants as a {@link MultiString}.
     * 
     * @return The multi-string or null if not available.
     */
    public abstract MultiString getNameAsMultiString();

    /**
     * This is specific to DSG application tunnels. If the service refers to an
     * app tunnel register interest in PSI acquisition.
     */
    public abstract void registerForPSIAcquisition();

    /**
     * This is specific to DSG application tunnels. If the service refers to an
     * app tunnel Un-register interest in PSI acquisition.
     */
    public abstract void unregisterForPSIAcquisition();

    /**
     * This method returns additional information about the <code>Service</code>
     * . This information is retrieved from the broadcast service information.
     * <p>
     * Note that if the content represented by this <code>Service</code> is
     * delivered on multiple transport-dependent streams there may be multiple
     * <code>ServiceDetails</code> for it. This method retrieves one of them
     * based on availability or user preferences. If access to all possible
     * <code>ServiceDetails</code> is required, the service <code>Locator</code>
     * can be transformed to transport-dependent <code>Locator</code> instances
     * and <code>ServiceDetails</code> can be retrieved for each.
     * <p>
     * This method returns data synchronously.
     * 
     * @return A <code>ServiceDetails</code> object for this
     *         <code>Service</code>.
     * @throws SIRequestException
     *             The retrieval failed
     * @throws InterruptedException
     *             The operation was interrupted before it completed
     * @see Service#retrieveDetails(SIRequestor)
     */
    public ServiceDetails getDetails() throws SIRequestException, InterruptedException
    {
        // Create the requestor
        SIRequestorImpl requestor = new SIRequestorImpl();

        // Make the request and wait for it to finish
        synchronized (requestor)
        {
            retrieveDetails(requestor);
            requestor.waitForCompletion();
        }

        // Return the results
        return (ServiceDetails) requestor.getResults()[0];
    }
}
