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

package org.cablelabs.impl.manager.host;

import org.ocap.hardware.VideoOutputPort;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import org.ocap.hardware.Host;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.VideoZoomPreference;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hardware.device.AudioOutputPortImpl;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsVideoOutputPortImpl;
import org.cablelabs.impl.ocap.hardware.VideoOutputPortImpl;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.apache.log4j.Logger;

import org.ocap.hardware.PowerModeChangeListener;

/**
 * 
 * @author Alan Cossitt
 * 
 */
public class DeviceSettingsHostManagerImpl implements Manager, HostManager, EDListener,
        PowerModeChangeListener
{
    private static final Logger log = Logger.getLogger(DeviceSettingsHostManagerImpl.class.getName());

    public static final int MPE_DISP_EVENT_CONNECTED = 0x2500; // display
                                                               // connected to
                                                               // video port

    public static final int MPE_DISP_EVENT_DISCONNECTED = 0x2501; // display
                                                                  // disconnected
                                                                  // from video
                                                                  // port

    public static final int MPE_DISP_EVENT_RESOLUTION = 0x2502; // display
                                                                // resolution
                                                                // changed

    public static final int MPE_DISP_EVENT_SHUTDOWN = 0x2503; // shutting down
                                                              // this queue

    private static DeviceSettingsHostManagerImpl manager = null;

    private static native int nRegisterAsync(Object EdListenerObject);

    private static native void nUnregisterAsync(int EdListenerHandle);

    private Host host;

    private Object synch = null;

    private Vector listeners = null;
    
    private Hashtable excludeFromUnmute = null;
    private Hashtable excludeFromEnable = null;
    
    // Support for situations where device settings extension is disabled/not
    // implemented.
    private static final boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);

    public static boolean isDeviceSettingsUsed()
    {
        return dsExtUsed;
    }

    /**
     * ED handle for the registered async display listener
     */
    private static int edListenerHandle = 0;

    protected DeviceSettingsHostManagerImpl()
    {
        synch = new Object();
        listeners = new Vector();
        excludeFromUnmute = new Hashtable();
        excludeFromEnable = new Hashtable();
    }

    public synchronized static Manager getInstance()
    {
        if (manager == null)
        {
            // TODO_DS, TODO is this correct
            manager = new DeviceSettingsHostManagerImpl();
            edListenerHandle = nRegisterAsync(manager);
            if (log.isDebugEnabled())
            {
                log.debug("DeviceHostSettingsManager.getInstance: manager = " + manager + " edListnerHandle = "
                        + edListenerHandle + "/n");
            }
        }
        return manager;
    }

    public static void checkPermissions()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("deviceController"));
    }

    private static final int PWR_REQ_UNKOWN = -1;
    private static final int PWR_REQ_LOW    = -0;
    private static final int PWR_REQ_FULL   =  1;
    private int lastPowerRequest = PWR_REQ_UNKOWN;     
    private void vopEnable(int nPM)
    {    	
        synchronized (synch)
        {
        	//
        	// Transition to Low Power
        	//        	
        	if ( nPM == Host.LOW_POWER && lastPowerRequest != PWR_REQ_LOW )  
			{
	    		// Audio ports
	            excludeFromUnmute.clear();
	            Enumeration audioOutputs = ((DeviceSettingsHostImpl)host).getAudioOutputs();
	            while(audioOutputs.hasMoreElements())
	            {
	            	AudioOutputPort audioPort = (AudioOutputPort) audioOutputs.nextElement();
	
	            	// if we can get a unique ID and port is muted, add it to un-mute exclusion list
	                if ( audioPort.isMuted() && audioPort instanceof AudioOutputPortImpl )
	                {
	                	String id = ((AudioOutputPortImpl)audioPort).getUniqueId();
	                	excludeFromUnmute.put(id,id);
	                }
	                audioPort.setMuted(true);
	            }
	
	            // Video ports
	            excludeFromEnable.clear();
	            Enumeration vopEnum = Host.getInstance().getVideoOutputPorts();
	            while (vopEnum.hasMoreElements())
	            {
	                VideoOutputPort vop = (VideoOutputPort) vopEnum.nextElement();
	                DeviceSettingsVideoOutputPortImpl dsVOPImpl = (vop instanceof VideoOutputPortImpl) ?
	                											  (DeviceSettingsVideoOutputPortImpl)vop : null;
	                
	                String vopUniqueId = (dsVOPImpl == null) ? "?" : dsVOPImpl.getUniqueId();
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-vop-pwr-s)    DeviceSettingsHostManagerImpl::vopEnable - processing VOP " + vopUniqueId);
                    }
	
	                // if we can get a unique ID and port is disabled, add it to enable exclusion list
	                if ( !vop.status() && dsVOPImpl != null )
	                {
	                	Integer id = new Integer(dsVOPImpl.getHandle());
	                	excludeFromEnable.put(id, id);
                        if (log.isDebugEnabled())
                        {
                            log.debug("+(ds-vop-pwr-s)        " + vopUniqueId + " added to VOP enable exclusion list");
                        }
                    }
	                vop.disable();
	            }
	            lastPowerRequest = PWR_REQ_LOW; 
			}
	    	//
	    	// Transition to Full Power
	    	//	
	    	else if ( nPM == Host.FULL_POWER && lastPowerRequest != PWR_REQ_FULL ) 
			{
	    		boolean enable = true;
	    		
	    		// Audio Ports
	            Enumeration audioOutputs = ((DeviceSettingsHostImpl)host).getAudioOutputs();
	            while(audioOutputs.hasMoreElements())
	            {
	                AudioOutputPort audioPort = (AudioOutputPort) audioOutputs.nextElement();
	
	                // check for audio ports in the un-mute exclusion list
	                enable = true;
	                if ( audioPort instanceof AudioOutputPortImpl )
	                {
	                	String id = ((AudioOutputPortImpl)audioPort).getUniqueId();
	                	if ( excludeFromUnmute.get(id) != null )
	                		{ enable = false; }
	                }
	                if ( enable )
	                	{ audioPort.setMuted(false);}
			    }
	            excludeFromUnmute.clear();
	    	
	    		// Video Ports
	            Enumeration vopEnum = Host.getInstance().getVideoOutputPorts();
	            while (vopEnum.hasMoreElements())
	            {
	                VideoOutputPort vop = (VideoOutputPort) vopEnum.nextElement();
	                DeviceSettingsVideoOutputPortImpl dsVOPImpl = (vop instanceof VideoOutputPortImpl) ?
	                											  (DeviceSettingsVideoOutputPortImpl)vop : null;
	                
	                String vopUniqueId = (dsVOPImpl == null) ? "?" : dsVOPImpl.getUniqueId();
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-vop-pwr-s)    DeviceSettingsHostManagerImpl::vopEnable - processing VOP " + vopUniqueId);
                    }
	
	                // check for video ports in the exclusion list
	                enable = true;
	                if ( dsVOPImpl != null )
	                {
	                	Integer id = new Integer(dsVOPImpl.getHandle());
	                	if ( excludeFromEnable.get(id) != null )
	                	{ 
                            if (log.isDebugEnabled())
                            {
                                log.debug("+(ds-vop-pwr-s)        " + vopUniqueId + " in exclusion list, won't be enabled");
                            }
	                        enable = false; 
	                	}
	                }
	                if ( enable )
	                { 
                        if (log.isDebugEnabled())
                        {
                            log.debug("+(ds-vop-pwr-s)        enabling VOP " + vopUniqueId);
                        }
	                	vop.enable(); 
	                }
                    if (log.isDebugEnabled())
	                {
                        log.debug("+(ds-vop-pwr-s)    DeviceSettingHostManagerImpl:powerModeChanged:  VideoOutputPort == " + 
                        		                      vopUniqueId + "status == " + vop.status());
	                }
	
	            }            
	            excludeFromEnable.clear();
	            lastPowerRequest = PWR_REQ_FULL; 
	        }
        }
    }

    public void powerModeChanged(int newPowerMode)
    {
        if (log.isDebugEnabled())
        {
            log.debug("+(ds-vop-pwr-s) DeviceSettingsHostManagerImpl:  Notified of power mode change to "
                    + ((newPowerMode == Host.FULL_POWER) ? "Full" : "Low") + " power");
        }
        vopEnable(newPowerMode);
    }

    /*
     * defined for HostManager interface but should NOT be used by any object
     * other then Host !!!! Use Host.getInstance() !!!!
     * 
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.HostManager#getHostInstance()
     */

    public Host getHostInstance()
    {
        this.host = new DeviceSettingsHostImpl(this);

        this.host.addPowerModeChangeListener(this);
        if (log.isDebugEnabled())
        {
            log.debug("DeviceSettingHostManagerImpl:  addPowerModeChangeListener added");
        }

        return host;
    }

    public void destroy()
    {
        nUnregisterAsync(edListenerHandle);
        manager = null;
        edListenerHandle = 0;
    }

    /**
     * Add a display listener.
     * 
     * This class is a EDListener which receives asychronous events from the MPE
     * and MPEOS layers. For example, this class will receive an event when a
     * video port (which supports this feature) is connected or disconnected to
     * a display device. This class handles the connection to MPE/MPEOS and then
     * uses this method to add Java level listeners that receive these events.
     * 
     * Not to be used by an application!!!!
     * 
     * @param listener
     */
    public void addDisplayListener(EDListener listener)
    {
        synchronized (synch)
        {
            if (!listeners.contains(listener))
            {
                listeners.add(listener);
            }
        }
    }

    public void removeDisplayListener(EDListener listener)
    {
        synchronized (synch)
        {
            listeners.remove(listener);
        }
    }

    public void asyncEvent(int eventCode, int videoPortHandle, int counter)
    {
        Vector clone = null;
        synchronized (synch)
        {
            clone = (Vector) listeners.clone();
        }

        Enumeration e = clone.elements();
        while (e.hasMoreElements())
        {
            EDListener edl = (EDListener) e.nextElement();

            // eventData1 is the port handle (mpe_DispOutputPort)
            edl.asyncEvent(eventCode, videoPortHandle, counter);
        }
    }

    public int getZoomModePreference()
    {
        return VideoZoomPreference.ZOOM_MODE;
    }

    // Initialize JNI layer.
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);
    }

}
