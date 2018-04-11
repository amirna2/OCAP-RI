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

/**
 * Platform implementations that wish to receive notifications from a
 * <code>SelectionProvider</code> register an instance that implements this
 * interface with the <code>SelectionProvider</code>.
 * 
 * @since MHP 1.1.3
 **/
public interface SelectionProviderContext
{

    /**
     * Called by a source when the list of services changes. The new list
     * replaces any previous list.
     * <p>
     * The services passed into this method shall be compared with the service
     * list returned from the call to getServiceList when this provider was
     * first registered. Where services are added, these shall be merged into
     * the platform service list as defined in the description of the
     * getServiceList method. Where services are removed, if the transport
     * independent service is left with no transport dependent services then it
     * shall be removed from the platform service list. In all cases where the
     * platform service list changes, SIChangeEvents shall be generated to
     * appropriate listeners.
     * 
     * @param serviceReferences
     *            The ServiceReference instances for the available services.
     *            This array must not change after this method is invoked.
     **/
    public void serviceListChanged(ServiceReference[] serviceReferences);

    /**
     * Called by a source when service description information is available to
     * offer that information to the platform implementation. The new list
     * replaces any previous list.
     * 
     * @param serviceReferences
     *            The ServiceReference instances for which descriptions are
     *            available. This array must not change after this method is
     *            invoked.
     **/
    public void serviceDescriptionAvailable(ServiceReference[] serviceReferences);

    /**
     * Called by a source to update the details of a service it supports. This
     * includes changing the service between one whose location is already known
     * and one whose location must be found when it is selected. It also
     * includes changing the location of a service whose location is already
     * known. ServiceReferences shall be matched using the transport independent
     * and transport dependent names.
     * 
     * @param service
     *            the new service reference
     * @throws IllegalArgumentException
     *             if a service with the same service identifier and transport
     *             independent locator has not been previously returned by this
     *             source to the implementation
     */
    public void updateService(ServiceReference service);

}
