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
package org.cablelabs.impl.media;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import org.havi.ui.HConfigurationException;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HImageHints;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoDevice;

/**
 * CannedGraphicsDevice
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedGraphicsDevice extends HGraphicsDevice
{

    private int handle;

    private HGraphicsConfiguration config;

    /**
     * 
     */
    CannedGraphicsDevice(int handle)
    {
        this.handle = handle;
        config = new HGraphicsConfigurationHelper(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HGraphicsDevice#getBestConfiguration(org.havi.ui.
     * HGraphicsConfigTemplate)
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate hgct)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HGraphicsDevice#getBestConfiguration(org.havi.ui.
     * HGraphicsConfigTemplate[])
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate[] hgcta)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HGraphicsDevice#getConfigurations()
     */
    public HGraphicsConfiguration[] getConfigurations()
    {
        return new HGraphicsConfiguration[] { config };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HGraphicsDevice#getCurrentConfiguration()
     */
    public HGraphicsConfiguration getCurrentConfiguration()
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HGraphicsDevice#getDefaultConfiguration()
     */
    public HGraphicsConfiguration getDefaultConfiguration()
    {
        return new HGraphicsConfigurationHelper(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HGraphicsDevice#setGraphicsConfiguration(org.havi.ui.
     * HGraphicsConfiguration)
     */
    public boolean setGraphicsConfiguration(HGraphicsConfiguration hgc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        if (hgc instanceof HGraphicsConfigurationHelper)
        {
            config = hgc;
            return true;
        }
        else
            return false;
    }

    public static class HGraphicsConfigurationHelper extends HGraphicsConfiguration
    {
        private HGraphicsDevice gd; // graphics device

        public HScreenRectangle sa; // screen area

        public Dimension pr; // pixel resolution

        public Dimension par; // pixel aspect ratio

        public HGraphicsConfigurationHelper(HGraphicsDevice gd)
        {
            this(gd, new Dimension(1, 1), new Dimension(640, 480), new HScreenRectangle(0, 0, 1, 1));
            this.gd = gd;
        }

        public HGraphicsConfigurationHelper(HGraphicsDevice gd, Dimension par, Dimension pr, HScreenRectangle sa)
        {
            this.gd = gd;
            this.par = par;
            this.pr = pr;
            this.sa = sa;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HGraphicsConfiguration#dispose(java.awt.Color)
         */
        public void dispose(Color c)
        {
            // TODO (Josh) Implement
            super.dispose(c);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HGraphicsConfiguration#getAllFonts()
         */
        public Font[] getAllFonts()
        {
            // TODO (Josh) Implement
            return super.getAllFonts();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getCompatibleImage(java.awt.Image,
         * org.havi.ui.HImageHints)
         */
        public Image getCompatibleImage(Image input, HImageHints ih)
        {
            // TODO (Josh) Implement
            return super.getCompatibleImage(input, ih);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getComponentHScreenRectangle(java
         * .awt.Component)
         */
        public HScreenRectangle getComponentHScreenRectangle(Component component)
        {
            // TODO (Josh) Implement
            return super.getComponentHScreenRectangle(component);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HGraphicsConfiguration#getConfigTemplate()
         */
        public HGraphicsConfigTemplate getConfigTemplate()
        {
            // TODO (Josh) Implement
            return super.getConfigTemplate();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HGraphicsConfiguration#getDevice()
         */
        public HGraphicsDevice getDevice()
        {
            return gd;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getPixelCoordinatesHScreenRectangle
         * (org.havi.ui.HScreenRectangle, java.awt.Container)
         */
        public Rectangle getPixelCoordinatesHScreenRectangle(HScreenRectangle sr, Container cont)
        {
            // TODO (Josh) Implement
            return super.getPixelCoordinatesHScreenRectangle(sr, cont);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getPunchThroughToBackgroundColor
         * (java.awt.Color, int, org.havi.ui.HVideoDevice)
         */
        public Color getPunchThroughToBackgroundColor(Color color, int percentage, HVideoDevice v)
        {
            // TODO (Josh) Implement
            return super.getPunchThroughToBackgroundColor(color, percentage, v);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getPunchThroughToBackgroundColor
         * (java.awt.Color, int)
         */
        public Color getPunchThroughToBackgroundColor(Color color, int percentage)
        {
            // TODO (Josh) Implement
            return super.getPunchThroughToBackgroundColor(color, percentage);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getPunchThroughToBackgroundColor
         * (int, org.havi.ui.HVideoDevice)
         */
        public Color getPunchThroughToBackgroundColor(int percentage, HVideoDevice hvd)
        {
            // TODO (Josh) Implement
            return super.getPunchThroughToBackgroundColor(percentage, hvd);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.havi.ui.HGraphicsConfiguration#getPunchThroughToBackgroundColor
         * (int)
         */
        public Color getPunchThroughToBackgroundColor(int percentage)
        {
            // TODO (Josh) Implement
            return super.getPunchThroughToBackgroundColor(percentage);
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.havi.ui.HScreenConfiguration#convertTo(org.havi.ui.
         * HScreenConfiguration, java.awt.Point)
         */
        public Point convertTo(HScreenConfiguration destination, Point source)
        {
            // TODO (Josh) Implement
            return super.convertTo(destination, source);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getFlickerFilter()
         */
        public boolean getFlickerFilter()
        {
            // TODO (Josh) Implement
            return super.getFlickerFilter();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getInterlaced()
         */
        public boolean getInterlaced()
        {
            // TODO (Josh) Implement
            return super.getInterlaced();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.havi.ui.HScreenConfiguration#getOffset(org.havi.ui.
         * HScreenConfiguration)
         */
        public Dimension getOffset(HScreenConfiguration hsc)
        {
            // TODO (Josh) Implement
            return super.getOffset(hsc);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getPixelAspectRatio()
         */
        public Dimension getPixelAspectRatio()
        {
            return par;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getPixelResolution()
         */
        public Dimension getPixelResolution()
        {
            return pr;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getScreenArea()
         */
        public HScreenRectangle getScreenArea()
        {
            return sa;
        }

    }
}
