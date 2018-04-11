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

package org.cablelabs.impl.havi;

import java.awt.*;
import java.awt.event.*;
import org.havi.ui.*;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.ocap.ui.HSceneBinding;

/**
 * The {@link DisplayMediator} interface defines the mediator between the
 * portable {@link HScene}/{@link HSceneFactory} implementation and the
 * platform-specific parent of the {@link HScene}.
 * 
 * @author Todd Earles
 * @version $Revision: 1.7 $ $Date: 2002/06/03 21:32:55 $
 */
public interface DisplayMediator
{
    /**
     * Add the specified HScene to the HGraphicsDevice
     * 
     * @param scene
     *            the scene to add to the device
     */
    public void add(HScene scene);

    /**
     * Remove the specified HScene from the HGraphicsDevice
     * 
     * @param scene
     *            the scene to remove from the device
     */
    public void remove(HScene scene);

    /**
     * Get the container that represents the device associated with this
     * mediator.
     */
    public Container getContainer();

    /**
     * Brings the specified scene to the front of the associated
     * {@link HScene}s z-ordering.
     * 
     * @param scene
     *            the scene to pop to the front.
     */
    public void popToFront(HScene scene);

    /**
     * Moves the specified scene to the rear of the associated
     * {@link HScene}s z-ordering.
     * 
     * @param scene
     *            the scene to push to the rear
     */
    public void pushToRear(HScene scene);

    /**
     * Adds the specified key listener to receive key events from the associated
     * {@link HScene}. If <code>l</code> is null, no exception is thrown and no
     * action is performed.
     * 
     * @param scene
     *            the scene in question
     * @param l
     *            the key listener.
     */
    public void addKeyListener(HScene scene, KeyListener l);

    /**
     * Removes the specified key listener so that it no longer receives key
     * events from the associated {@link HScene}. This method performs no
     * function, nor does it throw an exception, if the listener specified by
     * the argument was not previously added. If <code>l</code> is null, no
     * exception is thrown and no action is performed.
     * 
     * @param scene
     *            the scene in question
     * @param l
     *            the key listener.
     */
    public void removeKeyListener(HScene scene, KeyListener l);

    /**
     * Check whether the specified component is on the device represented by
     * this mediator.
     * 
     * @param component
     *            the component to check
     * @return true if the component is currently a child of the device
     *         represented by this mediator. Otherwise, returns false.
     */
    public boolean isOnDevice(Component component);

    /**
     * Returns the component on the specified HScene which currently has focus.
     * 
     * @param scene
     *            the <code>HScene</code> to get the focusOwner for.
     * @return the component which currently has focus on the specified scene.
     */
    public Component getFocusOwner(HScene scene);

    /**
     * Implements <code>HaviToolkit.showVirtualKeyboard</code>. This method
     * blocks until {@link #hideVirtualKeyboard()} is called.
     */
    public boolean showVirtualKeyboard(HTextValue editComponent);

    /**
     * Hide the virtual keyboard and let the {@link #showVirtualKeyboard} method
     * return.
     */
    public void hideVirtualKeyboard();

    /**
     * Returns whether or not the proposed scene position and size is allowed by
     * the monitor application
     * 
     * @param scene
     *            the scene that is requesting a change in position/size
     * @param newScene
     *            the proposed new scene position and size
     * @return true if the change in size and/or position is allowed or if there
     *         is no registered monitor application, false if the change is not
     *         allowed
     */
    public boolean checkNewPositionAndSize(HSceneBinding move);

    /**
     * Returns whether or not the given scene is allowed to be visible by the
     * monitor application
     * 
     * @param scene
     *            the scene that is requesting to be displayed
     * @return true if the scene is allowed to be displayed or if there is no
     *         registered monitor application, false if the change is not
     *         allowed
     */
    public boolean checkShow(HSceneBinding show);

    /**
     * Returns whether or not the given scene is allowed to move to the front of
     * the z-order list
     * 
     * @param scene
     *            the scene that is requesting to be move to the front of the
     *            z-order list
     * @return true if the scene is allowed to move to the front of the z-order
     *         list or if there is not registered monitor application, false if
     *         the change is not allowed
     */
    public boolean checkPopToFront(HScene scene);

    /**
     * Create and return a <code>WindowEvent</code> with the given <i>scene</i>
     * as the source.
     * 
     * @param scene
     *            HScene that is the source of the event
     * @param eventId
     *            the event id
     * @return a <code>WindowEvent</code> with the given <i>scene</i> as the
     *         source.
     */
    public WindowEvent createWindowEvent(HScene scene, int eventId);

    /**
     * Allows for a privileged operation to override the visibility setting of
     * all HScenes on the display (with the exception of the EAS tune override
     * scene defined by OCAP.eas.tune.interrupt.appId stack property)
     * 
     * @param visible
     *            True if scenes shall be allowed to control their visibility
     *            via normal stack operation. False if all scenes should be
     *            individually hidden and requests for changes in visibility
     *            subsequently denied.
     */
    public void setVisible(boolean visible);
}
