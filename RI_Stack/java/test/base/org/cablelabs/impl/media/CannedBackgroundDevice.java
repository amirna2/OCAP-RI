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

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;

/**
 * CannedBackgroundDevice
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedBackgroundDevice extends HBackgroundDevice
{

    private int handle;

    private HBackgroundConfiguration config;

    /**
     * 
     */
    public CannedBackgroundDevice(int handle)
    {
        this.handle = handle;
        config = new HBackgroundConfigurationHelper(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HBackgroundDevice#getBestConfiguration(org.havi.ui.
     * HBackgroundConfigTemplate)
     */
    public HBackgroundConfiguration getBestConfiguration(HBackgroundConfigTemplate hbc)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HBackgroundDevice#getBestConfiguration(org.havi.ui.
     * HBackgroundConfigTemplate[])
     */
    public HBackgroundConfiguration getBestConfiguration(HBackgroundConfigTemplate[] hbcta)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HBackgroundDevice#getConfigurations()
     */
    public HBackgroundConfiguration[] getConfigurations()
    {
        return new HBackgroundConfiguration[] { config };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HBackgroundDevice#getCurrentConfiguration()
     */
    public HBackgroundConfiguration getCurrentConfiguration()
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HBackgroundDevice#getDefaultConfiguration()
     */
    public HBackgroundConfiguration getDefaultConfiguration()
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.havi.ui.HBackgroundDevice#setBackgroundConfiguration(org.havi.ui.
     * HBackgroundConfiguration)
     */
    public boolean setBackgroundConfiguration(HBackgroundConfiguration hbc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        if (hbc instanceof HBackgroundConfigurationHelper)
        {
            config = hbc;
            return true;
        }
        else
            return false;
    }

    public static class HBackgroundConfigurationHelper extends HBackgroundConfiguration
    {

        private HBackgroundDevice bd;

        private Color color;

        public HBackgroundConfigurationHelper(HBackgroundDevice bd)
        {
            this.bd = bd;
            color = Color.black;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HBackgroundConfiguration#getColor()
         */
        public Color getColor()
        {
            return color;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HBackgroundConfiguration#getConfigTemplate()
         */
        public HBackgroundConfigTemplate getConfigTemplate()
        {
            // TODO (Josh) Implement
            return super.getConfigTemplate();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HBackgroundConfiguration#getDevice()
         */
        public HBackgroundDevice getDevice()
        {
            return bd;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HBackgroundConfiguration#setColor(java.awt.Color)
         */
        public void setColor(Color color) throws HPermissionDeniedException, HConfigurationException
        {
            this.color = color;
        }

    }
}
