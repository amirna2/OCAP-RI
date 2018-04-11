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

package org.cablelabs.impl.havi.port.mpe;

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.ExtendedGraphicsDevice;
import org.cablelabs.impl.havi.DisplayMediator;

import org.havi.ui.HScreen;
import org.havi.ui.HVideoConfiguration;

/**
 * An implementation of {@link HaviToolkit} for the MPE port. This is an
 * extension of {@link Toolkit} which changes the way that <code>HScreen</code>,
 * <code>HScreenDevice</code>, and relevant configurations are accessed. The
 * difference being that they are now based upon native MPE apis.
 * <p>
 * It is anticipated that at some point the functionality of this class be
 * merged back into it's parent (i.e., the <code>Toolkit</code> class), and the
 * classes that this references becoming the main version.
 * 
 * @author Aaron Kamienski
 */
public class HDToolkit extends Toolkit
{
    /** Not publicly instantiable. */
    protected HDToolkit()
    {
        /* Empty */
    }

    /**
     * Return a new toolkit object
     */
    public static HaviToolkit getToolkit()
    {
        return new HDToolkit();
    }

    // Definition copied from superclass
    public HVideoConfiguration getVideoNotContributing()
    {
        return HDScreen.getVideoNotContributing();
    }

    // Definition copied from superclass
    public HScreen[] getHScreens()
    {
        return HDScreen.getHScreens();
    }

    // Definition copied from superclass
    public HScreen getDefaultHScreen()
    {
        return HDScreen.getDefaultHScreen();
    }

    /**
     * Retrieves a container that is always above all <code>HScenes</code> that
     * may be displayed below it. This isn't currently used within the HAVi
     * implementation directly, but may be used outside the HAVi implementation
     * for various purposes. Including, but not limited to:
     * <ul>
     * <li>Display of system splash screens
     * <li>Display of Emergency Alert Messages.
     * <li>Display of system dialogs
     * </ul>
     * <p>
     * If no pane is supported, then <code>null</code> will be returned. Every
     * call should return the same instance of <code>Container</code>.
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
     * <li> {@link java.awt.Component#setVisible}
     * <li> {@link java.awt.Container#add(java.awt.Component)}
     * <li> {@link java.awt.Container#remove(java.awt.Component)}
     * <li> {@link java.awt.Container#setLayout}
     * <li> {@link java.awt.Component#repaint()}
     * <li> {@link java.awt.Component#addComponentListener} (for being notified
     * of size changes)
     * <li> {@link java.awt.Component#removeComponentListener}
     * </ul>
     * 
     * @return the <code>Container</code> a default window pane suitable for
     *         non-HAVi use; or <code>null</code> if no such
     *         <code>Container</code> is supported
     */
    public java.awt.Container getDefaultWindowPane()
    {
        HScreen screen = HDScreen.getDefaultHScreen();
        HDGraphicsDevice gfx = (HDGraphicsDevice) screen.getDefaultHGraphicsDevice();

        return gfx.getWindowPane();
    }

    /**
     * Overrides super implementation such that the application graphics on the
     * default screen's default graphics device are hidden or made visible.
     * 
     * @see org.cablelabs.impl.havi.HaviToolkit#setGraphicsVisible(boolean)
     */
    public void setGraphicsVisible(boolean visible)
    {
        HScreen screen = HDScreen.getDefaultHScreen();
        DisplayMediator dm = ((ExtendedGraphicsDevice) screen.getDefaultHGraphicsDevice()).getDisplayMediator();

        dm.setVisible(visible);
    }
}
