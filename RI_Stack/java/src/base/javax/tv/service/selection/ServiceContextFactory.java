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

package javax.tv.service.selection;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;

/**
 * This class serves as a factory for the creation of
 *<code>ServiceContext</code> objects.
 */
public abstract class ServiceContextFactory
{
    /**
     * Creates a <code>ServiceContextFactory</code>.
     */
    protected ServiceContextFactory()
    {
    }

    /**
     * Provides an instance of <code>ServiceContextFactory</code>.
     * 
     * @return An instance of <code>ServiceContextFactory</code>.
     */
    public static ServiceContextFactory getInstance()
    {
        // Get the actual service context factory implementation object by
        // asking the service manager for it.
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        return sm.getServiceContextFactory();
    }

    /**
     * Creates a <code>ServiceContext</code> object. The new
     * <code>ServiceContext</code> is created in the <em>not
     * presenting</em> state.
     * 
     * <p>
     * Due to resource restrictions, implementations may limit the total number
     * of simultaneous <code>ServiceContext</code> objects. In such a case,
     * <code>InsufficientResourcesException</code> is thrown.
     * 
     * @return A new <code>ServiceContext</code> object.
     * 
     * @throws InsufficientResourcesException
     *             If the receiver lacks the resources to create this
     *             <code>ServiceContext</code>.
     * 
     * @throws SecurityException
     *             if the caller doesn't have
     *             <code>ServiceContextPermission("create", "own")</code>.
     * 
     * @see ServiceContextPermission
     */
    public abstract ServiceContext createServiceContext() throws InsufficientResourcesException, SecurityException;

    /**
     * Provides the <code>ServiceContext</code> instances to which the caller of
     * the method is permitted access. If the caller has
     * <code>ServiceContextPermission("access","*")</code>, then all current
     * (i.e., undestroyed) <code>ServiceContext</code> instances are returned.
     * If the application making this call is running in a
     * <code>ServiceContext</code> and has
     * <code>ServiceContextPermission("access","own")</code>, its own
     * <code>ServiceContext</code> will be included in the returned array. If no
     * <code>ServiceContext</code> instances are accessible to the caller, a
     * zero-length array is returned. No <code>ServiceContext</code> instances
     * in the <em>destroyed</em> state are returned by this method.
     * 
     * @return An array of accessible <code>ServiceContext</code> objects.
     * 
     * @see ServiceContextPermission
     */
    public abstract ServiceContext[] getServiceContexts();

    /**
     * Reports the <code>ServiceContext</code> in which the <code>Xlet</code>
     * corresponding to the specified <code>XletContext</code> is running. The
     * returned <code>ServiceContext</code> is the one from which the
     * <code>Service</code> carrying the <code>Xlet</code> was selected, and is
     * the Xlet's "primary" <code>ServiceContext</code>.
     * 
     * @param ctx
     *            The <code>XletContext</code> of the <code>Xlet</code> of
     *            interest.
     * 
     * @return The <code>ServiceContext</code> in which the <code>Xlet</code>
     *         corresponding to <code>ctx</code> is running.
     * 
     * @throws SecurityException
     *             If the caller does not have
     *             <code>ServiceContextPermission("access", "own")</code>.
     * 
     * @throws ServiceContextException
     *             If the <code>Xlet</code> corresponding to <code>ctx</code> is
     *             not running within a <code>ServiceContext</code>.
     * 
     * @see ServiceContextPermission
     */
    public abstract ServiceContext getServiceContext(javax.tv.xlet.XletContext ctx) throws SecurityException,
            ServiceContextException;
}
