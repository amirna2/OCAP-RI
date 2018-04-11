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

package org.cablelabs.impl.havi.port.mpe;

import java.awt.Dimension;

import javax.media.Player;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.havi.ExtendedVideoDevice;
import org.cablelabs.impl.havi.ReservationAction;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.mpe.MediaAPIImpl;
import org.cablelabs.impl.media.player.AVPlayer;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.player.VideoDevice;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.NativeHandle;
import org.davic.resources.ResourceClient;
import org.dvb.media.VideoFormatControl;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;


/**
 * Implementation of {@link HVideoDevice} for the MPE port intended to run on an
 * OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @author Alan Cossitt (DSExt)
 */
public class HDVideoDevice extends HVideoDevice implements HDScreenDevice, ExtendedVideoDevice, VideoDevice
{
    /**
     * Destination for video device. DISPLAY_DEST_TV means the background video
     * device, DISPLAY_DEST_VIDEO means PIP or foreground. Reflects C platform.h
     * DispDeviceDest enumeration.
     */
    public static final int DISPLAY_DEST_TV = 0;

    public static final int DISPLAY_DEST_VIDEO = 1;

    public static final int DISPLAY_DEST_UNKNOWN = -1;

    private final Object videoControllerLock = new Object(); 
    /**
     * The native device handle.
     */
    private int nDevice;

    /**
     * Reference to the containing screen.
     */
    private HDScreen screen;

    /**
     * Configurations.
     */
    private HVideoConfiguration[] configurations = null;

    /**
     * Current configuration.
     */
    private HVideoConfiguration currentConfiguration = null;

    /**
     * Default configuration.
     */
    private HVideoConfiguration defaultConfiguration = null;

    private int supportedDFCs[] = { VideoFormatControl.DFC_PROCESSING_UNKNOWN };

    /**
     * A private lock for video association.
     */
    private Object lock = new Object();

    // Logging
    private static final Logger log = Logger.getLogger(HDVideoDevice.class);

    /**
     * Not-contributing configuration, if present. This isn't EXACTLY the same
     * as <code>HVideoDevice.NOT_CONTRIBUTING</code>, but is close.
     */
    private HVideoConfiguration notContributing = null;

    private int videoDest = DISPLAY_DEST_UNKNOWN;

    /**
     * Constructs a video device based upon the given native device handle.
     * 
     * @param nDevice
     *            the native device handle
     */
    HDVideoDevice(ExtendedScreen screen, int nDevice)
    {
        this.screen = (HDScreen) screen;
        this.nDevice = nDevice;

        // make native calls, create configurations
        initConfigurations();
    }

    /**
     * Initializes the configurations associated with this device.
     */
    private void initConfigurations()
    {
        if (log.isDebugEnabled())
        {
            log.debug("initConfigurations");
        }
        videoDest = HDScreen.nGetDeviceDest(nDevice); // make sure
                                                      // isBackgroundVideo works

        if (!dsExtUsed) // if DSExt is NOT in place, the previous algorithm is
                        // completely untouched so that it positively,
                        // absolutely, and without a doubt, unaffected by DSExt.
        {
            if (log.isDebugEnabled())
            {
                log.debug("ds extension not in place");
            }
            // Get the current/default configuration
            int curr = HDScreen.nGetDeviceConfig(nDevice);
            if (log.isDebugEnabled())
            {
                log.debug("current config handle: " + curr);
            }
            int nNotContrib = (HVideoDevice.NOT_CONTRIBUTING == null) ? 0
                    : ((NativeHandle) HVideoDevice.NOT_CONTRIBUTING).getHandle();

            // Initialize the configurations
            int config[] = HDScreen.nGetDeviceConfigs(nDevice);
            java.util.Vector v = new java.util.Vector();
            for (int i = 0; i < config.length; ++i)
            {
                HDVideoConfiguration cfg = new HDVideoConfiguration(this, config[i]);
                if (config[i] == nNotContrib)
                {
                    notContributing = cfg;
                }
                else
                {
                    v.addElement(cfg);
                }
                if (config[i] == curr)
                {
                    // Save current/default configuration
                    defaultConfiguration = currentConfiguration = cfg;
                    if (log.isDebugEnabled())
                    {
                        log.debug("currentConfiguration updated to: " + cfg);
                    }
                }
            }
            configurations = new HDVideoConfiguration[v.size()];
            v.copyInto(configurations);

            // Shouldn't occur... but could
            if (defaultConfiguration == null || currentConfiguration == null)
            {
                throw new NullPointerException();
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("ds extension in place");
            }
            // TODO_DS, TODO: Needs DSExt persistence for the current config
            // Get the current/default configuration
            int curr = HDScreen.nGetDeviceConfig(nDevice);
            if (log.isDebugEnabled())
            {
                log.debug("current config handle: " + curr);
            }
            int nNotContrib = (HVideoDevice.NOT_CONTRIBUTING == null) ? 0
                    : ((NativeHandle) HVideoDevice.NOT_CONTRIBUTING).getHandle();
            MediaAPIImpl mediaApi = (MediaAPIImpl) MediaAPIImpl.getInstance();

            // Initialize the configurations

            if (isBackgroundVideo())
            {
                supportedDFCs = mediaApi.getSupportedDFCs(nDevice);
                if (log.isDebugEnabled())
                {
                    log.debug("background device - supported DFCs: " + Arrays.toString(supportedDFCs));
                }
            }
            int config[] = HDScreen.nGetDeviceConfigs(nDevice);
            java.util.Vector v = new java.util.Vector();
            for (int i = 0; i < config.length; ++i)
            {
                // TODO_DS, TODO: equals is incorrect, needs DFC and persistence
                if (config[i] == nNotContrib)
                {
                    notContributing = new HDVideoConfiguration(this, config[i]);
                }
                else
                {
                    for (int j = 0; j < supportedDFCs.length; j++)
                    {
                        int dfc = supportedDFCs[j];
                        int defaultDfc = VideoFormatControl.DFC_PROCESSING_NONE;
                        if (dfc == VideoFormatControl.DFC_PROCESSING_UNKNOWN)
                        {
                            defaultDfc = VideoFormatControl.DFC_PROCESSING_UNKNOWN;
                        }
                        HDVideoConfiguration cfg = new HDVideoConfiguration(this, config[i], dfc);
                        v.addElement(cfg);

                        // TODO, TODO_DS: use persistence to get current config
                        // and DFC and use .equals
                        if (defaultConfiguration == null && cfg.equals(curr, defaultDfc))
                        {
                            // Save current/default configuration
                            defaultConfiguration = currentConfiguration = cfg;
                            if (log.isDebugEnabled())
                            {
                                log.debug("current config updated to: " + cfg);
                            }
                        }
                    }
                }
            }
            configurations = new HDVideoConfiguration[v.size()];
            v.copyInto(configurations);

            // Shouldn't occur... but could
            if (defaultConfiguration == null || currentConfiguration == null)
            {
                throw new NullPointerException();
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("default configuration: " + defaultConfiguration + ", current configuration: "
                    + currentConfiguration + ", configurations: " + Arrays.toString(configurations));
        }
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return nDevice;
    }

    // Definition copied from superclass
    public boolean tempReserveDevice() throws HPermissionDeniedException
    {
        return super.tempReserveDevice();
    }

    // Definition copied from superclass
    public void withReservation(ReservationAction action) throws HPermissionDeniedException, HConfigurationException
    {
        super.withReservation(action);
    }

    // Definition copied from superclass
    public int getMatchStrength(HScreenConfiguration hsc, HScreenConfigTemplate hsct)
    {
        int strength = super.getMatchStrength(hsc, hsct);
        // if returns -1, then no match possible, this can occur because hsct is
        // of the wrong class, or
        // if a required preference is not available, etc.

        if (dsExtUsed && strength >= 0) // if device extension and match is
                                        // still possible, deal with ZOOM_MODE
        {
            HDVideoConfiguration hvc = (HDVideoConfiguration) hsc;
            // Temporary for template priorities
            int p;

            // Add strength of match for ZOOM_MODE preference
            if ((p = hsct.getPreferencePriority(ZOOM_PREFERENCE)) != HScreenConfigTemplate.DONT_CARE)
            {
                Integer zoomPreferenceI = (Integer) hsct.getPreferenceObject(ZOOM_PREFERENCE);
                int zoomPreferenceDFC = zoomPreferenceI.intValue();

                boolean match = (hvc.getPlatformDfc() == zoomPreferenceDFC);

                if (((p == HScreenConfigTemplate.REQUIRED) && !match)
                        || ((p == HScreenConfigTemplate.REQUIRED_NOT) && match)) return -1;
                if (((p == HScreenConfigTemplate.PREFERRED) && match)
                        || ((p == HScreenConfigTemplate.PREFERRED_NOT) && !match))
                    strength += HScreenDevice.STRENGTH_INCREMENT;
            }
        }
        return strength;
    }

    // Definition copied from superclass
    public String getIDstring()
    {
        String idString = HDScreen.nGetDeviceIdString(nDevice);
        if (idString == null) idString = "VideoDevice" + nDevice;
        return idString;
    }

    // Definition copied from superclass. Must be dynamic since this could have
    // changed at the MPE/MPEOS layer
    // (current configuration could have changed).
    // TODO: should java code get the current configuration and get the value
    // from that?
    // gets a value such as 16:9 or 4:3
    public Dimension getScreenAspectRatio()
    {
        Dimension d = new Dimension();
        return HDScreen.nGetDeviceScreenAspectRatio(nDevice, d);
    }

    public int getAspectRatio()
    {
        Dimension d = getScreenAspectRatio();
        return Util.getAspectRatio(d);
    }

    // Definition copied from superclass
    public ExtendedScreen getScreen()
    {
        return screen;
    }

    // Definition copied from superclass
    public HVideoConfiguration[] getConfigurations()
    {
        HVideoConfiguration[] copy = new HVideoConfiguration[configurations.length];
        System.arraycopy(configurations, 0, copy, 0, configurations.length);
        return copy;
    }

    // Definition copied from superclass
    public HVideoConfiguration getDefaultConfiguration()
    {
        return defaultConfiguration;
    }

    // Definition copied from superclass
    public HVideoConfiguration getCurrentConfiguration()
    {
        synchronized (screen.lock)
        {
            return (currentConfiguration == notContributing) ? HVideoDevice.NOT_CONTRIBUTING : currentConfiguration;
        }
    }

    // Definition copied from HDScreenDevice
    public HScreenConfiguration getScreenConfig()
    {
        synchronized (screen.lock)
        {
            return currentConfiguration;
        }
    }

    /**
     * Returns the <i>non-contributing</i> configuration if there is one. This
     * is only to be used within the implementation; the configuration should
     * not be exposed to the public API.
     * 
     * @return the <i>non-contributing</i> configuration or <code>null</code>
     */
    HVideoConfiguration getNotContrib()
    {
        return notContributing;
    }

    // Definition copied from superclass
    // TODO, TODO_DS: make sure old tests for this code pass. Changed == to
    // .equals in several spots.
    public boolean setVideoConfiguration(final HVideoConfiguration hvc) throws SecurityException,
            HPermissionDeniedException, HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setVideoConfiguration: " + hvc);
        }

        // Check if it's a valid configuration
        boolean valid = false;
        for (int i = 0; i < configurations.length; ++i)
        {
            if (configurations[i].equals(hvc)) // TODO, TODO_DS: Test this
                                               // equals
            {
                valid = true;
                break;
            }
        }
        if (!valid) throw new HConfigurationException("Unsupported configuration");

        // Make sure the caller has reserved the device
        synchronized (screen.lock)
        {
            withReservation(new ReservationAction()
            {
                public void run() throws HPermissionDeniedException, HConfigurationException
                {
                    // If same, then we are done (permission check was enough)
                    // TODO, TODO_DS: Test this equals
                    if (currentConfiguration.equals(hvc))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("current configuration equal - not updating configuration");
                        }
                        return;
                    }

                    if (!HDScreen.nSetDeviceConfig(nDevice, ((NativeHandle) hvc).getHandle())) // TODO:
                                                                                               // !!!!
                                                                                               // util
                                                                                               // func!
                                                                                               // to
                                                                                               // remove
                                                                                               // cast
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setVideoConfiguration: calling changeConfiguration...");
                        }

                        // No conflict, simply set this configuration
                        changeConfiguration(hvc);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("setVideoConfiguration: calling setWithCoherentConfigurations...");
                        }

                        // There is a conflict.
                        // "Implicitly" reserve the other devices,
                        // Set a coherent configuration.
                        screen.setWithCoherentConfigurations(hvc);
                    }
                }
            });
        } // synchronized(screen.lock)

        return true;
    }

    // Definition copied from HDScreenDevice
    public void changeConfiguration(HScreenConfiguration config)
    {
        if (log.isInfoEnabled())
        {
            log.info("changeConfiguration - currentConfiguration updated to: " + config);
        }
        currentConfiguration = (HVideoConfiguration) config;
        if (dsExtUsed) // if DSEXT, then set the default platform DFC
        {
            HDVideoConfiguration hdConfig = (HDVideoConfiguration) config;
            if (hdConfig.getPlatformDfc() != VideoFormatControl.DFC_PROCESSING_UNKNOWN
                    && ((HDVideoDevice) hdConfig.getDevice()).isBackgroundVideo())
            {
                int result = HDScreen.nSetDefaultPlatformDFC(nDevice, hdConfig.getPlatformDfc());
                if (log.isDebugEnabled())
                {
                    log.debug("changeConfiguration - device settings extension available, background device and platform dfc is not PROCESSING_UNKNOWN - platform dfc set to: "
                            + hdConfig.getPlatformDfc() + ", result: " + result);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("changeConfiguration - config dfc: " + hdConfig.getPlatformDfc()
                            + ", isBackgroundVideo: " + ((HDVideoDevice) hdConfig.getDevice()).isBackgroundVideo()
                            + " - not setting platform dfc to: " + hdConfig.getPlatformDfc());
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("changeConfiguration - device settings extension not available - not setting platform dfc from config: "
                        + config);
            }
        }
        notifyScreenConfigListeners(config);
    }

    /**
     * Refresh the video device by calling the repaint method. This forces the
     * paint method to be called. The repaint is only performed if the current
     * configuration is the one that has changed.
     * 
     * @param hvc
     *            the video configuration that changed
     */
    void refresh(HVideoConfiguration hvc)
    {
        // !!!!FINISH
        // TODO(?)
    }

    /**
     * Return MediaLocator for the DataSource associated with the Player that is
     * the video controller.
     */
    public java.lang.Object getVideoSource() throws java.lang.SecurityException, HPermissionDeniedException
    {
        synchronized(videoControllerLock)
        {
            if (videoController != null)
            {
                DataSource ds = null;
                if (videoController instanceof ServicePlayer)
                {
                    ds = videoController.getSource();
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("unknown Player type");
                    }
                }

                if (ds != null) return ds.getLocator();
            }
        }
        return null;
    }

    public java.lang.Object getVideoController() throws java.lang.SecurityException, HPermissionDeniedException
    {
        synchronized (videoControllerLock)
        {
            if (videoController != null)
            {
                // Caller must be the same as the owner of the player, or null
                // is returned.
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                if (ccm.getCurrentContext().equals(videoController.getOwnerCallerContext())) return videoController;
            }
            return null;
        }
    }

    public boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context)
    {
        return super.reserveDevice(usage, client, context);
    }

    public void releaseDevice(CallerContext cc)
    {
        super.releaseDevice(cc);
    }

    /**
     * The {@link Player} that is acting as the "controller" of the
     * {@link org.cablelabs.impl.media.player.VideoDevice}. This is returned by
     * the {@link #getVideoController()} method.
     */
    private AVPlayer videoController = null;

    public AVPlayer getController()
    {
        synchronized (videoControllerLock)
        {
            return videoController;
        }
    }

    public int reserveAndControlDevice(AVPlayer player, ResourceUsageImpl usage, ResourceClient resClient)
    {
        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("reserveAndControlDevice: " + this);
            }
            // If Player's owning CallerContext has been destroyed, don't allow
            // this operation.
            CallerContext ownerCC = player.getOwnerCallerContext();
            if (ownerCC == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ownerCC is null - returning NO_CALLER_CONTEXT without reserving");
                }
                return NO_CALLER_CONTEXT;
            }

            // Make sure the caller can reserve the video device.
            boolean reserved = reserveDevice(usage, resClient, ownerCC);
            if (log.isDebugEnabled())
            {
                log.debug("reserveDevice result: " + reserved);
            }
            if (!reserved)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("reserveDevice returned false - returning NO_RESERVATION without reserving");
                }
                return NO_RESERVATION;
            }

            // Try to control the video device. If not possible, release the
            // device before returning.
            if (log.isDebugEnabled())
            {
                log.debug("attempting to control video device");
            }
            int status = controlVideoDevice(player, usage);
            if (log.isDebugEnabled())
            {
                log.debug("controlVideoDevice result: " + status);
            }
            if (status != CONTROL_SUCCESS)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("controlVideoDevice did not return CONTROL_SUCCESS - releasing device");
                }
                releaseDevice(ownerCC);
            }
            return status;
        }
    }

    public int controlVideoDevice(AVPlayer player, ResourceUsageImpl resourceUsage)
    {
        AVPlayer tempVideoController;
        synchronized (videoControllerLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("controlVideoDevice: " + this);
            }
            // Ensure that video configuration is contributing.

            HVideoConfiguration vcfg = getCurrentConfiguration();
            if (vcfg == HVideoDevice.NOT_CONTRIBUTING)
            {
                // Try to assign default configuration, which is contributing.
                try
                {
                    if (!setVideoConfiguration(getDefaultConfiguration()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Failed to set configuration on video device: " + this);
                        }
                        return BAD_CONFIGURATION;
                    }
                }
                catch (Exception x)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Exception setting configuration on video device " + this + ": " + x);
                    }
                    return BAD_CONFIGURATION;
                }
            }

            // It's contributing, so attempt to take it away from current
            // controller.

            // This should only succeed if either:
            // (a) there is no controller currently assigned or (b) the assigned
            // controller's
            // owning app has lower priority than the new controller's app (and player attempting to take control is not presenting EAS)
            // (This would already have
            // been determined at the top of this routine.)
            // If a current controller loses control, it will be notified by
            // {@link AVPlayer#loseVideoDeviceControl()}.

            // case (a)
            if (videoController == null)
            {
                videoController = player;
                return CONTROL_SUCCESS;
            }

            // case (b)
            if (videoController != null && (videoController.getOwnerPriority() >= player.getOwnerPriority() && !resourceUsage.isResourceUsageEAS()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Video device " + this + " controlled by higher priority app");
                }
                return INSUFFICIENT_PRIORITY;
            }

            // Do the bookkeeping to change the video controller.
            // Notify current controller that it is no longer the controller.
            tempVideoController = videoController;
            videoController = player;
        }
        tempVideoController.loseVideoDeviceControl();
        return CONTROL_SUCCESS;
    }

    public void relinquishVideoDevice(AVPlayer player)
    {
        synchronized (videoControllerLock)
        {
            if (player == videoController)
            {
                videoController = null;
            }
        }
    }

    public void swapControllers(AVPlayer otherPlayer)
    {
        // Lock this video device.
        synchronized (lock)
        {
            // Get the other player's video device and lock it, also.
            HDVideoDevice otherVd = (HDVideoDevice) otherPlayer.getVideoDevice();
            synchronized (otherVd.lock)
            {
                AVPlayer thisPlayer = videoController;
                thisPlayer.setVideoDevice(otherVd);
                videoController = otherPlayer;
                otherPlayer.setVideoDevice(this);
                otherVd.videoController = thisPlayer;

                // TODO(mas): swapping configuration listeners, etc.
            }
        }
    }

    public boolean isBackgroundVideo()
    {
        if (videoDest == DISPLAY_DEST_TV) return true;

        return false;
    }

    public String toString()
    {
        return "0x" + Integer.toHexString(getHandle());
    }

    public int[] getSupportedDFCs()
    {
        return supportedDFCs;
    }
}
