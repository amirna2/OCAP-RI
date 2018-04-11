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

package org.ocap.resource;

import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * <P>
 * This class manages a means of resolving a resource contention.
 * </P>
 * <P>
 * An application which has a MonitorAppPermission ("handler.resource") may have
 * a subclass of the AppsDatabaseFilter class or a class implementing the
 * ResourceContentionHandler interface, and may set an instance of them in the
 * ResourceContentionManager. The concrete class of the AppsDatabaseFilter class
 * identifies an application that is not allowed absolutely to reserve the
 * resource. The class implementing the ResourceContentionHandler interface
 * resolves a resource contention after a resource negotiation.
 * </P>
 * <P>
 * See the section 19 Resource Management in this specification for details.
 *</P>
 * 
 * @author Aaron Kamienski
 */
public class ResourceContentionManager
{
    /**
     * A constructor of this class. An application must use the
     * {@link ResourceContentionManager#getInstance} method to create an
     * instance.
     */
    protected ResourceContentionManager()
    {
    }

    /**
     * This method returns an instance of the ResourceContentionManager class.
     * It is not required to be a singleton manner.
     * 
     * @return The ResourceContentionManager instance.
     */
    public static ResourceContentionManager getInstance()
    {
        ResourceManager em = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        return em.getContentionManager();
    }

    /**
     * This method sets an instance of a concrete class that extends
     * AppsDatabaseFilter. The AppsDatabaseFilter.accept(AppID) method returns
     * true if an application specified by the AppID is allowed to reserve the
     * resource, and returns false if the application is not allowed to reserve
     * it. At most, only one AppsDatabaseFilter is set for each type of
     * resource. Multiple calls of this method replace the previous instance by
     * a new one. If an AppsDatabaseFilter has not been associated with the
     * resource, then any application is allowed to reserve the resource. By
     * default, no AppsDatabaseFilter is set, i.e., all applications are allowed
     * to reserve the resource.
     * 
     * @param filter
     *            the AppsDatabaseFilter to deny the application reserving the
     *            specified resource. If null is set, the AppsDatabaseFilter for
     *            the specified resource will be removed.
     * 
     * @param resourceProxy
     *            A full path class name of a concrete class of the
     *            org.davic.resources.ResourceProxy interface. It specifies a
     *            resource type that the specified AppsDatabaseFilter filters.
     *            For example,
     *            "org.davic.net.tuning.NetworkInterfaceController".
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    public void setResourceFilter(org.dvb.application.AppsDatabaseFilter filter, java.lang.String resourceProxy)
    {
        // this method should not be directly called - the RezMgr subclassed
        // version should be called instead
        return;
    }

    /**
     * This method sets the specified ResourceContentionHandler that decides
     * which application shall be denied reserving a scarce resource. At most
     * only one instance of ResourceContentionHandler can be set. Multiple calls
     * of this method replace the previous instance by a new one. By default, no
     * ResourceContentionHandler is set, i.e. the
     * {@link ResourceContentionHandler#resolveResourceContention} method is not
     * called.
     * 
     * @param handler
     *            the ResourceContentionHandler to be set. If null is set, the
     *            ResourceContentionHandler instance will be removed and the
     *            {@link ResourceContentionHandler#resolveResourceContention}
     *            method will not be called.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    public void setResourceContentionHandler(ResourceContentionHandler handler)
    {
        // this method should not be directly called - the RezMgr subclassed
        // version should be called instead
        return;
    }

    /**
     * Gets the warning period set by the setWarningPeriod method.
     * 
     * @return The warning period in milli-seconds.
     */
    public int getWarningPeriod()
    {
        // this method should not be directly called - the RezMgr subclassed
        // version should be called instead
        return -1;
    }

    /**
     * Sets the warning period used by the implementation to determine when to
     * call the resourceContentionWarning method in a registered
     * {@link ResourceContentionHandler}. If the parameter is zero the
     * implementation SHALL NOT call the resourceContentionWarning method. If
     * the parameter is non-zero the implementation SHALL call the
     * resourceContentionWarning method if it has enough information to do so.
     * Setting the warningPeriod to non-zero MAY NOT cause the
     * resourceContentionWarning method to be called for two reasons, 1) the
     * implementation cannot determine when contention is going to happen, and
     * 2) the warning period is longer than the duration to the contention.
     * 
     * @param warningPeriod
     *            New warning period in milli-seconds. If the value is smaller
     *            than the minimum clock resolution supported by the
     *            implementation, the implementation MAY round it up to the
     *            minimum.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is negative.
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.resource").
     */
    public void setWarningPeriod(int warningPeriod) throws IllegalArgumentException
    {
        // this method should not be directly called - the RezMgr subclassed
        // version should be called instead
        return;
    }

}
