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

import java.awt.Dimension;
import org.havi.ui.HScreenRectangle;

import org.cablelabs.impl.util.NativeHandle;

/**
 * A data structure which contains the data elements common to all device
 * configurations. This has been separated out so that each configuration
 * implementation doesn't have to provide the same implementation.
 * <p>
 * The default-access (i.e., <i>package private</i>) fields of this class are
 * initialized at construction time using a native method. Thereafter, they can
 * be accessed by the referencing <code>HScreenConfiguration</code>.
 * 
 * Represents the os_Config structure.
 * 
 * @see HDBackgroundConfiguration
 * @see HDGraphicsConfiguration
 * @see HDVideoConfiguration
 * @see HDStillImageBackgroundConfiguration
 * 
 * @author Aaron Kamienski
 */
class HDConfigInfo implements NativeHandle
{
    /**
     * Native configuration handle.
     */
    private int nConfig;

    /**
     * Flicker filter supported if true. To be initialized by
     * {@link #nInitConfigInfo}.
     */
    boolean flickerFilter;

    /**
     * Interlaced display supported if true. To be initialized by
     * {@link #nInitConfigInfo}.
     */
    boolean interlacedDisplay;

    /**
     * Pixel resolution. To be initialized by {@link #nInitConfigInfo}.
     */
    Dimension pixelResolution = new Dimension();

    /**
     * Pixel aspect ratio. To be initialized by {@link #nInitConfigInfo}.
     */
    Dimension pixelAspectRatio = new Dimension();

    /**
     * Normalized screen location. To be initialized by {@link #nInitConfigInfo}
     * .
     */
    HScreenRectangle screenArea = new HScreenRectangle(0.0F, 0.0F, 0.0F, 0.0F);

    /**
     * Still images supported if true. Only applies to background
     * configurations. To be initialized by {@link #nInitConfigInfo}.
     */
    boolean stillImage;

    /**
     * Changeable single color supported if true. Only applies to background
     * configurations. To be initialized by {@link #nInitConfigInfo}.
     */
    boolean changeableSingleColor;

    /**
     * Aspect Ratio for this configuration. Pixel resolution doesn't tell us the
     * info we need since you can have 640x480 configurations for 16:9 screens.
     * 
     * To be initialized by {@link #nInitConfigInfo}.
     */
    Dimension screenAspectRatio = new Dimension();

    /**
     * Creates a new <code>HDConfigInfo</code> based upon the given native
     * device configuration handle.
     */
    HDConfigInfo(int nConfig)
    {
        this.nConfig = nConfig;
        if (!nInitConfigInfo(nConfig)) throw new RuntimeException("Problems accessing configuration information");
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return nConfig;
    }

    /**
     * Initializes the contents of this <code>HDConfigInfo</code> using
     * information discovered at the native level about the native device
     * configuration handle.
     * 
     * @param nConfig
     *            the native configuration handle
     * @return <code>true</code> for success; <code>false</code> for failure
     */
    protected native boolean nInitConfigInfo(int nConfig);

    /**
     * Initializes the native interface.
     */
    private static native void nInit();

    /**
     * Initializes the native interface.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
