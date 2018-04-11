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

package org.ocap.media;

import java.awt.Rectangle;
import java.awt.Container;
import javax.media.Player;
import javax.media.Control;
import org.havi.ui.HVideoComponent;

/**
 * This interface represents an optional JMF control that can be returned from a
 * {@link Player}. This {@link Control} MAY be supported by an implementation
 * that can support a video <code>Component</code> in a limited set of
 * coordinates and sizes. When this {@link Control} is supported the
 * corresponding <code>Player.getVisualComponent</code> method SHALL return
 * null. </p>
 * <p>
 * The <code>setSize</code> and <code>setBounds</code> methods contained in a
 * <code>HVideoComponent</code> instance returned from the
 * <code>getVisualComponent</code> method in this interface SHALL do nothing
 * when passed values not supported by the implementation. An application can
 * determine supported values by calling the <code>getBestFit</code> method in
 * this interface.
 * </p>
 */
public interface VideoComponentControl extends Control
{
    /**
     * Gets a visual component associated with the <code>Player</code> this
     * <code>Control</code> is contained in. Except for returning a
     * <code>Component</code> that can be sized and positioned arbitrarily, this
     * method behaves in the same manner as
     * <code>Player.getVisualComponent</code> and as specified in MHP 1.0.3 [9].
     * 
     * @return Video <code>Component</code> for the <code>Player</code>.
     *         Implementations may limit the number of
     *         <code>HVideoComponent</code> instances supported simultaneously.
     *         Consequentely, subsequent calls to this method MAY return null.
     */
    public HVideoComponent getVisualComponent();

    /**
     * Gets the best fit relative to a given parent. If the implementation can
     * match the desired fit exactly it SHALL do so. "Best fit" determination is
     * implementation specific. Note that if the application alters the parent
     * after this method is called the implementation may alter any attached
     * video <code>Component</code> in accordance with possible implementation
     * support.
     * 
     * @param parent
     *            Container to be used as the parent for best fit determination.
     * @param desired
     *            Rectangle with size and coordinates to be used for best fit
     *            determination.
     * 
     * @return Rectangle with best fit size and coordinates.
     * 
     * @throws IllegalArgumentException
     *             if the desired <code>Rectangle</code> is not within the
     *             bounds of the parent <code>Container</code>.
     */
    public Rectangle getBestFit(Container parent, Rectangle desired) throws IllegalArgumentException;
}
