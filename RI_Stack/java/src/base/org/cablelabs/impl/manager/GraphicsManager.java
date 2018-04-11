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

package org.cablelabs.impl.manager;

import java.awt.Container;

import org.ocap.ui.HSceneManager;

import org.cablelabs.impl.dvb.ui.DVBBufferedImagePeer;

/**
 * A <code>Manager</code> that is responsible for initializing the graphics
 * subsystem as well as various other graphics-related responsibilities.
 * <p>
 * The <code>GraphicsManager</code> implementation currently does very little.
 * However, methods may be added in the future. For example:
 * <ul>
 * <li>Ability to display <i>splash</i> graphics on startup (related to
 * implementation).
 * <li>Ability to display <i>user alert</i> dialogs.
 * <li>Perhaps even the ability to display arbitrary components.
 * </ul>
 * 
 * @author Aaron Kamienski
 */
public interface GraphicsManager extends Manager
{
    /**
     * Returns the singleton instance <i>factory</i> object used to create new
     * <code>DVBBufferedImagePeer</code> instances. Returns an instance of the
     * implementation appropriate for a given platform.
     * 
     * @return an instance of <code>DVBBufferedImagePeer.Factory</code> suitable
     *         for the current runtime platform
     */
    public DVBBufferedImagePeer.Factory getImageFactory();

    /**
     * Returns the singleton instance of the <code>HSceneManager</code>. The
     * singleton <i>may</i> be implemented using application or implementation
     * scope.
     * 
     * @return HSceneManager
     */
    public HSceneManager getHSceneManager();

    /**
     * Retrieves the <code>Container</code> that represents the plane to be used
     * to render the Emergency Alert messages. The exact implementation of this
     * <code>Container</code> is unspecified.
     * <p>
     * The size of the retrieved container should not be modified. It will
     * automatically be adjusted to cover the maximum area of the screen allowed
     * by the implementation. The caller can make use of a
     * <code>ComponentListener</code> in order to be notified of changes in
     * size.
     * <p>
     * It is expected that only one <i>caller</i> access this container at a
     * time; otherwise the results are undefined.
     * <p>
     * It is expected that the <i>caller</i> limit its interaction with the
     * retrieved <code>Container</code> to the following methods:
     * <ul>
     * <li> {@link Container#setVisible}
     * <li> {@link Container#add}
     * <li> {@link Container#remove}
     * <li> {@link Container#setLayout}
     * <li> {@link Container#repaint}
     * <li> {@link Container#addComponentListener} (for being notified of size
     * changes)
     * <li> {@link Container#removeComponentListener}
     * </ul>
     * 
     * @return the <code>Container</code> representing the Emergency Alert
     *         display plane
     */
    public Container getEmergencyAlertPlane();

    /**
     * Retrieves the <code>Container</code> that represents the plane
     * to be used to render MMI dialog messages by the resident MMI
     * handler.  The exact implementation of this
     * <code>Container</code> is unspecified.
     * <p>
     * The size of the retrieved container should not be modified.
     * It will automatically be adjusted to cover the maximum area of the
     * screen allowed by the implementation.  The caller can make use of a
     * <code>ComponentListener</code> in order to be notified of changes in
     * size.
     * <p>
     * It is expected that only one <i>caller</i> access this container at
     * a time; otherwise the results are undefined.
     * <p>
     * It is expected that the <i>caller</i> limit its interaction with the
     * retrieved <code>Container</code> to the following methods:
     * <ul>
     * <li> {@link Container#setVisible}
     * <li> {@link Container#add}
     * <li> {@link Container#remove}
     * <li> {@link Container#setLayout}
     * <li> {@link Container#repaint}
     * <li> {@link Container#addComponentListener} (for being notified of size changes)
     * <li> {@link Container#removeComponentListener}
     * </ul>
     *
     * @return the <code>Container</code> representing the MMI
     *         display plane
     */
    public Container getMMIDialogPlane();

    /**
     * Adjusts the visibility of applications' graphics based upon the given
     * <i>visible</i> parameter.
     * <p>
     * The intention of this method is to allow the Emergency Alert subsystem to
     * hide application graphics, if deemed necessary. Graphics would most
     * likely be made invisible in order to ensure that the EAS details channel
     * is visible.
     * <p>
     * The exact manner in which this is implemented is not specified. Some
     * possibilities include:
     * <ul>
     * <li>Hide all application graphics on all screens.
     * <li>Hide all scenes on all screens.
     * <li>Hide all application graphics on the default screen and graphics
     * device.
     * <li>Hide all scenes on the default screen and graphics device.
     * </ul>
     * <p>
     * If graphics visibility already matches the given parameter, then this
     * method has no effect.
     * <p>
     * TODO(AaronK): It may make sense to move this to HGraphicsDevice subclass.
     * The question then would be how do we decide which graphics device? Some
     * possibilities include:
     * <ul>
     * <li>Do all of them (in which case, it's no better than this method).
     * <li>Default device on default screen (in which case, it's no better than
     * this method).
     * <li>All graphics devices on the screen that contains the video device
     * associated with the JMF player used for presentation.
     * </ul>
     * 
     * @param visible
     *            if <code>true</code> then application graphics should be made
     *            visible; if <code>false</code>, then application graphics
     *            should be made invisible
     */
    public void setVisible(boolean visible);
}
