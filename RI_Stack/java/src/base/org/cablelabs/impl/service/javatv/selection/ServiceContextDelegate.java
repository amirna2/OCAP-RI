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

package org.cablelabs.impl.service.javatv.selection;

import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.service.ServiceContextResourceUsageImpl;
import org.davic.net.tuning.NetworkInterface;

/**
 * Responsible for performing service-specific selection logic.
 */
public interface ServiceContextDelegate
{

    /**
     * Initialize the delegate. Must be called before
     * {@link #present}
     * 
     * @param serviceContextDelegateListener
     * @param creationDelegate
     * @param appDomain
     * @param videoModeSettings
     * @param lock
     */
    void initialize(ServiceContextDelegateListener serviceContextDelegateListener,
            ServiceMediaHandlerCreationDelegate creationDelegate, AppDomain appDomain,
            PersistentVideoModeSettings videoModeSettings, Object lock);

    /**
     * Prepare to present the service. This is a nonblocking method. Success or failure
     * will be discovered via a callback on the
     * {@link ServiceContextDelegateListener} passed in to
     * {@link ServiceContextDelegate#initialize}
     * <p/>
     * NOTE: This method may be called without a stopPresenting in between if
     * we're presenting the same service but possibly different
     * componentLocators
     *
     * @param service the service to present
     * @param componentLocators optional component locators of the service which should be presented
     * @param serviceContextResourceUsage the resourceUsage associated with the servicecontext
     * @param callerContext the application's callercontext
     * @param sequence a unique identifier associated with each call to present
     */
    void prepareToPresent(Service service, Locator[] componentLocators, ServiceContextResourceUsageImpl serviceContextResourceUsage,
                          CallerContext callerContext, long sequence);

    /**
     * Initiate presentation of the service and optional locators provided in prepareToPresent.
     * @param sequence the unique identifier matching the value provided in prepareToPresent.   
     *                 If sequence is different, no-op the method
     *              
     */
    void present(long sequence);
    
    /**
     * Stop presenting.  Must call notPresenting on the delegate if prepareToPresent was called even if present was not called.
     * @param sequence the unique identifier matching the value provided in prepareToPresent.   If sequence is different,
     *                 If sequence is different, no-op the stopPresenting call
     */
    void stopPresenting(long sequence);

    /**
     * Stop presenting the abstract service. No-op'd for all non-abstract
     * delegates.
     */
    void stopPresentingAbstractService();

    /**
     * Synchronously destroy all resources associated with the delegate.
     */
    void destroy();

    /**
     * Accessor
     * 
     * @return network interface
     */
    NetworkInterface getNetworkInterface();

    /**
     * Accessor
     * 
     * @return service content handlers for this delegate or a zero-length array
     *         (never null)
     */
    ServiceContentHandler[] getServiceContentHandlers();

    /**
     * Accessor
     * 
     * @return currently presenting service
     */
    Service getService();

    /**
     * Accessor
     * 
     * @return current media handler (may or may not have already notified of
     *         presenting) or null
     */
    ServiceMediaHandler getServiceMediaHandler();

    /**
     * Accessor, expected to be called only when the delegate is presenting a service.
     * 
     * @param service
     * 
     * @return true if delegate is presenting service
     */
    boolean isPresenting(Service service);

    /**
     * Report if this delegate can present the service.
     * 
     * @param service
     * 
     * @return true if delegate can present
     */
    boolean canPresent(Service service);
}
