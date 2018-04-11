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

import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;

/**
 * Implementation specific extensions to <code>ServiceContextFactory</code>
 * 
 * @author Todd Earles
 */
public abstract class ServiceContextFactoryExt extends ServiceContextFactory
{
    /**
     * Enable or disable the ability to create {@link ServiceContext}s. If
     * <code>ServiceContext</code> creation is disabled, then any and all
     * attempts to {@link ServiceContextFactory#createServiceContext() create} a
     * <code>ServiceContext</code> shall fail.
     * 
     * @param enabled
     *            indicates whether <code>ServiceContext</code> creation should
     *            be enabled (if <code>true</code>) or disabled (if
     *            <code>false</code>)
     */
    public abstract void setCreateEnabled(boolean enabled);

    /**
     * Creates a <code>ServiceContext</code> object for the purpose of selecting
     * an auto-select abstract service. This <code>ServiceContext</code> is
     * automatically destroyed when no service remains selected.
     * 
     * @return A new <code>ServiceContext</code> object.
     */
    public abstract ServiceContext createAutoSelectServiceContext();

    /**
     * Provides all <code>ServiceContext</code> instances. This <i>may</i>
     * return instances of <code>ServiceContext</code> that are in the
     * <i>destroyed</i> state.
     * 
     * @return An array of <code>ServiceContext</code> objects.
     */
    public abstract ServiceContext[] getAllServiceContexts();

    /**
     * Create a listener for ServiceContext creation and deletion.
     * 
     * @param l
     *            The listener to add.
     */
    public abstract void addServiceContextLifetimeListener(ServiceContextLifetimeListener l);

    /**
     * Remove a listener for ServiceContext creation and deletion.
     * 
     * @param l
     *            The listener to remove.
     */
    public abstract void removeServiceContextLifetimeListener(ServiceContextLifetimeListener l);
}
