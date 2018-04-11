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

import java.util.Enumeration;

import org.havi.ui.HScreen;
import org.ocap.hardware.Host;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.FeatureNotSupportedException;
import org.ocap.hardware.device.HostSettings;
import org.ocap.system.MonitorAppPermission;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.host.DeviceSettingsHostManagerImpl;
import org.cablelabs.impl.ocap.hardware.HostImpl;
import org.cablelabs.impl.ocap.hardware.HostPersistence;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * 
 * @author Alan Cossitt
 */
public class DeviceSettingsHostImpl extends HostImpl implements HostSettings, EDListener
{
    private static final Logger log = Logger.getLogger(DeviceSettingsHostImpl.class.getName());
	
	private HostSettingsProxy proxy = null;

    private static volatile DeviceSettingsHostManagerImpl hostMgr = null;

    public DeviceSettingsHostImpl()
    {
        if (hostMgr == null)
        {
            Manager m = ManagerManager.getInstance(HostManager.class);
            hostMgr = (DeviceSettingsHostManagerImpl) m;
        }

        if (DeviceSettingsHostManagerImpl.isDeviceSettingsUsed() == true)
        {
            // persistence is setup by base class
            proxy = new HostSettingsProxy(); // will call persistence to
                                             // initialize itself
            getPersistence().initHostSettings(this); // initialize this object
        }

        // don't start listening until everything else is ready.
        hostMgr.addDisplayListener(this);

    }

    public DeviceSettingsHostImpl(DeviceSettingsHostManagerImpl l)
    {
        if (hostMgr == null)
        {
            hostMgr = l;
        }

        if (DeviceSettingsHostManagerImpl.isDeviceSettingsUsed() == true)
        {
            // persistence is setup by base class
            proxy = new HostSettingsProxy(); // will call persistence to
                                             // initialize itself
            getPersistence().initHostSettings(this); // initialize this object
        }

        // don't start listening until everything else is ready.
        hostMgr.addDisplayListener(this);

    }

    void initFromPersistence()
    {
        int power = getPersistence().getPowerMode();

        setPowerModeNoPerm(power);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.Host#getVideoOutputPorts()
     */
    public Enumeration getVideoOutputPorts()
    {
        return DeviceSettingsVideoOutputPortImpl.getVideoOutputPorts();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.device.HostSettings#getAudioOutputs()
     */
    public Enumeration getAudioOutputs()
    {
        return proxy.getAudioOutputs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.hardware.device.HostSettings#getMainVideoOutputPort(org.havi
     * .ui.HScreen)
     */
    public VideoOutputPort getMainVideoOutputPort(HScreen screen)
    {
        return proxy.getMainVideoOutputPort(screen);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.device.HostSettings#resetAllDefaults()
     */
    public void resetAllDefaults() throws SecurityException
    {
        proxy.resetAllDefaults(); // must be called first.

        DeviceSettingsHostPersistence persistence = getPersistence();

        persistence.initHostSettings(this);
    }

    private DeviceSettingsHostPersistence getPersistence()
    {
        return (DeviceSettingsHostPersistence) getHostPersistence();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.hardware.device.HostSettings#setMainVideoOutputPort(org.havi
     * .ui.HScreen, org.ocap.hardware.VideoOutputPort)
     */
    public void setMainVideoOutputPort(HScreen screen, VideoOutputPort port) throws FeatureNotSupportedException
    {
        proxy.setMainVideoOutputPort(screen, port);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.device.HostSettings#setPowerMode(int)
     */
    public void setPowerMode(int mode)
    {
        // breaks the pattern slightly since the proxy should not be called
        // since the code (except for permissions)
        // is handled in base class.
        super.setPowerMode(mode);

        getPersistence().persistPowerMode(mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.hardware.device.HostSettings#setSystemMuteKeyControl(boolean)
     */
    public void setSystemMuteKeyControl(boolean enable)
    {
        proxy.setSystemMuteKeyControl(enable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.hardware.device.HostSettings#setSystemVolumeKeyControl(boolean)
     */
    public void setSystemVolumeKeyControl(boolean enable)
    {
        proxy.setSystemVolumeKeyControl(enable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.device.HostSettings#setSystemVolumeRange(int)
     */
    public void setSystemVolumeRange(int range)
    {
        proxy.setSystemVolumeRange(range);
    }

    /**
     * 
     * ED listener notification. ED is the mechanism used to get unsolicited
     * events from the MPE layer. In this case this is invoked when a display
     * device is connected or disconnected. This is called from the
     * DeviceSettingsHostManagerImpl which handles the actual MPE/MPEOS async
     * connection.
     * 
     * @see DeviceSettingsHostManagerImpl
     * 
     * @param eventCode
     * @param eventData1
     * @param eventData2
     */

    public void asyncEvent(int eventCode, int videoPortHandle, int eventCounter)
    {
        if (eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_DISCONNECTED)
        {
            if (log.isDebugEnabled())
        	{
                log.debug("Display disconnected event received");
        	}
            proxy.handlePortDisconnected(videoPortHandle, eventCounter);
        }
        else if (eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_CONNECTED
                || eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_RESOLUTION)
        {
            if (log.isDebugEnabled())
        	{
                log.debug("Display connected or display resolution changed event received");
        	}
            proxy.handlePortConnected(videoPortHandle, eventCounter);
        }
    }

    protected HostPersistence createNewHostPersistence()
    {
        return new DeviceSettingsHostPersistence();
    }
}
