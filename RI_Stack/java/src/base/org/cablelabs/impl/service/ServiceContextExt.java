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

import java.awt.Container;
import java.awt.Rectangle;
import java.util.List;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.cablelabs.impl.service.javatv.selection.ServiceContextDelegate;
import org.dvb.media.VideoTransformation;
import org.dvb.service.selection.DvbServiceContext;

/**
 * Implementation specific extensions to <code>ServiceContext</code>
 * 
 * @author Todd Earles
 */
public interface ServiceContextExt extends DvbServiceContext
{
    /**
     * Set whether this service context should automatically destroy itself when
     * it becomes idle.
     * 
     * @param destroyWhenIdle
     *            true if the service context should destroy itself when it
     *            becomes idle; otherwise, false. It is destroyed immediately if
     *            already idle. If the serviceContext is already destroyed, this
     *            is a no-op.
     */
    public void setDestroyWhenIdle(boolean destroyWhenIdle);

    /**
     * Determine if the service context is in the PRESENTING state.
     * 
     * @return true if the service context is presenting, otherwise false.
     */
    public boolean isPresenting();

    /**
     * Determine if the service context is in the DESTROYED state.
     * 
     * @return true if the service context is destroyed, otherwise false.
     */
    public boolean isDestroyed();

    /**
     * Enable/disable the persistent video mode.
     * 
     * @param enable
     *            is a boolean indicating whether the persistent video mode
     *            should be enabled (true) or disabled (false).
     */
    public void setPersistentVideoMode(boolean enable);

    /**
     * Enable/disable persistent application execution mode.
     * 
     * @param appsEnabled
     *            is a boolean indicating whether applications are enabled
     *            (true) or disabled (false).
     */
    public void setApplicationsEnabled(boolean appsEnabled);

    /**
     * Get the current state of the persistent video mode application execution
     * flag.
     * 
     * @return boolean indicating whether applications are enabled (true) or
     *         disabled (false).
     */
    public boolean isAppsEnabled();

    /**
     * Get the current state of the persistent video mode.
     * 
     * @return boolean indicating whether persistent mode is enabled (true) or
     *         disabled (false).
     */
    public boolean isPersistentVideoMode();

    /**
     * Perform a swap of the peristent video mode setting with the other
     * specified ServiceContext. The swap operation will be performed with the
     * state machine of the two service contexts locked and the locking will be
     * done in order respective of each service context's hash code. Which ever
     * service context has a numerically larger hash code will lock first and
     * then call back to the other service context to lock its state machine and
     * finish with the swap operation. Since, the swap is always completed by
     * calling the other service context, the audioUse flag may need to be
     * flipped in order to maintain the proper decoder association as the swap
     * trickles down the chain of classes involved (i.e.
     * ServiceContext->ServiceMediaHandler->Decoder).
     * 
     * @param sc
     *            is the other ServiceContextExt to swap with.
     * @param audioUse
     *            is a boolean indicating which ServiceContext's audio to
     *            present.
     * @param swapAppSettings
     *            is a boolean indicating whether the application enabled
     *            settings should be swapped.
     */
    public void swapSettings(ServiceContext sc, boolean audioUse, boolean swapAppSettings)
            throws IllegalArgumentException;

    /**
     * Get the <code>AppDomain</code> associated with this
     * <code>ServiceContext</code>.
     * 
     * @return The <code>AppDomain</code> currently associated with this
     *         <code>ServiceContext</code> or null if none.
     */
    public AppDomain getAppDomain();

    /**
     * Set the persistent video mode initial background vide transformation.
     * 
     * @param trans
     *            is the initial video transformation.
     */
    public void setInitialBackground(VideoTransformation trans);

    /**
     * Set the persistent video mode initial component video parameters.
     * 
     * @param parent
     *            is the parent container to add the component video to.
     * @param rect
     *            is the boundry rectangle of the component video relative to
     *            the specified parent container.
     */
    public void setInitialComponent(Container parent, Rectangle rect);

    /**
     * Returns the <code>CallerContext</code> that most recently called select.
     * <p>
     * <b>This is a KLUDGE! It has only been added for EAS and should not be
     * used in general. </b> It would be preferred if this method would go away
     * and EAS have a cleaner method of restoring previous service
     * presentations.
     * <p>
     * TODO: remove this method and its use altogether
     * 
     * @return the <code>CallerContext</code> that most recently called select;
     *         <code>null</code> is returned if there is no
     *         <code>CallerContext</code> currently associated with this
     *         <code>ServiceContext</code>
     */
    public CallerContext getCallerContext();

    /**
     * Returns the caller context that created this service context.
     * 
     * @return The <code>CallerContext</code> that created this ServiceContext.
     */
    public CallerContext getCreatingContext();

    /**
     * Add a {@link ServiceContextCallback} to the list of objects which are
     * synchronously notified of player changes. Notification is given in order
     * from highest to lowest priority.
     * 
     * @param callback
     *            The {@link ServiceContextCallback} to add.
     * @param priority
     *            The priority for this callback where a higher numerical value
     *            indicate a higher priority.
     * @return The current media player or null if none
     */
    public abstract ServiceMediaHandler addServiceContextCallback(ServiceContextCallback callback, int priority);

    /**
     * Remove the specified synchronous callback object (
     * {@link ServiceContextCallback}) from the list of registered callbacks.
     * Has no effect if the callback is not in the list.
     * 
     * @param callback
     *            The {@link ServiceContextCallback} to remove.
     */
    public abstract void removeServiceContextCallback(ServiceContextCallback callback);

    /**
     * Causes the <code>ServiceContext</code> to stop presenting content and
     * enter the <em>not presenting</em> state. Resources used in the
     * presentation will be released, associated
     * <code>ServiceContentHandlers</code> will cease presentation (
     * <code>ServiceMediaHandlers</code> will no longer be in the
     * <em>started</em> state), and a <code>PresentationTerminatedEvent</code>
     * will be posted.
     * <p>
     * 
     * This operation completes asynchronously. No action is performed if the
     * <code>ServiceContext</code> is already in the <em>not
     * presenting</em> state.
     */
    public void stopAbstractService();

    /**
     * Register ServiceContextDelegates available for use
     * 
     * @param serviceContextDelegates
     *            ordered list of {@link ServiceContextDelegate#}
     */
    void setAvailableServiceContextDelegates(List serviceContextDelegates);

    /**
     * Force EAS tune.  Prevents changes to ServiceContext or Player media time
     */
    void forceEASTune(Service service);

    /**
     * Un-force EAS tune.  Allows changes to ServiceContext and Player media time.
     */
    void unforceEASTune();
}
