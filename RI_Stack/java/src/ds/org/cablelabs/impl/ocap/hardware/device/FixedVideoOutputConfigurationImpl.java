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

package org.cablelabs.impl.ocap.hardware.device;

import java.awt.Dimension;
import java.io.Serializable;

import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.VideoResolution;

/**
 * @author Alan Cossitt
 */
public class FixedVideoOutputConfigurationImpl implements FixedVideoOutputConfiguration, Serializable
{
    private transient VideoResolution videoResolution = null; // VideoResolution
                                                              // is not
                                                              // Serializable.
                                                              // Grr

    private transient boolean resolutionNull = true;

    private String name = null;

    private boolean enabled = false;

    // TODO, TODO_DS: workaround for OCORI-345
    // private transient Dimension rez = null;
    private int rezHeight = 0;

    private int rezWidth = 0;

    private int ar;

    private float rate = 0.0f;

    private int scan;

    private int stereoscopicMode;

    FixedVideoOutputConfigurationImpl(boolean enabled, String name, VideoResolution res)
    {
        if (name == null) throw new IllegalArgumentException("String is null");

        this.name = name;
        this.enabled = enabled;

        if (res != null)
        {
            this.rezHeight = res.getPixelResolution().height;
            this.rezWidth = res.getPixelResolution().width;

            this.ar = res.getAspectRatio();
            this.rate = res.getRate();
            this.scan = res.getScanMode();
            this.stereoscopicMode = res.getStereoscopicMode();

            resolutionNull = false;
        }
        else
        {
            resolutionNull = true;
        }
    }

    public VideoResolution getVideoResolution()
    {
        if (videoResolution == null && !resolutionNull)
        {
            Dimension dim = new Dimension(rezWidth, rezHeight); // work around
                                                                // for OCORI-345
            videoResolution = new VideoResolution(dim, ar, rate, scan, stereoscopicMode); // work
                                                                        // around
                                                                        // for
                                                                        // non
                                                                        // Serializable
                                                                        // VideoResolution
        }
        return videoResolution;
    }

    public String getName()
    {
        return name;
    }

    boolean isEnabled()
    {
        return enabled;
    }

    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
