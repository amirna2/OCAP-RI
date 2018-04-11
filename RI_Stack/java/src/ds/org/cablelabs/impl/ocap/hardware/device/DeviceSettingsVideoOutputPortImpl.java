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
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.FeatureNotSupportedException;
import org.ocap.hardware.device.VideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputPortListener;
import org.ocap.hardware.device.VideoOutputSettings;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.host.DeviceSettingsHostManagerImpl;
import org.cablelabs.impl.ocap.hardware.VideoOutputPortImpl;
import org.cablelabs.impl.util.NativeHandle;

/**
 * 
 * @author Alan Cossitt
 */
public class DeviceSettingsVideoOutputPortImpl extends VideoOutputPortImpl implements NativeHandle,
        VideoOutputSettings, Persistable, EDListener
{
    private static final Logger log = Logger.getLogger(DeviceSettingsVideoOutputPortImpl.class.getName());

    private static VideoOutputPort[] ports;

    private static AudioOutputPort[] audioPorts;

    private AudioOutputPort audioPort = null;

    /*
     * this code is using the add/remove listener template found in
     * StorageManagerImpl.java
     */

    /**
     * An object private to this storage manager object. This object is used for
     * synchronizing access to the ccList and as a key for caller context data
     * (CCData).
     */
    private Object synchCCData = new Object();

    /**
     * Multicast list of caller context objects for tracking listeners for this
     * video output port. At any point in time this list will be the complete
     * list of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    private volatile VideoOutputSettingsProxy proxy = null;

    /**
     * Creates a new instance of <code>VideoOutputPortImpl</code> based upon the
     * given native handle.
     * 
     * @param handle
     *            native handle
     */
    protected DeviceSettingsVideoOutputPortImpl(int videoHandle)
    {
        super(videoHandle);

        if (DeviceSettingsHostManagerImpl.isDeviceSettingsUsed() == true)
        {
            audioPort = getNewAudioOutputPort(videoHandle, this);
            proxy = new VideoOutputSettingsProxy(this);
            // everything now setup so start listening
            getHostManager().addDisplayListener(this);
        }
    }

    private static DeviceSettingsHostManagerImpl getHostManager()
    {
        return (DeviceSettingsHostManagerImpl) DeviceSettingsHostManagerImpl.getInstance();
    }

    private static AudioOutputPortImpl getNewAudioOutputPort(int videoHandle, VideoOutputPort connectedPort)
    {
        return new AudioOutputPortImpl(videoHandle, connectedPort);
    }

    /**
     * Provides an implementation for
     * {@link org.ocap.hardware.Host#getVideoOutputPorts}.
     * 
     * @return an <code>Enumeration</code> of the supported
     *         <code>VideoOutputPorts</code>
     */
    public static Enumeration getVideoOutputPorts()
    {
        return new ArrayEnum(getPorts());
    }

    /**
     * Retrieves the array of known <code>VideoOutputPort</code>s. If the ports
     * have not yet been discovered and the <i>ports</i> array initialized, this
     * is performed first.
     * 
     * @return the array of known <code>VideoOutputPort</code>s.
     */
    private static synchronized VideoOutputPort[] getPorts()
    {
        if (ports == null)
        {
            int[] handles = nGetVideoOutputPorts();

            ports = new VideoOutputPort[handles.length];
            for (int i = 0; i < handles.length; ++i)
            {
                ports[i] = new DeviceSettingsVideoOutputPortImpl(handles[i]);
                
                // SPS Add some logging
                if (ports[i] instanceof DeviceSettingsVideoOutputPortImpl)
                { 
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-vop-s)----- DS_VOP_IMPL: Created VOP["+i+"] ----------");
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-vop-s)  VOP_IMPL");
                    }
                	((VideoOutputPortImpl)ports[i]).dump("+(ds-vop-s)    ");
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-vop-s)  VOPS_PROXY");
                    }
                	((DeviceSettingsVideoOutputPortImpl)ports[i]).proxy.dump("+(ds-vop-s)    "); 
                }                
            }
        }
        return ports;
    }

    static synchronized DeviceSettingsVideoOutputPortImpl findPort(String uniqueId)
    {
        if (log.isDebugEnabled())
        {
            log.debug("DeviceSettingsVideoOutputPortImpl.findPort desiredUniqueId: " + uniqueId);
        }
        if (uniqueId == null)
        {
            return null;
        }

        DeviceSettingsVideoOutputPortImpl p = null;

        VideoOutputPort[] ports = getPorts();
        for (int i = 0; i < ports.length; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DeviceSettingsVideoOutputPortImpl.findPort: " + ports[i]);
            }

            p = (DeviceSettingsVideoOutputPortImpl) ports[i];
            if (log.isDebugEnabled())
            {
                log.debug("DeviceSettingsVideoOutputPortImpl.findPort uniqueId: " + p.getUniqueId());
            }
            if (p.getUniqueId().equals(uniqueId))
            {
                return p;
            }
        }

        return null;
    }

    static synchronized VideoOutputConfiguration findConfig(String uniqueId)
    {
        VideoOutputPort[] ports = getPorts();
        for (int i = 0; i < ports.length; i++)
        {
            DeviceSettingsVideoOutputPortImpl port = (DeviceSettingsVideoOutputPortImpl) ports[i];
            VideoOutputConfiguration[] configs = port.getSupportedConfigurations();

            for (int j = 0; j < configs.length; j++)
            {
                VideoOutputConfiguration config = configs[j];
                if (config.getName().equals(uniqueId))
                {
                    return config;
                }
            }
        }
        return null;
    }

    private static synchronized AudioOutputPort[] getAudioPorts()
    {
        if (audioPorts == null)
        {
            getPorts();
            audioPorts = new AudioOutputPort[ports.length];
            for (int i = 0; i < ports.length; ++i)
            {
                audioPorts[i] = ((DeviceSettingsVideoOutputPortImpl) ports[i]).getAudioOutputPort();
            }
        }
        return audioPorts;
    }

    public static Enumeration getAudioOutputPorts()
    {
        return new ArrayEnum(getAudioPorts());
    }

    public void addListener(VideoOutputPortListener listener)
    {
        // Add the listener to the list of listeners for this caller context.
        if (listener == null) throw new IllegalArgumentException();

        synchronized (synchCCData)
        {
            // Get the data for the current caller context.
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CCData data = getCCData(ccm.getCurrentContext());

            // ensure that listener already isn't in the list
            if (data.listeners.contains(listener))
            {
                return;
            }
            // Add caller's listener to list of listeners.
            data.listeners.add(listener);
        }

    }

    public AudioOutputPort getAudioOutputPort()
    {
        return audioPort;
    }

    public int getAspectRatio()
    {
        return proxy.getAspectRatio();
    }

    public int getDisplayAspectRatio()
    {
        return proxy.getDisplayAspectRatio();
    }

    public Hashtable getDisplayAttributes()
    {
        return proxy.getDisplayAttributes();
    }

    public VideoOutputConfiguration getOutputConfiguration()
    {
        return proxy.getOutputConfiguration();
    }

    public VideoOutputConfiguration[] getSupportedConfigurations()
    {
        return proxy.getSupportedConfigurations();
    }

    public boolean isContentProtected()
    {
        return proxy.isContentProtected();
    }

    public boolean isDisplayConnected()
    {
        return proxy.isDisplayConnected();
    }

    public boolean isDynamicConfigurationSupported()
    {
        return proxy.isDynamicConfigurationSupported();
    }

    public void removeListener(VideoOutputPortListener listener)
    {
        if (listener == null) throw new IllegalArgumentException();

        // Remove the listener from the list of listeners for this caller
        // context.
        synchronized (synchCCData)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CCData data = getCCData(ccm.getCurrentContext());

            // Remove the caller from the list of listeners.
            data.listeners.remove(listener);
        }
    }

    public void setOutputConfiguration(VideoOutputConfiguration config) throws FeatureNotSupportedException
    {
        VideoOutputConfiguration oldConfig = this.getOutputConfiguration();
        proxy.setOutputConfiguration(config);
        refreshPortInfo();
        notifyConfigurationChanged(this, oldConfig, config);
    }

    public static void clearCaches()
    {
        for (int i = 0; i < ports.length; i++)
        {
            getHostManager().removeDisplayListener(((DeviceSettingsVideoOutputPortImpl) ports[i]));
        }
        ports = null;
        audioPorts = null;
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private CCData getCCData(CallerContext cc)
    {
        synchronized (synchCCData)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(synchCCData);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, synchCCData);
                ccList = CallerContext.Multicaster.add(ccList, cc);
            }
            return data;
        }
    }

    /**
     * Per caller context data
     */
    class CCData implements CallbackData
    {
        /**
         * The listeners is used to keep track of all objects that have
         * registered to be notified of video output port events.
         * 
         * VideoOutputPortListener
         */
        public volatile Vector listeners = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (synchCCData)
            {
                // Remove this caller context from the list then throw away
                // the CCData for it.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(synchCCData);
                listeners = null;
            }
        }
    }

    static final int ENABLE_STATUS_CHANGE = 0;

    static final int CONNECTION_STATUS_CHANGE = 1;

    void notifyConnectionChanged(final int type, final VideoOutputPort source, final boolean status)
    {
        final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        if (ccList != null)
        {
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    synchronized (synchCCData)
                    {
                        Vector listeners;
                        if ((listeners = getCCData(ccm.getCurrentContext()).listeners) != null)
                        {
                            // Invoke listeners
                            for (int i = 0; i < listeners.size(); i++)
                            {
                                VideoOutputPortListener listener = (VideoOutputPortListener) listeners.elementAt(i);

                                if (type == ENABLE_STATUS_CHANGE)
                                {
                                    listener.enabledStatusChanged(source, status);
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("enabledStatusChanged called");
                                    }
                                }
                                else if (type == CONNECTION_STATUS_CHANGE)
                                {
                                    listener.connectionStatusChanged(source, status);
                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("connectionStatusChange called");
                                    }
                                }
                                else
                                {
                                    if (log.isErrorEnabled())
                                    {
                                        log.error("DeviceSettingsVideoOuputportImpl.notifyConnectionChanged(): unkown change type: "+type);
                                    }
                                    throw new IllegalArgumentException("Invalid connection change type");
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    void notifyConfigurationChanged(final VideoOutputPort source, final VideoOutputConfiguration oldConfig,
            final VideoOutputConfiguration newConfig)
    {
        final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (ccList != null)
        {
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    synchronized (synchCCData)
                    {
                        Vector listeners;
                        if ((listeners = getCCData(ccm.getCurrentContext()).listeners) != null)
                        {
                            // Invoke listeners
                            for (int i = 0; i < listeners.size(); i++)
                            {
                                VideoOutputPortListener listener = (VideoOutputPortListener) listeners.elementAt(i);
                                listener.configurationChanged(source, oldConfig, newConfig);
                            }
                        }
                    }
                }
            });
        }
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
     * @param videoPortHandle
     *            native handle of video port which has been connected or
     *            disconnected.
     * @param eventData2
     */
    public void asyncEvent(int eventCode, int videoPortHandle, int eventCounter)
    {
        if (eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_CONNECTED
                || eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_DISCONNECTED)
        {
            if (videoPortHandle == getHandle())
            {
                proxy.handleRefresh(eventCounter);
            }
            notifyConnectionChanged(CONNECTION_STATUS_CHANGE, this, eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_CONNECTED);
        }
        else if (eventCode == DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_RESOLUTION)
        {
            if (videoPortHandle == getHandle())
            {
                proxy.handleResolutionChange(eventCounter);
            }
        }
    }

    public void waitForRefresh(int eventCounter)
    {
        proxy.waitForRefresh(eventCounter);

    }

    public String getUniqueId()
    {
        return proxy.getUniqueId();
    }

    // inform listeners of changes...
    public void enable() throws IllegalStateException, SecurityException
    {
        super.enable();
        this.notifyConnectionChanged(ENABLE_STATUS_CHANGE, this, true);
    }

    public void disable() throws IllegalStateException, SecurityException
    {
        super.disable();
        this.notifyConnectionChanged(ENABLE_STATUS_CHANGE, this, false);
    }

}
