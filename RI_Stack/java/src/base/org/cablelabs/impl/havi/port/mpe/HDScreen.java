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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.media.VideoFormatControl;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HVideoConfigTemplate;
import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.cablelabs.impl.manager.PropertiesManager;

import org.cablelabs.impl.awt.NativePeer;
import org.cablelabs.impl.havi.ExtendedScreen;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.NativeHandle;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.player.Util;

import java.lang.reflect.Constructor;


/**
 * Implementation of {@link HScreen} for the MPE port intended to run on an OCAP
 * implementation. The {@link Screen} class defines the actual screens (and
 * related devices) for the port.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @author Alan Cossitt (DSExt changes)
 */
public class HDScreen extends HScreen implements ExtendedScreen
{

    private static final Logger log = Logger.getLogger(HDScreen.class.getName());

    /**
     * Global array of all HScreen objects.
     */
    private static HScreen[] hScreens = null;

    private static final String HSCREEN_IMPL_CLASS_PARAM = "OCAP.HScreen.impl";

    /**
     * The native screen handle.
     */
    private int nScreen;

    /**
     * Global lock used when manipulating configurations for this screen.
     */
    Object lock = new Object();

    /**
     * Graphics devices for this screen.
     */
    private HDGraphicsDevice[] graphicsDevices;

    /**
     * Video devices for this screen.
     */
    private HDVideoDevice[] videoDevices;

    /**
     * Background devices for this screen.
     */
    private HDBackgroundDevice[] backgroundDevices;

    /**
     * The set of coherent configurations supported for this screen.
     */
    private HDCoherentConfig[] coherentConfigs = null;

    /**
     * Is DSExt (Device Settings Extension) being used.
     */
    protected static final boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);

    private MediaAPI mediaAPI;

    /**
     * Initializes all of the screens. Called by the toolkit at initialization
     * time.
     */
    private static void initScreens()
    {
        // Synchronize on something inaccessible to apps...
        synchronized (HDToolkit.class)
        {
            if (hScreens != null) return;

            // Initialize the screens
            int screens[] = nGetScreens();
            hScreens = new HScreen[Math.max(1, screens.length)]; // only support
                                                                 // 1 for now

            try
            {
                // This code determines the appropriate HScreen class to implement based on the property
                // OCAP.HScreen.impl in the properties files (e.g. base.properties, ds.properties, etc).
                // Each HScreen implementation must derive from HScreen and must implement a constructor 
                // with a single integer parameter equal to the native screen handle

                String hScreenClassName = PropertiesManager.getInstance().getPropertyValueByPrecedence(
                        HSCREEN_IMPL_CLASS_PARAM);
                if (log.isDebugEnabled())
                {
                    log.debug("HDScreen.initScreens: hScreenClassName = " + hScreenClassName);
                }

                Class hScreenClass = Class.forName(hScreenClassName);

                Class params[] = new Class[] {Integer.TYPE};
                Constructor ctor = hScreenClass.getDeclaredConstructor(params);

                for (int i = 0; i < hScreens.length; ++i)
                {
                    Object args[] = new Object[]{new Integer(screens[i])};
                    hScreens[i] = (HDScreen) ctor.newInstance(args);
                }
            }
            catch (Exception ex)
            {
                if (log.isErrorEnabled())
                {
                    log.error("HDScreen.initScreens: error instantiating HDScreen: " + ex);
                }
            }
        }
    }

    /**
     * Construct a screen based upon the given native screen handle.
     * 
     * @param nScreen
     *            the native screen handle
     */
    HDScreen(int nScreen)
    {
        this.nScreen = nScreen;

        if (log.isDebugEnabled())
        {
            log.debug("HDScreen: inside constructor");
        }

        // make native calls, create devices
        initDevices();

        // make native calls, create coherent configurations
        initConfigs();
    }

    /**
     * Initializes the devices associated with this screen.
     */
    private void initDevices()
    {
        try
        {
            // Create a mapping of MPE gfx handles to AWT GraphicsDevice(s)
            GraphicsDevice[] awtDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            Hashtable mapping = new Hashtable();
            if (awtDev != null)
            {
                for (int i = 0; i < awtDev.length; ++i)
                {
                    if (awtDev[i] instanceof NativePeer)
                    {
                        mapping.put(new Integer(((NativePeer) awtDev[i]).getPeer()), awtDev[i]);
                    }
                }
            }

            // Initialize the graphics devices...
            // Only support 1 per screen for now
            int gfx[] = nGetDevices(nScreen, TYPE_GFX);
            graphicsDevices = new HDGraphicsDevice[Math.min(gfx.length, 1)]; // Only
                                                                             // support
                                                                             // one
                                                                             // for
                                                                             // now
            for (int i = 0; i < graphicsDevices.length; ++i)
            {
                graphicsDevices[i] = new HDGraphicsDevice(this, gfx[i],
                        (GraphicsDevice) mapping.get(new Integer(gfx[i])));
            }
            mapping = null;
            gfx = null;

            // Initialize the background devices...
            int bg[] = nGetDevices(nScreen, TYPE_BG);
            backgroundDevices = new HDBackgroundDevice[bg.length];

            for (int i = 0; i < backgroundDevices.length; ++i)
            {
                backgroundDevices[i] = new HDBackgroundDevice(this, bg[i]);
            }
            bg = null;

            // Initialize the video devices...
            int vid[] = nGetDevices(nScreen, TYPE_VID);
            videoDevices = new HDVideoDevice[vid.length];
            for (int i = 0; i < videoDevices.length; ++i)
            {
                videoDevices[i] = new HDVideoDevice(this, vid[i]);
            }
            vid = null;
        }
        catch (Throwable t)
        {
            SystemEventUtil.logCatastrophicError(t);
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else if (t instanceof Error) throw (Error) t;
        }
    }

    /**
     * Initialize the coherent configurations associated with this screen.
     */
    private void initConfigs()
    {
        int nConfigs[] = nGetCoherentConfigs(nScreen);

        // Build up a mapping of handles->configurations
        Hashtable mapping = new Hashtable();

        if (log.isDebugEnabled())
        {
            log.debug("HDScreen.initConfigs: mapping graphic devices");
        }
        // Map graphics configuration handles to objects
        for (int i = 0; i < graphicsDevices.length; ++i)
        {
            addMapping(mapping, graphicsDevices[i].getConfigurations());
        }

        if (log.isDebugEnabled())
        {
            log.debug("HDScreen.initConfigs: mapping background devices");
        }
        // Map background configuration handles to objects
        for (int i = 0; i < backgroundDevices.length; ++i)
        {
            addMapping(mapping, backgroundDevices[i].getConfigurations());
        }

        // Map video configuration handles to objects
        for (int i = 0; i < videoDevices.length; ++i)
        {
            if (log.isDebugEnabled())
            {
                log.debug("HDScreen.initConfigs: mapping video device - " + videoDevices[i]);
            }
            addMapping(mapping, videoDevices[i].getConfigurations());

            if (log.isDebugEnabled())
            {
                log.debug("HDScreen.initConfigs: mapping video devices -- non-contributing");
            }
            // include not-contributing config, if there is one
            addMapping(mapping, videoDevices[i].getNotContrib());
        }

        if (!dsExtUsed) // not DSEXT
        {
            coherentConfigs = new HDCoherentConfig[nConfigs.length];

            for (int i = 0; i < nConfigs.length; i++) // TODO, TODO_DS: ++i
                                                      // ?????
            {
                coherentConfigs[i] = new HDCoherentConfig(nConfigs[i], mapping);
            }
        }
        else
        // DSEXT
        {
            // TODO, TODO_DS: comment this code. This section is confusing!
            // TODO, TODO_DS: does this code support (example) two video
            // devices, one with 2 DFCs and
            // the other with 3 DFCs. This should create 5 coherent configs, one
            // of which does not have
            // the device w/ only 2 DFCs.

            int[] supportedDfcs = null;
            for (int i = 0; i < videoDevices.length; ++i)
            {
                if (videoDevices[i].isBackgroundVideo())
                {
                    supportedDfcs = videoDevices[i].getSupportedDFCs();
                    break;
                }
            }

            coherentConfigs = new HDCoherentConfig[nConfigs.length * supportedDfcs.length];

            for (int idx = 0, i = 0; i < nConfigs.length; ++i)
            {
                for (int j = 0; j < supportedDfcs.length; j++)
                {
                    coherentConfigs[idx++] = new HDCoherentConfig(nConfigs[i], mapping, supportedDfcs[j]);
                }
            }

            //guard loop with debug check
            if (log.isDebugEnabled())
            {
                log.debug("+(ds-scr-s) ----- HDScreen.initConfigs(): [" + coherentConfigs.length + "] coherant configs generated");
                
                for (int i = 0; i < coherentConfigs.length; i++)
                {
                	HScreenConfiguration[] curCCSet = coherentConfigs[i].getConfigurations(false);
                	log.debug("+(ds-scr-s)    Coh Cfg [" + i + "], num sub-cfgs = " + curCCSet.length );
	                for (int j = 0; j < curCCSet.length; j++)
	                {
                    	log.debug("+(ds-scr-s)        " + 
                    	          "sub cfg[" + j + "]:  " + 
                                          "Rsln(" + curCCSet[j].getPixelResolution().width   + "x"   + 
                                                    curCCSet[j].getPixelResolution().height  + "), " +
                                            "AR(" + curCCSet[j].getPixelAspectRatio().width  + "x"   +
                                                    curCCSet[j].getPixelAspectRatio().height + ")"   );
	                }
	            }
            }
        }
    }

    // private int[] getAllSupportedDfcs()
    // {
    // if(allSupportedDfcs != null)
    // {
    // int nMaxCount = 0;
    // for(int i = 0; i < videoDevices.length; i++)
    // {
    // nMaxCount += videoDevices[i].getSupportedDFCs().length;
    // }
    //            
    // int[] tempAllSupported = new int[nMaxCount];
    // int tempIdx = 0;
    // for(int i = 0; i < videoDevices.length; i++)
    // {
    // int[] deviceDfcs = videoDevices[i].getSupportedDFCs();
    // for(int j = 0; j < deviceDfcs.length; j++)
    // {
    // int dfc = deviceDfcs[j];
    // boolean contains = false;
    // for(int k = 0; k < tempIdx; k++)
    // {
    // if(tempAllSupported[k] == dfc)
    // {
    // contains = true;
    // break;
    // }
    // }
    // if(!contains)
    // {
    // tempAllSupported[tempIdx++] = dfc;
    // }
    // }
    // }
    //            
    // allSupportedDfcs = new int[tempIdx];
    //            
    // for(int m = 0; m < tempIdx; m++)
    // {
    // allSupportedDfcs[m] = tempAllSupported[m];
    // }
    // }
    // return allSupportedDfcs;
    // }

    private static void addMapping(Hashtable mapping, HScreenConfiguration[] configs)
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDScreen.addMapping: numConfigs=" + configs.length);
        }
        for (int j = 0; j < configs.length; j++)
        {
            addMapping(mapping, configs[j]);
        }
    }

    private static void addMapping(Hashtable mapping, HScreenConfiguration config)
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDScreen.addMapping: " + config);
        }

        if (config == null) return;

        HDConfigId configId = (HDConfigId) config;

        UniqueConfigId uniqueId = configId.getUniqueId();

        // Integer handle = new Integer(((NativeHandle)config).getHandle());
        // if (mapping.get(handle) != null)
        // throw new RuntimeException("Impl Error - non-unique configurations");
        // mapping.put(handle, config);

        Object o = mapping.put(uniqueId, config);
        if (o != null)
        {
            String s = "HDScreen.addMapping:  Impl Error - non-unique configuration, o=" + o.toString() + ", config="
                    + config;
            if (log.isDebugEnabled())
            {
                log.debug(s);
            }
            throw new RuntimeException(s);
        }
    }

    // Definition copied from superclass
    public HGraphicsDevice[] getHGraphicsDevices()
    {
        // Copy array of devices
        HGraphicsDevice[] copy = new HGraphicsDevice[graphicsDevices.length];
        System.arraycopy(graphicsDevices, 0, copy, 0, graphicsDevices.length);
        return copy;
    }

    // Definition copied from superclass
    public HGraphicsDevice getDefaultHGraphicsDevice()
    {
        return (graphicsDevices.length > 0) ? graphicsDevices[0] : null;
    }

    // Definition copied from superclass
    public HVideoDevice[] getHVideoDevices()
    {
        // Copy array of devices
        HVideoDevice[] copy = new HVideoDevice[videoDevices.length];
        System.arraycopy(videoDevices, 0, copy, 0, videoDevices.length);
        return copy;
    }

    // Definition copied from superclass
    public HVideoDevice getDefaultHVideoDevice()
    {
        return (videoDevices.length > 0) ? videoDevices[0] : null;
    }

    // Definition copied from superclass
    public HBackgroundDevice[] getHBackgroundDevices()
    {
        // Copy array of devices
        HBackgroundDevice[] copy = new HBackgroundDevice[backgroundDevices.length];
        System.arraycopy(backgroundDevices, 0, copy, 0, backgroundDevices.length);
        return copy;
    }

    // Definition copied from superclass
    public HBackgroundDevice getDefaultHBackgroundDevice()
    {
        return (backgroundDevices.length > 0) ? backgroundDevices[0] : null;
    }

    // Definition copied from superclass
    public static HScreen[] getHScreens()
    {
        // Make sure all screens are initialized
        initScreens();

        // Return a copy of the screens
        HScreen[] copy = new HScreen[hScreens.length];
        System.arraycopy(hScreens, 0, copy, 0, hScreens.length);
        return copy;
    }

    // Definition copied from superclass
    public static HScreen getDefaultHScreen()
    {
        // Make sure all screens are initialized
        initScreens();

        // The first screen is the default on this platform
        return hScreens[0];
    }

    // Definition copied from superclass
    // HScreen implementation is broken
    public HScreenConfiguration[] getCoherentScreenConfigurations(HScreenConfigTemplate[] hscta)
    {
        // Check that the array of templates is not empty
        if ((hscta == null) || (hscta.length == 0)) throw new IllegalArgumentException();

        HDCoherentConfig config = getNativeCoherentScreenConfigurations(hscta);
        if (config != null)
        {
            HScreenConfiguration[] configs = config.getConfigurations(true);

            HScreenConfiguration[] returnedConfigs = new HScreenConfiguration[hscta.length];
            for (int i = 0; i < returnedConfigs.length; i++)
            {
                boolean bVideo = false;
                boolean bGraphics = false;
                boolean bBackground = false;

                if (hscta[i] instanceof HVideoConfigTemplate)
                {
                    bVideo = true;
                }
                else if (hscta[i] instanceof HGraphicsConfigTemplate)
                {
                    bGraphics = true;
                }
                else if (hscta[i] instanceof HBackgroundConfigTemplate)
                {
                    bBackground = true;
                }

                for (int ii = 0; ii < configs.length; ii++)
                {
                    if ((bVideo && configs[ii] instanceof HVideoConfiguration)
                            || (bGraphics && configs[ii] instanceof HGraphicsConfiguration)
                            || (bBackground && configs[ii] instanceof HBackgroundConfiguration))
                    {
                        returnedConfigs[i] = configs[ii];
                    }
                }
            }

            return returnedConfigs;
        }
        else
        {
            return null;
        }
    }

    /**
     * Finds a suitable coherent configuration given the set of configuration
     * templates.
     * 
     * @return the highest scoring coherent config
     */
    private HDCoherentConfig getNativeCoherentScreenConfigurations(HScreenConfigTemplate[] hscta)
    {
        int max = -1;
        HDCoherentConfig config = null;
        for (int i = 0; i < coherentConfigs.length; ++i)
        {
            // TODO, TODO_DS: remove multiple calls to elementAt
            int score = ((HDCoherentConfig) coherentConfigs[i]).score(hscta);
            if (score > max)
            {
                config = coherentConfigs[i];
                max = score;
            }
        }

        return config;
    }

    // Definition copied from superclass
    // Original HScreen implementation is broken
    public boolean setCoherentScreenConfigurations(HScreenConfiguration[] hsca) throws java.lang.SecurityException,
            org.havi.ui.HPermissionDeniedException, org.havi.ui.HConfigurationException
    {
        // Check that the array of templates is not empty
        if ((hsca == null) || (hsca.length == 0)) throw new IllegalArgumentException();

        if (log.isDebugEnabled())
        {
            log.debug("HDScreen: inside setCoherentScreenConfigurations");
        }

        // Lock out other global changes
        synchronized (lock)
        {
            // Will "implicitly" reserve devices not indicated by hsca
            boolean bReturn = setWithCoherentConfigurations(hsca);

            if (bReturn)
            {
                bReturn = setVideoOutputPortConfig();
            }
            

            return bReturn;

        }
    }

    protected boolean setVideoOutputPortConfig()
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDScreen: inside setVideoOutputPortConfig");
        }

        return true;
    }

    // Definition copied from superclass
    public byte getGraphicsImpact(HScreenConfiguration hsc)
    {
        return getImpact(hsc, graphicsDevices);
    }

    // Definition copied from superclass
    public byte getVideoImpact(HScreenConfiguration hsc)
    {
        return getImpact(hsc, videoDevices);
    }

    // Definition copied from superclass
    public byte getBackgroundImpact(HScreenConfiguration hsc)
    {
        return getImpact(hsc, backgroundDevices);
    }

    // Definition copied from superclass
    public boolean isPixelAligned(HScreenConfiguration hsc1, HScreenConfiguration hsc2)
    {
        /*
         * Should be true if... pixels are the same size and location...
         * certainly true if screen areas are same and pixel resolutions are the
         * same
         */

        // same pixel aspect ratio
        if (!hsc1.getPixelAspectRatio().equals(hsc2.getPixelAspectRatio())) return false;

        // normalized "size" of pixels is the same
        Dimension res1 = hsc1.getPixelResolution();
        Dimension res2 = hsc2.getPixelResolution();
        HScreenRectangle area1 = hsc1.getScreenArea();
        HScreenRectangle area2 = hsc2.getScreenArea();
        int width = (int) (area1.width / res1.width * 10000);
        int height = (int) (area1.height / res1.height * 10000);
        if (width != (int) (area2.width / res2.width * 10000) || height != (int) (area2.height / res2.height * 10000))
            return false;

        // difference of normalized offsets are an integral number of pixels
        float x = (area1.x - area2.x) * 10000;
        float y = (area1.y - area2.y) * 10000;

        if ((int) (x / width) % 10000 != 0 || (int) (y / width) % 10000 != 0) return false;

        return true;
    }

    // Definition copied from superclass
    public boolean supportsVideoMixing(HGraphicsConfiguration hgc, HVideoConfiguration hvc)
    {
        // Always assume video mixing is supported
        return true;
    }

    // Definition copied from superclass
    public boolean supportsGraphicsMixing(HVideoConfiguration hvc, HGraphicsConfiguration hgc)
    {
        // Always assume graphics mixing is supported
        return true;
    }

    // Definition copied from superclass
    public HGraphicsConfiguration createEmulatedConfiguration(HGraphicsConfigTemplate[] hgcta)
    {
        return null; // no emulated configurations are supported
    }


    public void setCoherentConfiguration(int videoAspectRatio, Dimension desiredGraphicsResolution, int desiredDFC) throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDScreen::setCoherentConfiguration: videoAspectRatio = " + videoAspectRatio +
            ", desiredGraphicsResolution = " + desiredGraphicsResolution + ", desiredDFC = " + desiredDFC);
        }
        synchronized (lock)
        {
            HVideoDevice[] videoDeviceA = this.getHVideoDevices();

            // if the current video devices match the requested aspect ratio,
            // then don't do anything

            boolean match = false;

            for (int i = 0; i < videoDeviceA.length; i++)
            {
                HDVideoDevice videoDevice = (HDVideoDevice) videoDeviceA[i];

                if (videoDevice.isBackgroundVideo()) // main screen, not PIP
                {
                    int deviceAR = videoDevice.getAspectRatio();

                    if (deviceAR == videoAspectRatio)
                    {
                        match = true;
                    }
                    break;
                }
            }

            if (match) return;  // GORP: WAIT: check graphics rez and DFC

            HGraphicsDevice[] gfxDeviceA = this.getHGraphicsDevices();
            HBackgroundDevice[] bkDeviceA = this.getHBackgroundDevices();

            int numDevices = videoDeviceA.length + gfxDeviceA.length + bkDeviceA.length;
            HScreenConfiguration[] hsca = new HScreenConfiguration[numDevices];
            Hashtable devices = new Hashtable(); // HScreenDevices

            int hscaIdx = 0;
            for (int i = 0; i < videoDeviceA.length; i++)
            {
                HVideoDevice device = videoDeviceA[i];
                hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                devices.put((HScreenDevice) device, (HScreenDevice) device);
            }

            for (int i = 0; i < gfxDeviceA.length; i++)
            {
                HGraphicsDevice device = gfxDeviceA[i];
                hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                devices.put((HScreenDevice) device, (HScreenDevice) device);
            }

            for (int i = 0; i < bkDeviceA.length; i++)
            {
                HBackgroundDevice device = bkDeviceA[i];
                hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                devices.put((HScreenDevice) device, (HScreenDevice) device);
            }

            HScreenConfigTemplate[] hscta = desiredTemplates(hsca, devices);

            // the current scoring does not take into consideration the aspect
            // ratio so this code has
            // to do that itself.

            IdentityHashMap hm = new IdentityHashMap(); // since many scores are
                                                        // of identical values,
                                                        // this has
            // hashmap will not "lose" them by replacing the old
            // map with the new "identical" map. Only if the
            // keys are the same object will this occur.
            for (int i = 0; i < coherentConfigs.length; i++) // TODO, TODO_DS:
                                                             // remove multiple
                                                             // elementAt calls
            {
                int score = coherentConfigs[i].score(hscta);
                hm.put(new Integer(score), coherentConfigs[i]);
                // hm.put(new Integer(score), "A_"+score+"_"+i); // TODO,
                // TODO_DS: remove debug
            }

            TreeMap sortedMap = new TreeMap(new DescendingIntegerComparator()); // sorted
                                                                                // by
                                                                                // key
                                                                                // (score),
                                                                                // highest
                                                                                // scores
                                                                                // first,
                                                                                // allows
                                                                                // dups
            sortedMap.putAll(hm);

            Collection sortedCoherentConfigs = sortedMap.values();

            Iterator iter = sortedCoherentConfigs.iterator();

            boolean bConfigSet = false;

            while (iter.hasNext())
            {
                HDCoherentConfig set = (HDCoherentConfig) iter.next();
                HScreenConfiguration[] screenConfigs = set.getConfigurations(false);

                boolean matchVideo = false;
                boolean matchGraphics = false;
                boolean matchDFC = false;

                if (set.getDFC() == desiredDFC)
                {
                    matchDFC = true;
                }

                // find a coherent config with the correct aspect ratio, reserve devices
                // and select this to the screen. Once this is done, exit the loop
                for (int i = 0; i < screenConfigs.length; i++)
                {
                    HScreenConfiguration screenConfig = screenConfigs[i];
                    if (screenConfig instanceof HDVideoConfiguration)
                    {
                        HDVideoConfiguration vidConfig = (HDVideoConfiguration) screenConfig;
                        HDVideoDevice vidDevice = (HDVideoDevice) vidConfig.getDevice();
                        if (vidDevice.isBackgroundVideo()) // don't use PIP for
                                                           // aspect ratio calc
                        {
                            int configAR = vidConfig.getAspectRatio();
                            if (configAR == videoAspectRatio)
                            {
                                matchVideo = true;
                            }
                        }
                    }
                    if (screenConfig instanceof HGraphicsConfiguration)
                    {
                        HGraphicsConfiguration graphicsConfig = (HGraphicsConfiguration) screenConfig;

                            Dimension configGraphicsResolution = graphicsConfig.getPixelResolution();
                            if (configGraphicsResolution.equals(desiredGraphicsResolution))
                            {
                                matchGraphics = true;
                            }
                    }
                }

                if (matchVideo && matchGraphics && matchDFC)
                {
                    Set compatibleReservedDevices = new HashSet();
                    try
                    {
                        for (Iterator devicesIter = devices.entrySet().iterator();devicesIter.hasNext();)
                        {
                            Map.Entry entry = (Map.Entry)devicesIter.next();
                            HDScreenDevice thisDevice = (HDScreenDevice)entry.getKey();
                            if (thisDevice.tempReserveDevice())
                            {
                                compatibleReservedDevices.add(thisDevice);
                            }
                        }
                        selectCoherentConfig(devices, set);
                        bConfigSet = true;
                    }
                    catch (HPermissionDeniedException e)
                    {
                        SystemEventUtil.logCatastrophicError(e);
                    }
                    catch (HConfigurationException e)
                    {
                        SystemEventUtil.logCatastrophicError(e);
                    }
                    finally
                    {
                        for (Iterator reservedIter = compatibleReservedDevices.iterator();reservedIter.hasNext();)
                        {
                            HScreenDevice thisDevice = (HScreenDevice)reservedIter.next();
                            thisDevice.releaseDevice();
                        }
                    }
                    break;
                }
 
                if (bConfigSet)
                {
                    break;
                }
           }
        }
    }



    /**
     * 
     * @param aspectRatio
     *            VideoFormatControl.ASPECT_RATIO_
     * @throws HConfigurationException
     * @throws HPermissionDeniedException
     * @throws SecurityException
     *             *
     */

    public void setCoherentConfiguration(int videoAspectRatio, boolean bPreserveGraphicsRezAndDfc) throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("+(ds-scr-s) ENTER: setCoherentConfiguration: videoAspectRatio = " + videoAspectRatio + 
                ", bPreserveGraphicsRezAndDfc = " + bPreserveGraphicsRezAndDfc);
        }

        if (bPreserveGraphicsRezAndDfc)
        {
		    // this code sets the coherent config to one with the new video aspect ratio while preserving
		    // the graphics rez and the dfc
            HGraphicsDevice graphicsDevice = this.getDefaultHGraphicsDevice();
            Dimension graphicsResolution = graphicsDevice.getCurrentConfiguration().getPixelResolution();


            HVideoDevice videoDevice = this.getDefaultHVideoDevice();
            int desiredDFC = 0;
            if (videoDevice instanceof HDVideoDevice)
            {
                desiredDFC = getMediaAPI().getDFC(((HDVideoDevice)videoDevice).getHandle());
            }

            setCoherentConfiguration(videoAspectRatio, graphicsResolution, desiredDFC);
        }
        else
        {
 		    // this code sets the coherent config to one with the new video aspect ratio but with potentially
            // different dfc and graphics rez
            synchronized (lock)
            {
                HVideoDevice[] videoDeviceA = this.getHVideoDevices();

                // if the current video devices match the requested aspect ratio,
                // then don't do anything

                boolean match = false;

                for (int i = 0; i < videoDeviceA.length; i++)
                {
                    HDVideoDevice videoDevice = (HDVideoDevice) videoDeviceA[i];

                    if (videoDevice.isBackgroundVideo()) // main screen, not PIP
                    {
                        int deviceAR = videoDevice.getAspectRatio();

                        if (deviceAR == videoAspectRatio)
                        {
                            match = true;
                        }
                        break;
                    }
                }

                if (match) 
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-scr-s) setCoherentConfigurationWithAspectRatio: current Coh. Cfg has same aspect ratio as requested " +
                                "[" + videoAspectRatio + "] ... taking no action");
                    }
            	    return;
                }

                HGraphicsDevice[] gfxDeviceA = this.getHGraphicsDevices();
                HBackgroundDevice[] bkDeviceA = this.getHBackgroundDevices();

                int numDevices = videoDeviceA.length + gfxDeviceA.length + bkDeviceA.length;
                HScreenConfiguration[] hsca = new HScreenConfiguration[numDevices];
                Hashtable devices = new Hashtable(); // HScreenDevices

                int hscaIdx = 0;
                for (int i = 0; i < videoDeviceA.length; i++)
                {
                    HVideoDevice device = videoDeviceA[i];
                    hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                    devices.put((HScreenDevice) device, (HScreenDevice) device);
                }

                for (int i = 0; i < gfxDeviceA.length; i++)
                {
                    HGraphicsDevice device = gfxDeviceA[i];
                    hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                    devices.put((HScreenDevice) device, (HScreenDevice) device);
                }

                for (int i = 0; i < bkDeviceA.length; i++)
                {
                    HBackgroundDevice device = bkDeviceA[i];
                    hsca[hscaIdx++] = (HScreenConfiguration) device.getCurrentConfiguration();
                    devices.put((HScreenDevice) device, (HScreenDevice) device);
                }

                HScreenConfigTemplate[] hscta = desiredTemplates(hsca, devices);

                // the current scoring does not take into consideration the aspect
                // ratio so this code has
                // to do that itself.

                IdentityHashMap hm = new IdentityHashMap(); // since many scores are
                                                            // of identical values,
                                                            // this has
                // hashmap will not "lose" them by replacing the old
                // map with the new "identical" map. Only if the
                // keys are the same object will this occur.
                for (int i = 0; i < coherentConfigs.length; i++) // TODO, TODO_DS:
                                                                 // remove multiple
                                                                 // elementAt calls
                {
                    int score = coherentConfigs[i].score(hscta);
                    hm.put(new Integer(score), coherentConfigs[i]);
                    // hm.put(new Integer(score), "A_"+score+"_"+i); // TODO,
                    // TODO_DS: remove debug
                }

                TreeMap sortedMap = new TreeMap(new DescendingIntegerComparator()); // sorted
                                                                                    // by
                                                                                    // key
                                                                                    // (score),
                                                                                    // highest
                                                                                    // scores
                                                                                    // first,
                                                                                    // allows
                                                                                    // dups
                sortedMap.putAll(hm);

                Collection sortedCoherentConfigs = sortedMap.values();

                Iterator iter = sortedCoherentConfigs.iterator();

                boolean bConfigSet = false;

                while (iter.hasNext())
                {
                    HDCoherentConfig set = (HDCoherentConfig) iter.next();
                    HScreenConfiguration[] screenConfigs = set.getConfigurations(false);

                    // find a coherent config with the correct aspect ratio, reserve devices
                    // and select this to the screen. Once this is done, exit the loop
                    for (int i = 0; i < screenConfigs.length; i++)
                    {
                        HScreenConfiguration screenConfig = screenConfigs[i];
                        if (screenConfig instanceof HDVideoConfiguration)
                        {
                            HDVideoConfiguration vidConfig = (HDVideoConfiguration) screenConfig;
                            HDVideoDevice vidDevice = (HDVideoDevice) vidConfig.getDevice();
                            if (vidDevice.isBackgroundVideo()) // don't use PIP for
                                                               // aspect ratio calc
                            {
                                int configAR = vidConfig.getAspectRatio();
                                if (configAR == videoAspectRatio)
                                {
                                    Set compatibleReservedDevices = new HashSet();
                                    try
                                    {
                                        for (Iterator devicesIter = devices.entrySet().iterator();devicesIter.hasNext();)
                                        {
                                            Map.Entry entry = (Map.Entry)devicesIter.next();
                                            HDScreenDevice thisDevice = (HDScreenDevice)entry.getKey();
                                            if (thisDevice.tempReserveDevice())
                                            {
                                                compatibleReservedDevices.add(thisDevice);
                                            }
                                        }
                                        selectCoherentConfig(devices, set);
                                        bConfigSet = true;
                                    }
                                    catch (HPermissionDeniedException e)
                                    {
                                        SystemEventUtil.logCatastrophicError(e);
                                    }
                                    catch (HConfigurationException e)
                                    {
                                        SystemEventUtil.logCatastrophicError(e);
                                    }
                                    finally
                                    {
                                        for (Iterator reservedIter = compatibleReservedDevices.iterator();reservedIter.hasNext();)
                                        {
                                            HScreenDevice thisDevice = (HScreenDevice)reservedIter.next();
                                            thisDevice.releaseDevice();
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
 
                    if (bConfigSet)
                    {
                        break;
                    }
               }
            }
        }
    }

    /**
     * causes the sort order to be from high to low and fools TreeMap into
     * keeping values whose keys are identical (notice there is no return of 0
     * for equal values).
     */
    private class DescendingIntegerComparator implements Comparator
    {

        public int compare(Object o1, Object o2)
        {
            Integer i1 = (Integer) o1;
            Integer i2 = (Integer) o2;
            if (i1.intValue() > i2.intValue())
                return -1; // make the sorted collects return in descending
                           // order
            else
                return 1;
        }

    }

    /**
     * Attempts to find and set a coherent configuration with as little impact
     * on other devices as possible. This is called by the
     * <code>set<i>Type</i>Configuration()</code> method of the various device
     * types when it is known that setting a configuration requires changes to
     * other devices.
     * <p>
     * This should be called while holding the global <code>HDScreen.lock</code>.
     * <p>
     * This operation is composed of the following steps:
     * <ol>
     * <li>Find a coherent configuration that is suitable.
     * <li>Determine which device/configurations are incompatible.
     * <li>Temporarily reserve those devices, if this fails then an
     * <code>HPermissionDeniedException</code> exception is thrown.
     * <li>Set the coherent configuration
     * <li>Make sure that all devices know about the changes (and notify any
     * listeners)
     * <li>Release all "temporary" reservations
     * </ol>
     * 
     * @param config
     *            the new desired configuration for <i>device</i> which
     *            conflicts with known configurations for other devices
     * 
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * 
     * @throws HPermissionDeniedException
     *             if necessary <i>temporary</i> reservations could not be made
     */
    boolean setWithCoherentConfigurations(HScreenConfiguration config) throws HPermissionDeniedException,
            HConfigurationException
    {
        // Find a suitable coherent configuration
        return setWithCoherentConfigurations(new HScreenConfiguration[] { config });
    }

    /**
     * Attempts to find and set a coherent configuration that includes the given
     * configurations and has as little impact on other devices as possible.
     * <p>
     * This should be called while holding the global <code>HDScreen.lock</code>.
     * <p>
     * This operation is composed of the following steps:
     * <ol>
     * <li>Find a coherent configuration that is suitable.
     * <li>Determine which device/configurations are incompatible.
     * <li>Temporarily reserve those devices if they weren't indicated by the
     * initial set of configurations; if this fails then an
     * <code>HPermissionDeniedException</code> exception is thrown.
     * <li>Set the coherent configuration
     * <li>Make sure that all devices know about the changes (and notify any
     * listeners)
     * <li>Release all "temporary" reservations
     * </ol>
     * 
     * @param hsca
     *            the new desired configurations for their associated devcies,
     *            which may conflict with known configurations for other devices
     * 
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * 
     * @throws HPermissionDeniedException
     *             if necessary <i>temporary</i> reservations could not be made
     */
    private boolean setWithCoherentConfigurations(HScreenConfiguration[] hsca) throws HPermissionDeniedException,
            HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setWithCoherentConfigurations: entering...hsca.length = " + hsca.length);
        }

        // Determine the desired devices
        Hashtable devices = new Hashtable();
        for (int i = 0; i < hsca.length; ++i)
        {
            HScreenDevice device = ((HDScreenConfiguration) hsca[i]).getScreenDevice();
            if (log.isDebugEnabled())
            {
                log.debug("setWithCoherentConfigurations: adding device " + device);
            }

            devices.put(device, device);
        }

        // Find a suitable coherent configuration
        HScreenConfigTemplate[] templates = desiredTemplates(hsca, devices);
        HDCoherentConfig set = getNativeCoherentScreenConfigurations(templates);

        return selectCoherentConfig(devices, set);
    }

    private boolean selectCoherentConfig(Hashtable devices, HDCoherentConfig set) throws HPermissionDeniedException,
            HConfigurationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Inside HDScreen::selectCoherentConfig...set = " + set);
        }

        // If no matching configuration is found, return false
        if (set == null) 
        {
            if (log.isDebugEnabled())
            {
                log.debug("+(ds-scr-s) selectCoherentConfig: empty set of configurations supplied, doing nothing");
            }
        	return false;
        }
        	
        HScreenConfiguration[] screenConfigs = set.getConfigurations(false); // include
                                                                             // non-contrib

        // "Reserve" all incompatible devices
        // (the ones that aren't reserved cannot be changed anyhow,
        // because of the screen lock)
        boolean ok = false;
        Vector free = new Vector();
        try
        {
            // Potential reservations are those in configs-hsca.
            // Or better yet, those whose device aren't in devices

            for (int i = 0; i < screenConfigs.length; ++i)
            {
                HScreenDevice dev = ((HDScreenConfiguration) screenConfigs[i]).getScreenDevice();
                if (log.isDebugEnabled())
                {
                    log.debug("Inside HDScreen::screenConfigs[" + i + "] = " + screenConfigs[i] + ", dev = " + dev);
                }

                if (log.isDebugEnabled())
                {
                    log.debug("+(ds-scr-s) selectCoherentConfig: processing HScreenDevice[" + i +
                            "]: " + dev.getIDstring());
                }
                // If not a configuration that should already be reserved
                // and the change would impact the current configuration...
                if (devices.get(dev) == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("devices.get(dev) == null");
                    }
                }
                
                if (set.wouldImpact(dev))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("set.wouldImpact(dev)");
                    }
                }

                if (devices.get(dev) == null && set.wouldImpact(dev))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("+(ds-scr-s)     reserving HScreenDevice[" + i + "]: " + dev.getIDstring());
                    }
                    // Temporarily reserve the device, and remember...
                    // if we need to release that reservation...
                    if (((HDScreenDevice) dev).tempReserveDevice())
                    {
                        free.addElement(dev);
                    }
                }
            }
            // Set the coherent configuration, by selecting it into the screen!
            ok = set.select(nScreen, devices);
        }
        finally
        {
            // Finally, release all temporary reservations
            for (int i = 0; i < free.size(); ++i)
            {
                HScreenDevice dev = (HScreenDevice) free.elementAt(i);
                dev.releaseDevice();
            }
        }

        return ok;
    }

    /**
     * Creates an HVideoConfiguration suitable for use as
     * {@link org.havi.ui.HVideoDevice#NOT_CONTRIBUTING}; or <code>null</code>
     * if no such things is required.
     */
    static HVideoConfiguration getVideoNotContributing()
    {
        int notContrib = nGetVideoNotContributing();
        if (notContrib == 0) return null;

        return new HDVideoConfiguration(null, notContrib);
    }

    /**
     * Create a set of templates used to find a coherent set of configurations
     * for all devices on this screen. This is performed essentially by creating
     * suitable templates and searching for the desired configuration. Suitable
     * templates are:
     * <ul>
     * <li>The exact template for the desired configuration.
     * <li>The templates for all other devices current configurations with
     * <code>REQUIRED</code> and <code>REQUIRED_NOT</code> changed to
     * <code>PREFERRED</code> and <code>PREFERRED_NOT</code>.
     * </ul>
     * 
     * @param hsca
     *            the configurations to be used
     * @param devices
     *            the devices represented by the configurations
     */
    private HScreenConfigTemplate[] desiredTemplates(HScreenConfiguration[] hsca, Hashtable devices)
    {
        HScreenConfigTemplate[] templates = new HScreenConfigTemplate[graphicsDevices.length + videoDevices.length
                + backgroundDevices.length];

        // First, generate a "hard" template for each given config
        int idx = 0;
        for (int i = 0; i < hsca.length; ++i)
        {
            templates[idx++] = ((HDScreenConfiguration) hsca[i]).getHardConfigTemplate();
        }
        // If hsca covered every device, return immediately
        if (idx >= templates.length) return templates;

        // Now, generate "soft" templates for current configs for other devices
        for (int i = 0; i < graphicsDevices.length; ++i)
        {
            if (devices.get(graphicsDevices[i]) == null)
            {
                templates[idx++] = ((HDScreenConfiguration) graphicsDevices[i].getCurrentConfiguration()).getSoftConfigTemplate();
                // If we have templates for every device already, return
                if (idx >= templates.length) return templates;
            }
        }
        for (int i = 0; i < backgroundDevices.length; ++i)
        {
            if (devices.get(backgroundDevices[i]) == null)
            {
                templates[idx++] = ((HDScreenConfiguration) backgroundDevices[i].getCurrentConfiguration()).getSoftConfigTemplate();
                // If we have templates for every device already, return
                if (idx >= templates.length) return templates;
            }
        }
        for (int i = 0; i < videoDevices.length; ++i)
        {
            if (devices.get(videoDevices[i]) == null)
            {
                templates[idx++] = ((HDScreenConfiguration) videoDevices[i].getCurrentConfiguration()).getSoftConfigTemplate();
                // If we have templates for every device already, return
                if (idx >= templates.length) return templates;
            }
        }

        return templates;
    }

    /**
     * Returns whether selecting the given configuration into its associated
     * device would affect the other devices (that are not equal to the
     * associated device).
     * 
     * @param config
     *            the configuration to test for impact
     * @param devices
     *            the devices to test for impact by selecting the given config
     * @return a score specifying the amount of impact
     */
    private byte getImpact(HScreenConfiguration config, HScreenDevice[] devices)
    {
        // Get the associated device
        HScreenDevice device = ((HDScreenConfiguration) config).getScreenDevice();
        int dev = ((NativeHandle) device).getHandle();
        int cfg = ((NativeHandle) config).getHandle();

        int score = 0;
        int increment = (Byte.MAX_VALUE < devices.length) ? 1 : (Byte.MAX_VALUE / devices.length);
        for (int i = 0; i < devices.length; ++i)
        {
            if (wouldImpact(dev, cfg, devices[i])) score += increment;

            /*
             * If there is an impact, we could be a bit more exact... - Find the
             * coherent configuration that would be required. - Determine a
             * score based on the differences between the current config and the
             * selected configuration.
             */
        }
        return (byte) Math.min(score, Byte.MAX_VALUE);
    }

    public MediaAPI getMediaAPI()
    {
        if (mediaAPI == null)
        {
            mediaAPI = (MediaAPI) ManagerManager.getInstance(MediaAPIManager.class);
        }

        return mediaAPI;
    }

    /**
     * Returns whether the two pairs of device/configurations are incompatible
     * or not. Essentially determining if selecting configuration <i>cfg</i>
     * into <i>dev</i> would affect <i>device2</i>.
     * 
     * @param dev
     *            native device handle representing device to be changed
     * @param cfg
     *            native configuration handle representing new configuration
     * @param device2
     *            device to compare against
     */
    private boolean wouldImpact(int dev, int cfg, HScreenDevice device2)
    {
        HScreenConfiguration config2 = ((HDScreenDevice) device2).getScreenConfig();

        int dev2 = ((NativeHandle) device2).getHandle();
        int cfg2 = ((NativeHandle) config2).getHandle();

        return dev != dev2 && cfg != cfg2 && nWouldImpact(dev, cfg, dev2, cfg2);
    }

    private static final int TYPE_GFX = 1;

    private static final int TYPE_BG = 2;

    private static final int TYPE_VID = 3;

    /**
     * Perform any necessary JNI initialization.
     */
    private native static void nInit();

    /**
     * Returns a new array which contains <code>int</code>s representing the
     * native screen handles for the system.
     * 
     * @return a new array which contains <code>int</code>s representing the
     *         native screen handles for the system.
     */
    private native static int[] nGetScreens();

    /**
     * Returns a new array which contains <code>int</code>s representing the
     * native device handles for the given screen of the given type
     * 
     * @param screen
     *            the screen
     * @param type
     *            device type; one of {@link #TYPE_GFX}, {@link #TYPE_BG},
     *            {@link #TYPE_VID}
     * @return a new array which contains <code>int</code>s representing the
     *         native device handles for the given screen of the given type
     */
    private native static int[] nGetDevices(int screen, int type);

    /**
     * Returns the id string for the given device. Called by
     * <code>HDGraphicsDevice</code>, <code>HDVideoDevice</code>,
     * <code>HDBackgroundDevice</code>.
     * 
     * @param device
     *            the native device handle
     */
    native static String nGetDeviceIdString(int device);

    /**
     * Returns the screen aspect ratio of the given device. Records the screen
     * aspect ratio in the given <code>Dimension</code> object and returns the
     * same <code>Dimension</code> object.
     * 
     * @param device
     *            the native device handle
     * @param dim
     *            the dimension object to fill with the screen aspect ratio
     * @return the <code>Dimension</code> object that was passed in as
     *         <i>dim</i>
     */
    native static Dimension nGetDeviceScreenAspectRatio(int device, Dimension dim);

    /**
     * Returns the destination of the video device.
     * 
     * @param device
     *            the native video device handle. Only video devices support
     *            this.
     * @return the destination DISPLAY_DEST_TV or DISPLAY_DEST_VIDEO (PIP)
     */
    native static int nGetDeviceDest(int videoDevice);

    /**
     * Returns a new array which contains <code>int</code>s representing the
     * native configuration handles for the given device.
     * 
     * @param device
     *            the device
     * @return a new array which contains <code>int</code>s representing the
     *         native configuration handles for the given device
     */
    native static int[] nGetDeviceConfigs(int device);

    /**
     * Attempts to set the current configuration for <i>device</i> to
     * <i>config</i>.
     * 
     * @param device
     *            native device handle
     * @param config
     *            native configuration handle
     * @return <code>true</code> if the operation failed because it would
     *         conflict with another device's configuration; <code>false</code>
     *         if the operation was successful
     */
    native static boolean nSetDeviceConfig(int device, int config);

    /**
     * Returns the current configuration set on the given device.
     * 
     * @param device
     *            native device handle
     * @return native device handle representing the current configuration for
     *         the given device
     */
    native static int nGetDeviceConfig(int device);

    /**
     * Sets the background device's single color, replacing any previous color
     * or displayed background image. Should only ever be used for background
     * devices.
     * 
     * @param device
     *            native bg device
     * @param rgb
     *            RGB888 color
     * @return 0 for success, 1 for unsupported operation, 2 for illegal color
     */
    native static int nSetDeviceBGColor(int device, int rgb);

    /**
     * Retrieves the currently set background color. Should only ever be used
     * for background devices.
     * 
     * @param device
     *            native bg device
     * @return currently set background color (may be different from that passed
     *         to {@link #nSetDeviceBGColor}) as RGB888
     */
    static native int nGetDeviceBGColor(int device);

    /**
     * Returns whether the two pairs of device/configurations are incompatible
     * or not.
     * 
     * @param device
     *            native device handle representing device to be changed
     * @param config
     *            native configuration handle representing new configuration
     * @param device2
     *            native device handle for other device
     * @param config2
     *            native configuration handle for current configuration of
     *            <i>device2</i>
     */
    private native static boolean nWouldImpact(int device, int config, int device2, int config2);

    /**
     * Returns the native coherent configuration handles supported by the given
     * screen.
     * 
     * @param nScreen
     *            native screen handle
     */
    private native static int[] nGetCoherentConfigs(int nScreen);

    /**
     * Returns the "not contributing" video configuration (which is used when
     * background stills are enabled), if there is one. If there isn't one, zero
     * is returned.
     * 
     * @return the "not contributing" video configuration or zero
     */
    private native static int nGetVideoNotContributing();

    /**
     * 
     * Set the default DFC value. This is used when DSExt is in place. This is
     * set to the configuration DFC that is used whenever the application DFC is
     * set to platform.
     * 
     * If this value is set to {@link VideoFormatControl.DFC_PROCESSING_UNKNOWN}
     * then the value is not used by MPEOS (default for non-DSExt
     * implemenations).
     * 
     * Placed here rather then in MediaAPI because this should not be used
     * througout the code.
     * 
     * @param nDevice
     * @param dfcAction
     * @return success or failure code
     * 
     */
    native static int nSetDefaultPlatformDFC(int nDevice, int dfcAction);

    /** Performs necessary JNI initialization. */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
