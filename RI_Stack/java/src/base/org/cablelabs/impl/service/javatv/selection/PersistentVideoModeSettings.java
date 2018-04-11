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

package org.cablelabs.impl.service.javatv.selection;

import java.awt.Container;
import java.awt.Rectangle;

import org.dvb.media.VideoTransformation;

/**
 * PersistentVideoModeSettings is a simple implementation convenience class used
 * as a container for the persistent video settings associated with a
 * ServiceContext.
 */
public class PersistentVideoModeSettings
{
    /*
     * Constructor:
     */
    PersistentVideoModeSettings()
    {
    }

    /**
     * Get the parent container associated with component video settings.
     */
    Container getComponentParent()
    {
        return this.parent;
    }

    /**
     * Get the boundary rectangle associated with component video settings.
     */
    Rectangle getComponentRectangle()
    {
        return this.rectangle;
    }

    /**
     * Get the video transformation associated with background video settings.
     */
    VideoTransformation getVideoTransformation()
    {
        return this.transformation;
    }

    /**
     * Get persistent video mode enabled setting.
     */
    boolean isEnabled()
    {
        return this.enabled;
    }

    /**
     * Get applications enabled setting.
     */
    boolean isAppsEnabled()
    {
        return this.appsEnabled;
    }

    /**
     * Set the parent container associated with component video settings.
     */
    void setComponent(Container parent, Rectangle rect)
    {
        this.parent = parent;
        this.rectangle = rect;
    }

    /**
     * Set the video transformation associated with background video settings.
     */
    void setVideoTransformation(VideoTransformation trans)
    {
        this.transformation = trans;
    }

    /**
     * Set persistent video mode enabled setting.
     */
    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Set applications enabled setting.
     */
    void setAppsEnabled(boolean appsEnabled)
    {
        this.appsEnabled = appsEnabled;
    }

    /*
     * Variables:
     */
    Container parent = null;

    Rectangle rectangle = null;

    VideoTransformation transformation = null;

    boolean enabled = false;

    // by default, apps are enabled
    boolean appsEnabled = true;
}
