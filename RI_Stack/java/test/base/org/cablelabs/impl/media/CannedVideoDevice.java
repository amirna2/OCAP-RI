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

import java.awt.Dimension;

import org.davic.resources.ResourceClient;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfigTemplate;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.havi.ui.event.HScreenConfigurationListener;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.player.AVPlayer;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;

/**
 * CannedVideoDevice
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedVideoDevice extends HVideoDevice implements VideoDevice
{

    protected AVPlayer player;

    protected HVideoConfiguration config;

    protected HVideoConfiguration defaultConfig;

    protected int currentHandle;

    boolean controlVideoDevice = true;

    public CannedVideoDevice(int handle)
    {
        player = null;
        currentHandle = handle;

        defaultConfig = new HVideoConfigurationHelper(this, new Dimension(1, 1), new Dimension(640, 480));
        config = defaultConfig;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.VideoDevice#getCurrentConfiguration()
     */
    public HVideoConfiguration getCurrentConfiguration()
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.util.NativeHandle#getHandle()
     */
    public int getHandle()
    {
        return currentHandle;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HVideoDevice#getBestConfiguration(org.havi.ui.
     * HVideoConfigTemplate)
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate hvct)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HVideoDevice#getBestConfiguration(org.havi.ui.
     * HVideoConfigTemplate[])
     */
    public HVideoConfiguration getBestConfiguration(HVideoConfigTemplate[] hvcta)
    {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HVideoDevice#getConfigurations()
     */
    public HVideoConfiguration[] getConfigurations()
    {
        return new HVideoConfiguration[] { config };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HVideoDevice#getDefaultConfiguration()
     */
    public HVideoConfiguration getDefaultConfiguration()
    {
        return defaultConfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.havi.ui.HVideoDevice#getMatchStrength(org.havi.ui.HScreenConfiguration
     * , org.havi.ui.HScreenConfigTemplate)
     */
    protected int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HVideoDevice#getVideoController()
     */
    public Object getVideoController() throws SecurityException, HPermissionDeniedException
    {
        // TODO (Josh) Implement
        return super.getVideoController();
    }

    public AVPlayer getController()
    {
        try
        {
            return (AVPlayer) getVideoController();
        }
        catch (Exception x)
        {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.havi.ui.HVideoDevice#getVideoSource()
     */
    public Object getVideoSource() throws SecurityException, HPermissionDeniedException
    {
        // TODO (Josh) Implement
        return super.getVideoSource();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HVideoDevice#setVideoConfiguration(org.havi.ui.
     * HVideoConfiguration)
     */
    public boolean setVideoConfiguration(HVideoConfiguration hvc) throws SecurityException, HPermissionDeniedException,
            HConfigurationException
    {
        config = hvc;
        return true;
    }

    public static class HVideoConfigurationHelper extends HVideoConfiguration
    {
        public HScreenRectangle hsr; // screen area

        public HVideoDevice vd; // video device

        public Dimension pr; // pixel resolution

        public Dimension par; // pixel aspect ratio

        public HVideoConfigurationHelper(HVideoDevice vd, Dimension par, Dimension pr)
        {
            this(vd, par, pr, new HScreenRectangle(0.0f, 0.0f, 1.0f, 1.0f));
        }

        public HVideoConfigurationHelper(HVideoDevice vd, Dimension par, Dimension pr, HScreenRectangle sa)
        {
            this.vd = vd;
            this.pr = pr;
            this.par = par;
            this.hsr = sa;
        }

        public HVideoDevice getDevice()
        {
            return vd;
        }

        public Dimension getPixelAspectRatio()
        {
            return new Dimension(par);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getPixelResolution()
         */
        public Dimension getPixelResolution()
        {
            return new Dimension(pr);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.havi.ui.HScreenConfiguration#getScreenArea()
         */
        public HScreenRectangle getScreenArea()
        {
            return new HScreenRectangle(hsr.x, hsr.y, hsr.width, hsr.height);
        }

    }

    public void cannedSetHandle(int handle)
    {
        currentHandle = handle;
    }

    public void setControlVideoDevice(boolean b)
    {
        controlVideoDevice = b;
    }

    public int controlVideoDevice(AVPlayer player, ResourceUsageImpl resourceUsage)
    {
        // TODO (Josh) Implement
        return VideoDevice.CONTROL_SUCCESS;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.havi.ui.HScreenDevice#releaseDevice(org.cablelabs.impl.manager.
     * CallerContext)
     */
    public void releaseDevice(CallerContext context)
    {
        // TODO (Josh) Implement
        super.releaseDevice(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.havi.ui.HScreenDevice#reserveDevice(org.cablelabs.impl.ocap.resource
     * .ResourceUsageImpl, org.davic.resources.ResourceClient)
     */
    protected ResourceClient currentClient;

    public ResourceClient getCurrentClient()
    {
        return currentClient;
    }

    public boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context)
    {
        // TODO (Josh) Implement
        currentClient = client;
        return true;
        // return super.reserveDevice(usage, client, context);
    }

    //
    // expose for testing
    // 
    public void cannedNotifyResourceReleased()
    {
        notifyResourceReleased();
    }

    //
    // expose for testing
    //
    public void cannedNotifyResourceReserved()
    {
        notifyResourceReserved();
    }

    public void swapControllers(AVPlayer otherPlayer)
    {
    }

    public void relinquishVideoDevice(AVPlayer player)
    {
    }

    private HScreenConfigurationListener lastScreenConfigListener;

    public void addScreenConfigurationListener(HScreenConfigurationListener hscl)
    {
        super.addScreenConfigurationListener(hscl);
    }

    public HScreenConfigurationListener getLastScreenConfigListener()
    {
        return lastScreenConfigListener;
    }

    public int reserveAndControlDevice(AVPlayer player, ResourceUsageImpl usage, ResourceClient resClient)
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
