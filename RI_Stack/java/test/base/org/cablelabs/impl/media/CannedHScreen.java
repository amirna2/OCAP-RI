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

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HVideoConfigTemplate;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;

/**
 * CannedHScreen
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedHScreen extends HScreen
{

    private static CannedHScreen instance = new CannedHScreen();

    private HVideoDevice[] vds;

    private HGraphicsDevice[] gds;

    private HBackgroundDevice[] bds;

    /**
     * 
     */
    private CannedHScreen()
    {
        vds = new HVideoDevice[2];
        vds[0] = new CannedVideoDevice(1);
        vds[1] = new CannedVideoDevice(2);

        gds = new HGraphicsDevice[1];
        gds[0] = new CannedGraphicsDevice(1);

        bds = new HBackgroundDevice[1];
        bds[0] = new CannedBackgroundDevice(1);
    }

    public static CannedHScreen getInstance()
    {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HScreen#getBestConfiguration(org.havi.ui.
     * HBackgroundConfigTemplate[])
     */
    public HBackgroundConfiguration getBestConfiguration(HBackgroundConfigTemplate[] hbcta)
    {
        return bds[0].getBestConfiguration(hbcta);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.havi.ui.HScreen#getBestConfiguration(org.havi.ui.HGraphicsConfigTemplate
     * [])
     */
    public HGraphicsConfiguration getBestConfiguration(HGraphicsConfigTemplate[] hgcta)
    {
        return gds[0].getBestConfiguration(hgcta);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.havi.ui.HScreen#getBestConfiguration(org.havi.ui.HVideoConfigTemplate
     * [])
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate[] hvcta)
    {
        return vds[0].getBestConfiguration(hvcta);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HScreen#getCoherentScreenConfigurations(org.havi.ui.
     * HScreenConfigTemplate[])
     */
    public HScreenConfiguration[] getCoherentScreenConfigurations(HScreenConfigTemplate[] hscta)
    {
        // TODO (Josh) Implement
        return super.getCoherentScreenConfigurations(hscta);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getDefaultHBackgroundDevice()
     */
    public HBackgroundDevice getDefaultHBackgroundDevice()
    {
        return bds[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getDefaultHGraphicsDevice()
     */
    public HGraphicsDevice getDefaultHGraphicsDevice()
    {
        return gds[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getDefaultHVideoDevice()
     */
    public HVideoDevice getDefaultHVideoDevice()
    {
        return vds[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getHBackgroundDevices()
     */
    public HBackgroundDevice[] getHBackgroundDevices()
    {
        return bds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getHGraphicsDevices()
     */
    public HGraphicsDevice[] getHGraphicsDevices()
    {
        return gds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HScreen#getHVideoDevices()
     */
    public HVideoDevice[] getHVideoDevices()
    {
        return vds;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HScreen#setCoherentScreenConfigurations(org.havi.ui.
     * HScreenConfiguration[])
     */
    public boolean setCoherentScreenConfigurations(HScreenConfiguration[] hsca) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        // TODO (Josh) Implement
        return super.setCoherentScreenConfigurations(hsca);
    }
}
