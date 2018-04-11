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

package org.davic.resources;

/**
 * This interface should be implemented by objects that use a scarce resource.
 */

public interface ResourceClient
{
    /**
     * A call to this operation informs the ResourceClient that another
     * application has requested the resource accessed via the proxy parameter.
     * If the ResourceClient decides to give up the resource as a result of
     * this, it should terminate its usage of proxy and return True, otherwise
     * False. requestData may be used to pass more data to the ResourceClient so
     * that it can decide whether or not to give up the resource, using
     * semantics specified outside this framework; for conformance to this
     * framework, requestData can be ignored by the ResourceClient.
     * 
     * @param proxy
     *            the ResourceProxy representing the scarce resource to the
     *            application
     * @param requestData
     *            application specific data
     * @return If the ResourceClient decides to give up the resource following
     *         this call, it should terminate its usage of proxy and return
     *         True, otherwise False.
     */

    public abstract boolean requestRelease(ResourceProxy proxy, Object requestData);

    /**
     * A call to this operation informs the ResourceClient that proxy is about
     * to lose access to a resource. The ResourceClient shall complete any
     * clean-up that is needed before the resource is lost before it returns
     * from this operation. This operation is not guaranteed to be allowed to
     * complete before notifyRelease() is called.
     * 
     * @param proxy
     *            the ResourceProxy representing the scarce resource to the
     *            application
     */

    public abstract void release(ResourceProxy proxy);

    /**
     * A call to this operation notifies the ResourceClient that proxy has lost
     * access to a resource. This can happen for two reasons: either the
     * resource is unavailable for some reason beyond the control of the
     * environment (e.g. hardware failure) or because the client has been too
     * long in dealing with a ResourceClient.release() call.
     * 
     * @param proxy
     *            the ResourceProxy representing the scarce resource to the
     *            application
     * 
     */

    public abstract void notifyRelease(ResourceProxy proxy);
}
