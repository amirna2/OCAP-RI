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
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.DynamicVideoOutputConfiguration;
import org.ocap.hardware.device.FeatureNotSupportedException;
import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputPortListener;
import org.ocap.hardware.device.VideoOutputSettings;
import org.ocap.hardware.device.VideoResolution;
import org.ocap.media.VideoFormatControl;
import org.ocap.media.S3DFormatTypes;

import org.cablelabs.impl.havi.port.mpe.HDScreen;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.host.DeviceSettingsHostManagerImpl;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.util.SystemEventUtil;
import org.havi.ui.HScreen;

/**
 * 
 * @author Alan Cossitt
 */
public class VideoOutputSettingsProxy implements VideoOutputSettings, Persistable
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(VideoOutputSettingsProxy.class.getName());

    static final String AttrManNameKey = "Manufacturer Name";

    static final String AttrProdcodeKey = "Product Code";

    static final String AttrSerialNumKey = "Serial Number";

    static final String AttrManWeekKey = "Manufacture Week";

    static final String AttrManYearKey = "Manufacture Year";

    static final int UnknownAR = -1;

    private DeviceSettingsVideoOutputPortImpl parentVideoPort = null;

    /**
     * Unique ID of parent port, set by JNI
     * 
     * proxy gets the unique id of the parent (DeviceSettingsVideoOuputputPort)
     * this is purely convenience so we don't have another JNI file just for
     * that one thing. I know, ugly object separation and it bothers me but the
     * JNI development/testing overhead... TODO, TODO_DS: create JNI file for
     * DeviceSettingsVideoOutputPortImpl and move unique id of port to there.
     * 
     * SPS: this class and DeviceSettingsVideoOutputPortImpl are so tightly 
     *      interdependent at this point that the benefit of moving parentUniqueID 
     *      to DeviceSettingsVideoOutputPortImpl is not worth the work/risks 
     */
    private String parentUniqueId;

    /**
     * Display attributes set by jni
     */
    private String manufacturerName;

    private short productCode;

    private int serialNumber;

    private byte manufactureYear;

    private byte manufactureWeek;

    private Dimension aspectRatio = new Dimension();

    /**
     * Configurations
     */
    // map between config and config handle. Handle is needed for setting
    // current config.
    // These maps are set by JNI which uses callbacks into this class to do the
    // actual adding of
    // supported configs.
    Hashtable configToHandle = new Hashtable();

    Hashtable handleToConfig = new Hashtable();

    VideoOutputConfiguration currentConfig = null; // null if not supported

    private VideoOutputConfiguration[] configArray = null;

    /**
     * Misc fields set by JNI
     * 
     */
    private boolean isDisplayConnected = false;

    // not set by JNI
    // see constructor
    private boolean isDynamicConfigSupported = false;

    /**
     * An object private to this storage manager object. This object is used for
     * synchronizing access to the ccList and as a key for caller context data
     * (CCData).
     */
    private Object synchCCData = new Object();

    /**
     * Multi-cast list of caller context objects for tracking listeners for this
     * video output port. At any point in time this list will be the complete
     * list of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    /**
     * nInit
     * 
     * initializes JNI for this class. Does not set any fields.
     * 
     */
    private static native void nInit();

    /**
     * nIsContentProtected
     * 
     * Retrieves IsContentProtected state
     * 
     * @return IsContentProtected boolean
     */
    private static native boolean nIsContentProtected(int videoPortHandle);

    /**
     * nInitValues
     * 
     * Used during construction to initialize all values.
     * 
     * @param videoPortHandle
     * @return
     */
    private native boolean nInitValues(int videoPortHandle);

    /**
     * nInitCurrentConfig
     * 
     * Used during construction get the initial current config set by platform.
     * 
     * @param videoPortHandle
     * @return
     */
    private native boolean nInitCurrentConfig(int videoPortHandle);

    /**
     * nRefreshDisplayInfo
     * 
     * refreshes display attributes and connected. Used when a display is
     * connected to a port and the port supports the dynamic detection of
     * connection/disconnection
     * 
     * @param videoPortHandle
     */
    private native boolean nRefreshDisplayInfo(int videoPortHandle);

    private native boolean nSetCurrentOutputConfig(int videoPortHandle, int intValue);

    private Object synchRefresh = new Object();

    public VideoOutputSettingsProxy(DeviceSettingsVideoOutputPortImpl parentPort)
    {
        this.parentVideoPort = parentPort;

        int p = getParentVideoPortHandle();
        if (log.isDebugEnabled())
        {
            log.debug("ParentVideoPortHandle = " + p);
        }
        boolean q1 = nInitValues(p);

        boolean q2 = nInitCurrentConfig (p);

        // after values have been initialized, then infer if dynamic output
        // configuration is supported...
        VideoOutputConfiguration voc[] = this.getSupportedConfigurations();
        for (int i = 0; i < voc.length; i++)
        {
            if (voc[i] instanceof DynamicVideoOutputConfiguration)
            {
                this.isDynamicConfigSupported = true;
                if (log.isInfoEnabled())
                {
                    log.info("isDynamicConfigSupported = true");
                }
                break;
            }
        }

        if (!q1)
        {
            RuntimeException e = new RuntimeException("Error in nInitValues");
            SystemEventUtil.logCatastrophicError(e);
            throw e;
        }

        if (!q2)
        {
            RuntimeException e = new RuntimeException("Error in nInitCurrentConfig");
            SystemEventUtil.logCatastrophicError(e);
            throw e;
        }

        // before another refresh occurs, set the default values.
        getPersistence().initVideoOutputSettings(this);
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

    public AudioOutputPort getAudioOutputPort()
    {
        throw new UnsupportedOperationException();
    }

    public int getDisplayAspectRatio()
    {
        synchronized (synchRefresh)
        {
            // TODO: I'm fairly certain that this should be from the DISPLAY...
        	// SPS:  We never know what the display AR is, only a set of supported ARs
            switch (Util.getAspectRatio(aspectRatio.width, aspectRatio.height))
            {
                case VideoFormatControl.ASPECT_RATIO_16_9:
                    return VideoFormatControl.DAR_16_9;
                case VideoFormatControl.ASPECT_RATIO_4_3:
                    return VideoFormatControl.DAR_4_3;
                default:
                    return -1;
            }
        }
    }

    public int getAspectRatio()
    {
        synchronized (synchRefresh)
        {
            return Util.getAspectRatio(aspectRatio.width, aspectRatio.height);
        }
    }

    public Hashtable getDisplayAttributes()
    {
        synchronized (synchRefresh)
        {
            if (!isDisplayConnected) return null;

            Hashtable attr = new Hashtable();

            attr.put(AttrManNameKey, this.manufacturerName);
            attr.put(AttrManWeekKey, new Byte(this.manufactureWeek));
            attr.put(AttrManYearKey, new Byte(this.manufactureYear));
            attr.put(AttrProdcodeKey, new Short(this.productCode));
            attr.put(AttrSerialNumKey, new Integer(this.serialNumber));

            return attr;
        }
    }

    public VideoOutputConfiguration getOutputConfiguration()
    {
        return currentConfig;
    }

    public VideoOutputConfiguration findSupportedConfiguration(String name)
    {
        // TODO, TODO_DS: do we need to check for disabled configs?
        VideoOutputConfiguration[] configs = getSupportedConfigurations();

        for (int i = 0; i < configs.length; i++)
        {
            if (configs[i].getName().equals(name))
            {
                return configs[i];
            }
        }
        return null;
    }

    public VideoOutputConfiguration[] getSupportedConfigurations()
    {

        // TODO, TODO_DS: do we need to check for disabled configs?
        if (configArray == null)
        {
            Object[] objArray = configToHandle.keySet().toArray();
            configArray = new VideoOutputConfiguration[objArray.length];

            System.arraycopy(objArray, 0, configArray, 0, objArray.length);
        }
        return configArray;
    }

    public boolean isContentProtected()
    {
        return nIsContentProtected(getParentVideoPortHandle());
    }

    public boolean isDisplayConnected()
    {
        return isDisplayConnected;
    }

    public boolean isDynamicConfigurationSupported()
    {
        return isDynamicConfigSupported;
    }

    public void setOutputConfiguration(VideoOutputConfiguration config) throws FeatureNotSupportedException
    {
        if (config == null)
        {
            throw new IllegalArgumentException("Configuration cannot be null.");
        }

        checkDSExtPermissions();

        if (log.isDebugEnabled())
        {
            log.debug("setOutputConfigNoPermissions: before setting config: getAspectRatio = " + getAspectRatio());
        }
        setOutputConfigNoPermissions(config);
        if (log.isDebugEnabled())
        {
            log.debug("setOutputConfigNoPermissions: after setting config: getAspectRatio = " + getAspectRatio());
        }

        getPersistence().persistPortOutputConfig(parentVideoPort.getUniqueId(), config);
    }

    void setOutputConfigNoPermissions(VideoOutputConfiguration newConfig) throws FeatureNotSupportedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setOutputConfigNoPermissions: parentVideoPort = " + parentVideoPort);
        }

        boolean inputIsDynamicConfig = newConfig instanceof DynamicVideoOutputConfiguration;
        if ( inputIsDynamicConfig && !isDynamicConfigurationSupported() )
        {
            if (log.isErrorEnabled())
            {
                log.error("setOutputConfigNoPermissions: request to set dynamic video output " +
                		               "port active when dynamic configurations are not supported");
            }
        		throw new FeatureNotSupportedException("Dynamic configurations are not supported");
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("+(ds-vop-s) VideoOutputSettingsProxy::setOutputConfigNoPermissions: PRIOR TO updating members via " +
                    "callJniSetCurrConfig: local aspect ratio = " + aspectRatio.width + ":" + aspectRatio.height);
        }

        // Set the VOP at the platform layer
        // TODO: _should_ set platform config even if dynamic... but currently causes CTP issues.
        if(!inputIsDynamicConfig)
        {
            callJniSetCurrConfig(newConfig);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("Dynamic configuration NOT propagated to platform.");
            }
        }
       
        // set local data members
        this.currentConfig = newConfig;
        refresh(); // Causes platform to update appropriate data members 
                   // of this class including this.apsectRatio
        
        if (log.isDebugEnabled())
        {
            log.debug("+(ds-vop-s) VideoOutputSettingsProxy::setOutputConfigNoPermissions: AFTER updating members via " +
                    "callJniSetCurrConfig: local aspect ratio = " + aspectRatio.width + ":" + aspectRatio.height);
        }
 
        // Update coherent config based on apsectRation driven by new VOP config
        updateHScreenAspectRatio(HScreen.getDefaultHScreen(), getAspectRatio());

        if (log.isDebugEnabled())
        {
            log.debug("+(ds-vop-s) VideoOutputSettingsProxy::setOutputConfigNoPermissions, final state of this object:");
        }
        	dump("+(ds-vop-s)     ");
        }


    private void updateHScreenAspectRatio(HScreen screen, int AR)
    {
    	
    	if (AR != VideoFormatControl.ASPECT_RATIO_UNKNOWN)
        {
            if (log.isDebugEnabled())
            {
            	log.debug("+(ds-vop-s) VideoOutputSettingsProxy::updateHScreenAspectRatio: updating coherent configs " +
                		  "with following aspect ratio: " + aspectRatio.width + ":" + aspectRatio.height);
            }
        	((HDScreen) screen).setCoherentConfiguration(AR, true);
        }
        
    }

    void refresh()
    {
        synchronized (synchRefresh)
        {
            // TODO, TODO_DS: handle failure
        	// SPS: failure already handled ???
            if (!nRefreshDisplayInfo(getParentVideoPortHandle()))
            {
                RuntimeException e = new RuntimeException("VideoOutputSettingsProxy refresh failed in JNI");
                SystemEventUtil.logCatastrophicError(e);
                synchRefresh.notifyAll();
                throw e;
            }
            synchRefresh.notifyAll();
        }
    }

    int lastEventCounter = -1;

    void handleRefresh(int eventCounter)
    {
        // this method will refresh all display info, including lists of supported configs

        if (log.isDebugEnabled())
        {
            log.debug("handleRefresh -- enter");
        }
        synchronized (synchRefresh)
        {
            if (log.isDebugEnabled())
            {
                log.debug("handleRefresh -- clearing maps");
            }
            configToHandle.clear();
            handleToConfig.clear();
            
            if (log.isDebugEnabled())
            {
                log.debug("handleRefresh: calling nInitValues");
            }
            if (!nInitValues(getParentVideoPortHandle()))
            {
                RuntimeException e = new RuntimeException("VideoOutputSettingsProxy refresh failed in JNI");
                SystemEventUtil.logCatastrophicError(e);
                synchRefresh.notifyAll();
                throw e;
            }
            synchRefresh.notifyAll();

            lastEventCounter = eventCounter; // thread atomic action
        }

        if (log.isDebugEnabled())
        {
            log.debug("handleRefresh -- exit");
        }
    }

    void waitForRefresh(int eventCounter)
    {
        synchronized (synchRefresh)
        {
            if (eventCounter == lastEventCounter)
            {
                return; // refresh has occurred, don't wait
            }
            else
            {
                try
                {
                    synchRefresh.wait(2000);
                }
                catch (InterruptedException e)
                {
                    // do nothing if interrupted, just return
                }
                // if timed out do nothing. It is not worth the effort to catch
                // a almost never error. if
                // this does time out, a lot more will be going wrong that is a
                // lot more obvious
            }
        }
        return;
    }

    void handleResolutionChange(int eventCounter)
    {
        VideoOutputConfiguration curOutputConfig = getOutputConfiguration();
        if (isDynamicConfigurationSupported() && curOutputConfig instanceof DynamicVideoOutputConfiguration)
        {
            int oldAr = getDisplayAspectRatio();

            refresh();

            int newAr = getDisplayAspectRatio();

            if (newAr != oldAr)
            {
                // okay, we need to handle this.
                DynamicVideoOutputConfiguration curDynamic = (DynamicVideoOutputConfiguration) curOutputConfig;

                VideoResolution desiredRes = new VideoResolution(null, newAr, 0.0F, VideoResolution.SCANMODE_UNKNOWN);
                FixedVideoOutputConfiguration desiredFixed = curDynamic.getOutputResolution(desiredRes);
                if (desiredFixed != null)
                {
                    try
                    {
                        setOutputConfigNoPermissions(desiredFixed);
                    }
                    catch (FeatureNotSupportedException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("resolution change failed due to device not supporting setting output config", e);
                        }
                    }
                }

                // don't need the change to the HScreen if aspect ratio in this
                // method since the HSettingsProxy code
                // that deals with that issue is listening to the same event
                // that caused this to be called.

            }
        }
    }

    private void callJniSetCurrConfig(VideoOutputConfiguration config) throws FeatureNotSupportedException
    {
        Integer configHandle = (Integer) configToHandle.get(config);

        if (configHandle != null)
        {
            if (!nSetCurrentOutputConfig(getParentVideoPortHandle(), configHandle.intValue()))
            {
                throw new FeatureNotSupportedException("Can not set output config");
            }
        }
        else
            throw new IllegalArgumentException("configuration did not equal any supported configuration");
    }

    /*
     * Methods used by JNI
     */
    // /**
    // * Clear the supported config Hashtable
    // * Used by JNI
    // */
    // private void clearSupportedConfigurations()
    // {
    // configToHandle.clear();
    // handleToConfig.clear();
    // }

    /**
     * Add supported config
     * 
     * used by java
     */
    private void addSupportedConfiguration(VideoOutputConfiguration supportedConfig, int configHandle)
    {
        Integer handle = new Integer(configHandle);
        configToHandle.put(supportedConfig, handle);
        handleToConfig.put(handle, supportedConfig);
    }

    /**
     * Setup the proxy to reflect a fixed configuration
     * 
     * Called from JNI
     * 
     * @param enabled
     * @param name
     * @param rezWidth
     * @param rezHeight
     * @param arWidth
     * @param arHeight
     * @param rate
     * @param interlaced
     * @param supportedConfigMPEHandle
     *            // low level handle for the config
     */
    private void addFixedConfig(boolean enabled, String name, int rezWidth, int rezHeight, int arWidth, int arHeight,
            int rate, boolean interlaced, int configHandle)
    {
        FixedVideoOutputConfiguration fixed = createFixedVideoOutputConfiguration(enabled, name, rezWidth, rezHeight,
                rate, interlaced, S3DFormatTypes.FORMAT_2D);

        addSupportedConfiguration(fixed, configHandle);
    }

    private void addFixedConfig(boolean enabled, String name, int rezWidth, int rezHeight, int arWidth, int arHeight,
            int rate, boolean interlaced, int stereoscopicMode, int configHandle)
    {
        FixedVideoOutputConfiguration fixed = createFixedVideoOutputConfiguration(enabled, name, rezWidth, rezHeight,
                rate, interlaced, stereoscopicMode);

        addSupportedConfiguration(fixed, configHandle);
    }

    private void updateFixedConfig(boolean enabled, int configHandle)
    {
        Integer handle = new Integer(configHandle);
        FixedVideoOutputConfigurationImpl config = (FixedVideoOutputConfigurationImpl) handleToConfig.get(handle);

        config.setEnabled(enabled);
    }

    /**
     * Construct a new FixedVideoOutputConfiguration
     * 
     * @param name
     * @param rezWidth
     * @param rezHeight
     * @param rate
     * @param interlaced
     * @return
     */
    private FixedVideoOutputConfiguration createFixedVideoOutputConfiguration(boolean enabled, String name,
            int rezWidth, int rezHeight, int rate, boolean interlaced, int stereoscopicMode)
    {
        VideoResolution res = createVideoResolution(rezWidth, rezHeight, rate, interlaced, stereoscopicMode);
        FixedVideoOutputConfigurationImpl fixed = new FixedVideoOutputConfigurationImpl(enabled, name, res);
        return fixed;
    }

    private DynamicVideoOutputConfiguration workingConfig = null;

    /**
     * Start the construction of a dynamic config.
     * 
     * Called from JNI
     * 
     * @param configMPEHandle
     */
    private void startDynamicConfig(int configMPEHandle)
    {
        /* workaround for serialization as in FixedVideoOutputCingurationImpl */
        workingConfig = new DynamicVideoOutputConfigurationProxy();
    }

    /**
     * Add a output resolution to the working config.
     * 
     * Called from JNI
     * 
     * @param inRezWidth
     * @param inRezHeight
     * @param inArWidth
     * @param inArHeight
     * @param inRate
     * @param inInterlaced
     * @param fixedName
     * @param fixedRezWidth
     * @param fixedRezHeight
     * @param fixedArWidth
     * @param fixedArHeight
     * @param fixedRate
     * @param fixedInterlaced
     * @param configMPEHandle
     */
    private void addOutputResolution(int inRezWidth, int inRezHeight, int inArWidth, int inArHeight, int inRate,
            boolean inInterlaced, int inStereoscopicMode, String fixedName, int fixedRezWidth, int fixedRezHeight, int fixedArWidth,
            int fixedArHeight, int fixedRate, boolean fixedInterlaced, int fixedStereoscopicMode, int configMPEHandle)
    {
        VideoResolution inRes = createVideoResolution(inRezWidth, inRezHeight, inRate, inInterlaced, 
            inStereoscopicMode);

        // assume that 'enabled' is true
        boolean enabled = true;
        FixedVideoOutputConfiguration fixed = createFixedVideoOutputConfiguration(enabled, fixedName, fixedRezWidth,
                fixedRezHeight, fixedRate, fixedInterlaced, fixedStereoscopicMode);

        workingConfig.addOutputResolution(inRes, fixed);
    }

    /**
     * Add a output resolution to the working config.
     * 
     * Called from JNI
     * 
     * @param inRezWidth
     * @param inRezHeight
     * @param inArWidth
     * @param inArHeight
     * @param inRate
     * @param inInterlaced
     * @param fixedName
     * @param fixedRezWidth
     * @param fixedRezHeight
     * @param fixedArWidth
     * @param fixedArHeight
     * @param fixedRate
     * @param fixedInterlaced
     * @param configMPEHandle
     */
/*    private void addOutputResolution(int inRezWidth, int inRezHeight, int inArWidth, int inArHeight, int inRate,
            boolean inInterlaced, String fixedName, int fixedRezWidth, int fixedRezHeight, int fixedArWidth,
            int fixedArHeight, int fixedRate, boolean fixedInterlaced, int stereoscopicMode, int configMPEHandle)
    {
        VideoResolution inRes = createVideoResolution(inRezWidth, inRezHeight, inRate, inInterlaced, 
            stereoscopicMode);

        // assume that 'enabled' is true
        boolean enabled = true;
        FixedVideoOutputConfiguration fixed = createFixedVideoOutputConfiguration(enabled, fixedName, fixedRezWidth,
                fixedRezHeight, fixedRate, fixedInterlaced, stereoscopicMode);

        workingConfig.addOutputResolution(inRes, fixed);
    }
    */

    /**
     * end the construction of a Dynamic configuration
     * 
     * Called from JNI
     * 
     * @param configMPEHandle
     */
    private void endDynamicConfig(int configMPEHandle)
    {
        addSupportedConfiguration(workingConfig, configMPEHandle);
        workingConfig = null;
    }

    private VideoResolution createVideoResolution(int rezWidth, int rezHeight, int rate, boolean interlaced,
        int stereoscopicMode)
    {
        int scan = VideoResolution.SCANMODE_UNKNOWN;
        Dimension rez = new Dimension(rezWidth, rezHeight);
        int ar = Util.getAspectRatio(rezWidth, rezHeight);

        if (interlaced)
        {
            scan = VideoResolution.SCANMODE_INTERLACED;
        }
        else
        {
            scan = VideoResolution.SCANMODE_PROGRESSIVE;
        }
        VideoResolution res = new VideoResolution(rez, ar, rate, scan, stereoscopicMode);
        return res;
    }

    /* called from JNI */
    private void setCurrOutputConfigUsingHandle(int configMPEHandle)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setCurrOutputConfigUsingHandle: parentVideoPort = " + parentVideoPort);
        }

        VideoOutputConfiguration config = (VideoOutputConfiguration) this.handleToConfig.get(new Integer(
                configMPEHandle));
        if (config == null)
        {
            SystemEventUtil.logCatastrophicError("unable to match config and handle", new IllegalArgumentException());
        }
        else
        {
            currentConfig = config;
            refresh();

            FixedVideoOutputConfiguration portFixedConfig = (FixedVideoOutputConfiguration) currentConfig;
        }
    }

    private void checkDSExtPermissions()
    {
        DeviceSettingsHostManagerImpl.checkPermissions();
    }

    // Initialize JNI layer.
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }

    public int getParentVideoPortHandle()
    {
        return parentVideoPort.getHandle();
    }

    /**
     * getUniqueId
     * 
     * see the remarks for parentUniqueId
     * 
     * @return the unique id of the parent port
     */
    public String getUniqueId()
    {
        return parentUniqueId;
    }

    private static DeviceSettingsHostPersistence getPersistence()
    {
        return (DeviceSettingsHostPersistence) DeviceSettingsHostImpl.getHostPersistence();
    }

    public void initFromPersistence(VideoOutputConfiguration defaultConfig)
    {
        if (defaultConfig != null)
        {
            try
            {
                setOutputConfigNoPermissions(defaultConfig); // do not write the
                                                             // persistence out
                                                             // during init of
                                                             // persistence!
            }
            catch (FeatureNotSupportedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(e);
                }
                ; // do nothing
        }
        }
        return;
    }

    // dump the current state of this object
    public void dump(String sPre)
    {
        if (log.isDebugEnabled())
        {
            log.debug(sPre + "parentUniqueId: " + parentUniqueId +
                    sPre + "aspectRatio: " + aspectRatio.width + ":" + aspectRatio.height +
                    sPre + "currentConfig: " + currentConfig.getName() +
                    (configArray == null ? "null configArray" : sPre + "num configs: " + configArray.length) +
                    sPre + "isDisplayConnected: " + isDisplayConnected +
                    sPre + "isDynamicConfigSupported: " + isDynamicConfigSupported +
                    sPre + "isContentProtected: " + isContentProtected());
        }
    }

}
