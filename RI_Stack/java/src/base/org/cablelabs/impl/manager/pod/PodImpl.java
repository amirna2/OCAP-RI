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

package org.cablelabs.impl.manager.pod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.ExtendedSystemEventManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hardware.pod.HostParamHandler;
import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.PODApplication;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.event.CableCARDResetEvent;
import org.ocap.system.event.DeferredDownloadEvent;
import org.ocap.system.event.SystemEventManager;

/**
 * This class provides an access to functions and information of the OpenCable
 * CableCARD device on the OCAP Host device. The following functions and
 * information are provided.
 * <ul>
 * <li>Get a list of all applications in the CableCARD device.
 * <li>Get Feature list supported by the Host.
 * <li>Get a manufacture ID and a version number of the CableCARD device.
 * <li>Get a current status of the CableCARD device.
 * <li>Update the Feature parameter in the Host.
 * <li>Reject updating of the Feature parameter in the Host.
 * </ul>
 */
public class PodImpl extends POD
{
    /**
     * A constructor of this class. An application must use the
     * {@link POD#getInstance} method to create an instance.
     */
    protected PodImpl(PODManager podmgr)
    {
        // Store the reference to the PODManager being used
        PodImpl.podmgr = podmgr;

        // Store a reference to ourseleves (for the static getInstance() method)
        PodImpl.podSingleton = this;

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        podEventHandler = new PODEventHandler(this);
        podmgr.initPOD(podEventHandler);

        updateGFCache();

        // Call native method to instigate retrieval of application information.
        if (podmgr.isPODReady())
        {
            updateJavaSystemProperties(true);
        }
        
        File podFeatureDir = new File(MPEEnv.getEnv("OCAP.persistent.podgf"));
        
        // Persisted POD Feature Values files
        podFeatureValuesFile = new File(podFeatureDir, "podFeatureValuesFile.dat");
        podFeatureValuesFileNew = new File(podFeatureDir, "podFeatureValuesFile.new");
        podFeatureValuesFileBak = new File(podFeatureDir, "podFeatureValuesFile.bak");
    }

    /**
     * This method returns the sole instance of the POD class. The POD instance
     * is either a singleton for each OCAP application or a singleton for an
     * entire OCAP implementation.
     * 
     * @return a singleton POD instance.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     * 
     */
    public static POD getInstance()
    {
        // Check with security manager to verify application has access to POD.
        SecurityUtil.checkPermission(new MonitorAppPermission("podApplication"));

        return podSingleton;
    }

    /**
     * This method provides a current status of the CableCARD device.
     * 
     * @return true if the CableCARD device has completed the booting process.
     */
    public boolean isReady()
    {
        // Call native method to verify pod is still available.
        return podmgr.isPODReady();
    }

    /**
     * This method returns a CableCARD device manufacturer ID.
     * 
     * @return a pod_manufacturer_id in the Application_info_cnf() APDU defined
     *         in the CableCARD Interface 2.0 Specification [4].
     * 
     * @throws IllegalStateException
     *             if the CableCARD is not ready, i.e., the {@link #isReady}
     *             method returns false.
     */
    public int getManufacturerID()
    {
        // Check for POD available...
        if (!isReady()) throw new IllegalStateException("CableCard device not available");

        String propValue;
        int manuf = -1;
        if ((propValue = MPEEnv.getEnv("OCAP.cablecard.manufacturer")) != null)
        {
            try
            {
                manuf = Integer.parseInt(propValue);
            }
            catch (NumberFormatException e)
            {
            }
        }
        if (manuf != -1) return manuf;

        // Call PODManager to acquire POD manufacturer identifier.
        return podmgr.getManufacturerID();
    }

    /**
     * This method returns a CableCARD device version number.
     * 
     * @return pod_version_number in the Application_info_cnf() APDU defined in
     *         the CableCARD Interface 2.0 Specification [4].
     * 
     * @throws IllegalStateException
     *             if the CableCARD is not ready, i.e., the {@link #isReady}
     *             method returns false.
     */
    public int getVersionNumber()
    {
        // Check for POD available...
        if (!isReady()) throw new IllegalStateException("CableCard device not available");

        String propValue;
        int version = -1;
        if ((propValue = MPEEnv.getEnv("OCAP.cablecard.version")) != null)
        {
            try
            {
                version = Integer.parseInt(propValue);
            }
            catch (NumberFormatException e)
            {
            }
        }
        if (version != -1) return version;

        // Call PODManager to acquire POD version number.
        return podmgr.getVersion();
    }

    /**
     * <p>
     * This method returns the CableCARD device applications listed in the
     * Application_info_cnf() APDU defined in the OpenCable CableCARD Interface
     * specification.
     * </p>
     * <p>
     * Note that the Host need not to send the Application_info_req APDU. It may
     * cache the information.
     * </p>
     * 
     * @return a list of CableCARD device applications in the CableCARD device.
     * 
     * @throws IllegalStateException
     *             if the CableCARD is not ready, i.e., the {@link #isReady}
     *             method returns false.
     */
    public PODApplication[] getApplications()
    {
        // Check for POD available...
        if (!isReady()) throw new IllegalStateException("CableCard device not available");

        return podmgr.getPODApplications();
    }

    /**
     * This method returns a list of the Feature IDs supported by the Host
     * device. Feature ID is defined in the OpenCable CableCARD Interface
     * specification.
     * 
     * @return a list of Feature IDs supported by the Host device.
     */
    public int[] getHostFeatureList()
    {
        int[] retVal = new int[genericFeatureCache.size()];

        // Return cached features list, which is a series of integers
        // representing the generic feature IDs per SCTE28 Table 8.12-B
        Enumeration e = genericFeatureCache.keys();

        for (int i = 0; e.hasMoreElements(); ++i)
        {
            Integer id = (Integer) (e.nextElement());
            retVal[i] = id.intValue();
        }

        return retVal;
    }

    /**
     * <p>
     * This method updates the Feature parameter value in the Host device. In
     * this method call, the {@link HostParamHandler#notifyUpdate} method shall
     * be called. The notifyUpdate() method may reject update of the Feature
     * parameter and also the Host device may reject it. The updated Feature
     * parameter shall be notified to the CableCARD device according to the
     * CableCARD Interface 2.0 Specification [4] after this method returns, but
     * this method doesn't confirm a successful notification to the CableCARD
     * device.
     * </p>
     * <p>
     * The Feature ID and Feature parameter format is defined in the CableCARD
     * Interface 2.0 Specification [4]. See also the
     * {@link org.ocap.hardware.pod.HostParamHandler} for more information.
     * </p>
     * <p>
     * Note that the {@link org.ocap.hardware.pod.HostParamHandler#notifyUpdate}
     * method shall be called before the Feature parameter is updated by this
     * method call.
     * </p>
     * 
     * @see org.ocap.hardware.pod.HostParamHandler
     * 
     * @param featureID
     *            a Feature ID to be updated. Feature ID is defined in the
     *            CableCARD Interface 2.0 Specification [4]. The Feature ID
     *            reserved for proprietary use (0x70 - 0xFF) can be specified.
     * 
     * @param value
     *            a new Feature parameter value for the specified featureID. An
     *            actual format of each Feature parameter is defiend in the
     *            CableCARD Interface 2.0 Specification [4]. For example, if the
     *            featureID is 0x1, the value is <br>
     *            <code>
     *                Rf_output_channel() {<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel_ui<br>
     *                }<br>
     *                </code>
     * 
     * @return <code>true</code> if update was successful. <code>false</code> if
     *         rejected by the Host.
     * 
     * @throws IllegalArgumentException
     *             if the specified featureID is not in a range of 0 <=
     *             featureID <= 0xFF, or the value is null.
     */
    public boolean updateHostParam(int featureID, byte[] value)
    {
        // Validate input parameters (must be 0 <= featureId <= 0xFF).
        if ((featureID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("feature identifier is out of range");

        if (value == null) throw new IllegalArgumentException("value byte array is null");

        if (!isReady()) return false;

        if (!isFeatureSupported(featureID)) return false;

        // If handler monitoring changes approves of the proposed new parameter
        // value,
        // call native interface to update the value.
        if (updateHandler(featureID, value))
        {
            // Call native interface to perform parameter update.
            if (podmgr.updatePODHostParam(featureID, value))
            {
                synchronized (lock)
                {
                    genericFeatureCache.put(new Integer(featureID), new GenericFeatureValue(value));
                }

                return true; // success
            }
        }

        return false; // Unsupported feature or rejected parameter change or
                      // failed
        // to update POD via native interface
    }

    public void addPODListener(PODListener listener)
    {
        synchronized (lock)
        {
            // CallerContextManager ccm = (CallerContextManager)
            // ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cc = ccm.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = EventMulticaster.add(data.listeners, listener);
        }
    }

    public void removePODListener(PODListener listener)
    {
        synchronized (lock)
        {
            // CallerContextManager ccm = (CallerContextManager)
            // ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cc = ccm.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = EventMulticaster.remove(data.listeners, listener);
        }
    }

    /**
     * This method returns the current Feature parameter value in the Host
     * device for the specified Feature ID. The Feature ID and Feature parameter
     * format is defined in the CableCARD Interface 2.0 Specification [4]. See
     * also the {@link org.ocap.hardware.pod.HostParamHandler} for more
     * information.
     * 
     * @see org.ocap.hardware.pod.HostParamHandler
     * 
     * @param featureID
     *            a Feature ID defined in the CableCARD Interface 2.0
     *            Specification [4]. The Feature ID reserved for proprietary use
     *            (0x70 - 0xFF) can be specified.
     * 
     * @return a current Feature parameter value for the specified featureID.
     *         For example, if the featureID is 0x1, the value is <br>
     *         <code>
     *                Rf_output_channel() {<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel<br>
     *                &nbsp;&nbsp;&nbsp;&nbsp;Output_channel_ui<br>
     *                }<br>
     *                </code> An array of length zero, if the specified
     *         featureID is not supported.
     * 
     * @throws IllegalArgumentException
     *             if the specified featureID is not in a range of 0 <=
     *             featureID <= 0xFF.
     */
    public byte[] getHostParam(int featureID)
    {
        // Validate input parameter (must be 0 <= featureId <= 0xFF).
        if ((featureID < 0) || (featureID > 255))
            throw new IllegalArgumentException("feature identifier is out of range");

        if (!isFeatureSupported(featureID)) return new byte[0];

        // Get the feature's value from the cache
        byte[] retVal;
        synchronized (lock)
        {
            GenericFeatureValue gfv = (GenericFeatureValue) (genericFeatureCache.get(new Integer(featureID)));
            retVal = new byte[gfv.value.length];
            System.arraycopy(gfv.value, 0, retVal, 0, gfv.value.length);
        }

        return retVal;
    }

    /**
     * <p>
     * This is the method called when an application or POD wishes to update a
     * specific parameter of one of the features supported by the Host/POD. It
     * calls the registered "handler" to notify it of the request to make a
     * change in one of the parameter values. This allows the registered handler
     * to reject changes instigated by the POD or application. The installed
     * handler's "update" method will be called within the associated
     * application's context on the application's thread. Hence, the result of
     * the update must be saved in a class variable such that it can be returned
     * back to the native caller. If the result of the update if "false" then
     * the update has been rejected by the handler and no change in the
     * parameter value should occur.
     * 
     * @param featureID
     *            is the generic feature ID per SCTE28 Table 8.12-B
     * @param value
     *            is the object value for the proposed update
     */
    private boolean updateHandler(final int featureID, final byte[] value)
    {
        final HostParamHandler handler; // Reference to installed handler.
        final CallerContext ctx; // Installed handler's context.

        // Synchronize access to installed handler and its context.
        synchronized (lock)
        {
            handler = hostParamHandler; // Get current handler.
            ctx = hostParamHandlerContext; // Get associated context.
            updateResponse = true; // Set response when no handler installed.
        }

        // Make sure there's a handler.
        if ((handler != null) && (ctx != null))
        {
            // Synchronously call handler in its context on its thread.
            if (!CallerContext.Util.doRunInContextSync(ctx, new Runnable()
            {
                public void run()
                {
                    // Invoke the handler within its own context.
                    updateResponse = handler.notifyUpdate(featureID, value);
                }
            })) return false;
        }

        // Return the result of the update notification.
        return updateResponse;
    }

    /**
     * This method sets an instance of a class that implements the
     * HostParamHandler interface. Only one instance of such class can be set to
     * the OCAP system. Multiple calls of this method replace the previous
     * instance by a new one. By default, no HostParamHandler is set, i.e., all
     * update of Feature parameter is decided by the Host device.
     * 
     * @param handler
     *            an instance of a class that implements the HostParamHandler.
     *            if <code>null</code> is specified, the current
     *            HostParamHandler is removed.
     * 
     * @see org.ocap.hardware.pod.HostParamHandler
     */
    public void setHostParamHandler(HostParamHandler handler)
    {
        CallerContext cc = ccm.getCurrentContext(); // Get caller's context.

        // Check for caller unregistering a handler, if so re-install default
        // handler.
        if (null == handler)
        {
            cc = null; // Clear context when clearing handler.
        }

        // Synchronize setting of handler information.
        synchronized (lock)
        {
        	//findbugs complains about "write to static field from instance" - ok, here, based on synchronized block.
            hostParamHandler = handler; // Set new handler.
            hostParamHandlerContext = cc; // Remember the caller context the
                                          // "update" method.
        }

        return;
    }

    private boolean isFeatureSupported(int featureID)
    {
        return genericFeatureCache.containsKey(new Integer(featureID));
    }

    private synchronized void updateGFCache()
    {
        // Empty the previous cache
        genericFeatureCache = new Hashtable();

        // If the POD is ready, then update the cache from POD.
        // If the POD is not ready, then update the cache from the persisted
        // file values.
        if (podmgr.isPODReady())
        {
            // Get the supported POD features
            int[] featureList = podmgr.getPODHostFeatures();

            // Query the value for each feature and update the cache
            if (featureList != null)
            {
                // Allocate data structures for the feature values
                for (int i = 0; i < featureList.length; ++i)
                {
                    // Retrieve the feature value from native
                    byte[] value = podmgr.getPODHostParam(featureList[i]);

                    // If we get a null value back, we should be returning a
                    // 0-length
                    // array to anyone who asks for this feature
                    GenericFeatureValue gfv = (value == null) ? new GenericFeatureValue(new byte[0])
                            : new GenericFeatureValue(value);

                    genericFeatureCache.put(new Integer(featureList[i]), gfv);
                }

                // save off updated feature values
                savePersistedGFCache();
            }
        }
        else
        {
            // Restore the generic features cache based on the values persisted
            // from
            // the last time a POD was inserted & ready.
            SavedPODFeature[] savedFeatureList = restorePersistedGFCache();
            if (savedFeatureList != null)
            {
                // Allocate data structures for the feature values
                for (int i = 0; i < savedFeatureList.length; ++i)
                {
                    // Retrieve the feature value from native
                    byte[] value = savedFeatureList[i].getData();

                    // If we get a null value back, we should be returning a
                    // 0-length
                    // array to anyone who asks for this feature
                    GenericFeatureValue gfv = (value == null) ? new GenericFeatureValue(new byte[0])
                            : new GenericFeatureValue(value);

                    genericFeatureCache.put(new Integer(savedFeatureList[i].getFeatureID()), gfv);
                }
            }
        }

        return;
    }

    /*
     * Persist the feature values required by the OpenCable HOST 2.0 spec: - RF
     * Output Channel - Parental Control PIN (if supported) - Parental Control
     * Settings (if supported) - Timezone - Daylight Savings Control - AC Outlet
     * (if supported) - Language (if supported) - EAS Location Code (if
     * supported)
     * 
     * Use double files to guard against a power-outage in the middle of
     * updating the file.
     * 
     * NOTE: Whenever this code is changed, watch out for
     * backwards-compatibility!
     */
    private void savePersistedGFCache()
    {
        if (log.isDebugEnabled())
        {
            log.debug("saving Persisted POD Feature Values to file " + podFeatureValuesFile.getPath());
        }

        synchronized (podFeatureValuesFile)
        {
            // open persistent POD feature values file for writing (even if it
            // previously existed)
            ObjectOutputStream oos = null;
            try
            {
                oos = new ObjectOutputStream(new FileOutputStream(podFeatureValuesFileNew.getPath()));
            }
            catch (Exception e1)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception opening persistent POD Feature Values file - " + e1.getMessage());
                }

                // error opening file,
                // so try deleting this "bad" file try again

                try
                {
                    podFeatureValuesFile.delete();
                }
                catch (Exception e1a)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("exception deleteing persistent POD Feature Values file - " + e1a.getMessage());
                    }

                    // ignore any exceptions deleting the file
                    // (worst case - we try doing this again later)
                }

                try
                {
                    oos = new ObjectOutputStream(new FileOutputStream(podFeatureValuesFile.getPath()));
                }
                catch (Exception e1b)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("exception re-opening the persistent POD Feature Values file - " + e1b.getMessage());
                    }

                    // just return - we tried our best to open the file
                    return;
                }
            }

            // create array of POD features values to be persisted
            SavedPODFeature[] podFeatureList = new SavedPODFeature[] {
                    new SavedPODFeature(GF_RF_OUTPUT_CHANNEL, getHostParam(GF_RF_OUTPUT_CHANNEL)),
                    new SavedPODFeature(GF_P_C_PIN, getHostParam(GF_P_C_PIN)),
                    new SavedPODFeature(GF_P_C_SETTINGS, getHostParam(GF_P_C_SETTINGS)),
                    new SavedPODFeature(GF_TIME_ZONE, getHostParam(GF_TIME_ZONE)),
                    new SavedPODFeature(GF_DAYLIGHT_SAVINGS, getHostParam(GF_DAYLIGHT_SAVINGS)),
                    new SavedPODFeature(GF_AC_OUTLET, getHostParam(GF_AC_OUTLET)),
                    new SavedPODFeature(GF_LANGUAGE, getHostParam(GF_LANGUAGE)),
                    new SavedPODFeature(GF_EAS_LOCATION, getHostParam(GF_EAS_LOCATION)),
                    new SavedPODFeature(GF_VCT_ID, getHostParam(GF_VCT_ID)) };

            // save off the POD feature values to be persisted
            try
            {
                oos.writeObject(podFeatureList);
            }
            catch (Exception e2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception writing to the persistent POD Feature Values file - " + e2.getMessage());
                }
            }

            // close the file (we're done writing to it)
            try
            {
                oos.close();
            }
            catch (Exception e3)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception closing the persistent POD Feature Values file - " + e3.getMessage());
                }
            }

            // rename this new file to be the "real" file
            try
            {
                // then rename the current file to be the backup file
                podFeatureValuesFileBak.delete();
                podFeatureValuesFile.renameTo(podFeatureValuesFileBak);

                // then rename the new file to be the current file
                podFeatureValuesFileNew.renameTo(podFeatureValuesFile);

                // then delete the backup file
                podFeatureValuesFileBak.delete();
            }
            catch (Exception e4)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception renaming the new persistent POD Feature Values file - " + e4.getMessage());
                }
        }
        }

        return;
    }

    /*
     * Retrieve the persisted feature values (persisted in persistGFCache())
     * 
     * NOTE: Whenever this code is changed, watch out for
     * backwards-compatibility!
     */
    private SavedPODFeature[] restorePersistedGFCache()
    {
        SavedPODFeature[] podFeatureList = null;

        if (log.isDebugEnabled())
        {
            log.debug("restoring Persisted POD Feature Values from file " + podFeatureValuesFile.getPath());
        }

        synchronized (podFeatureValuesFile)
        {
            // open POD feature persistent feature values file for writing
            ObjectInputStream ois = null;
            try
            {
                ois = new ObjectInputStream(new FileInputStream(podFeatureValuesFile.getPath()));
            }
            catch (Exception e1)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception opening persistent POD Feature Values file - " + e1.getMessage());
                }

                // error opening file, so try the backup file
                try
                {
                    ois = new ObjectInputStream(new FileInputStream(podFeatureValuesFileBak.getPath()));
                }
                catch (Exception e1a)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("exception opening backup persistent POD Feature Values file - " + e1a.getMessage());
                    }

                    // error opening file, so try the new file
                    try
                    {
                        ois = new ObjectInputStream(new FileInputStream(podFeatureValuesFileNew.getPath()));
                    }
                    catch (Exception e1aa)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("exception opening new persistent POD Feature Values file - " + e1aa.getMessage());
                        }

                        // errors trying to open all backup files,
                        // so all we can do now is return without updating GF
                        // database
                        return null;
                    }

                    // error opening backup file,
                    // so all we can do is return without updating GF database
                    return null;
                }
            }

            // read in persisted POD feature values
            try
            {
                podFeatureList = (SavedPODFeature[]) ois.readObject();
            }
            catch (Exception e2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception reading from the persistent POD Feature Values file - " + e2.getMessage());
                }
            }

            // close the file (we're done writing to it)
            try
            {
                ois.close();
            }
            catch (Exception e3)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("exception closing the persistent POD Feature Values file - " + e3.getMessage());
                }
        }
        }

        return podFeatureList;
    }

    private void updateAppCache()
    {
        // Not caching applications at this time
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
        synchronized (lock)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(lock);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, lock);
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
         * registered to be notified of POD events.
         */
        public volatile PODListener listeners;

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
            synchronized (lock)
            {
                // Remove this caller context from the list then throw away
                // the CCData for it.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(lock);
                listeners = null;
            }
        }
    }

    /**
     * Notifies PODListener
     */
    private void notifyListeners(final int event)
    {

        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    CallerContext cc = ccm.getCurrentContext();
                    CCData data = getCCData(cc);
                    if ((data != null) && (data.listeners != null))
                    {
                        data.listeners.notify(new PODEvent(event));
                    }
                }
            });
        }
    }

    // ****************************** Private Variables
    // **************************
    private volatile CallerContext ccList;

    // Implementation singleton
    private static PodImpl podSingleton;

    // Reference to the PODManager being used
    private static PODManager podmgr;

    // Define the variable for keeping a reference to the install
    // HostParamHandler.
    private static HostParamHandler hostParamHandler = null;

    // Define a private CallerContext reference for remembering the caller
    // context
    // of the application installed as the host parameter change handler.
    private static CallerContext hostParamHandlerContext = null;

    // Define variable for saving the response from HostParamHandler update.
    protected static boolean updateResponse;

    // General purpose lock.
    // Private lock used to avoid using <code>this</code> for synchronization.
    private Object lock = new Object();

    private PODEventHandler podEventHandler;

    private Hashtable genericFeatureCache = new Hashtable();

    private static final Logger log = Logger.getLogger(POD.class.getName());

    /*
     * This is a wrapper class that will allow us to store generic feature
     * values (byte[]) in a Hashtable
     */
    class GenericFeatureValue
    {
        public GenericFeatureValue(byte[] value)
        {
            this.value = value;
        }

        public byte[] value = new byte[0];
    }

    // Singleton instance of the CallerContextManager.
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    // Persisted POD Feature Values files
    File podFeatureValuesFile = new File(MPEEnv.getEnv("OCAP.persistent.userprefs"), "podFeatureValuesFile.dat");

    File podFeatureValuesFileNew = new File(MPEEnv.getEnv("OCAP.persistent.userprefs"), "podFeatureValuesFile.new");

    File podFeatureValuesFileBak = new File(MPEEnv.getEnv("OCAP.persistent.userprefs"), "podFeatureValuesFile.bak");

    // Update POD-related Java system properties
    private void updateJavaSystemProperties(boolean podIsReady)
    {
        if (podIsReady)
        {
            try
            {
                String propValue;

                // CableCARD Manufacturer ID
                Integer manuf = new Integer(getManufacturerID());
                System.setProperty("ocap.cablecard.manufacturer", manuf.toString());

                // CableCARD Version
                Integer version = new Integer(getVersionNumber());
                System.setProperty("ocap.cablecard.version", version.toString());

                // CableCARD ID
                if ((propValue = MPEEnv.getEnv("OCAP.cablecard.identifier")) != null)
                {
                    System.setProperty("ocap.cablecard.identifier", propValue);
                }

                // CableCARD VCT ID
                int vctid = -2;
                if ((propValue = MPEEnv.getEnv("OCAP.cablecard.vct-id")) != null)
                {
                    try
                    {
                        vctid = Integer.parseInt(propValue);
                        System.setProperty("ocap.cablecard.vct-id", propValue);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
                if (vctid == -2)
                {
                    byte[] vct_id_bytes = getHostParam(GF_VCT_ID);
                    if (vct_id_bytes.length > 0)
                    {
                        Short vct_id = new Short((short) ((vct_id_bytes[0] << 8) & vct_id_bytes[1]));
                        System.setProperty("ocap.cablecard.vct-id", vct_id.toString());
                    }
                    else
                        System.setProperty("ocap.cablecard.vct-id", "-1");
                }

                return;
            }
            catch (IllegalStateException e)
            {
                // POD is not ready now
            }
        }

        // POD is not ready or became not ready while setting properties
        System.setProperty("ocap.cablecard.manufacturer", "");
        System.setProperty("ocap.cablecard.version", "");
        System.setProperty("ocap.cablecard.identifier", "");
        System.setProperty("ocap.cablecard.vct-id", "");
    }

    /**
     * Inner class for handling cache-update events from native
     */
    class PODEventHandler implements EDListener
    {
        public PODEventHandler(PodImpl pod)
        {
            this.pod = pod;
        }

        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            SystemEventManager sem = (SystemEventManager) SystemEventManager.getInstance();
            switch (eventCode)
            {
                case PODEvent.EventID.POD_EVENT_GENERIC_FEATURE_UPDATE:
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_GENERIC_FEATURE_UPDATE event...");
                    }
                    pod.updateGFCache();
                    break;
                case PODEvent.EventID.POD_EVENT_APP_INFO_UPDATE:
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_APP_INFO_UPDATE event...");
                    }
                    pod.updateAppCache();
                    break;
                case PODEvent.EventID.POD_EVENT_POD_INSERTED:
                    // Call native method to instigate retrieval of application
                    // information.
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_POD_INSERTED event...");
                    }
                    pod.updateGFCache();
                    updateJavaSystemProperties(false);
                    notifyListeners(PODEvent.EventID.POD_EVENT_POD_INSERTED);
                    break;
                case PODEvent.EventID.POD_EVENT_POD_READY:
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_POD_READY event...");
                    }
                    pod.updateGFCache();
                    updateJavaSystemProperties(true);
                    notifyListeners(PODEvent.EventID.POD_EVENT_POD_READY);
                                        
                    if(resetReceived)
                    {
                        // Send a CableCARDResetEvent System Event
                        sem.log(new CableCARDResetEvent(CableCARDResetEvent.CABLECARD_RESET_COMPLETE));
                    }
                    
                    break;
                case PODEvent.EventID.POD_EVENT_POD_REMOVED:
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_POD_REMOVED event...");
                    }
                    updateJavaSystemProperties(false);
                    notifyListeners(PODEvent.EventID.POD_EVENT_POD_REMOVED);
                    break;
                case PODEvent.EventID.POD_EVENT_RESET_PENDING:
                    if (log.isDebugEnabled())
                    {
                        log.debug("POD_EVENT_RESET_PENDING event...");
                    }
                    notifyListeners(PODEvent.EventID.POD_EVENT_RESET_PENDING);                    

                    resetReceived = true;
                    // Send a CableCARDResetEvent System Event
                    sem.log(new CableCARDResetEvent(CableCARDResetEvent.CABLECARD_RESET_BEGIN));
                    break;
	        case PODEvent.EventID.POD_EVENT_SEND_APDU_FAILURE:
	        case PODEvent.EventID.POD_EVENT_RECV_APDU:
                    // NOTE: SystemModuleMgr runs a thread that
                    // receives and processes APDUs using a blocking
                    // receive call, so nothing needs to be done here
                    // for these events.
                    break;
                default:
                    if (log.isDebugEnabled())
                    {
                        log.debug("PodImpl -- unexpected Event, code=" + eventCode);
                    }
                    break;
            }
        }

        private PodImpl pod = null;
    }

    private boolean resetReceived = false;
    
    // Generic feature IDs from SCTE28 Table 8.12-B.
    public static final int GF_RF_OUTPUT_CHANNEL = 0x01;

    public static final int GF_P_C_PIN = 0x02;

    public static final int GF_P_C_SETTINGS = 0x03;

    // public static final int GF_IPPV_PIN = 0x04;
    public static final int GF_TIME_ZONE = 0x05;

    public static final int GF_DAYLIGHT_SAVINGS = 0x06;

    public static final int GF_AC_OUTLET = 0x07;

    public static final int GF_LANGUAGE = 0x08;

    // public static final int GF_RATING_REGION = 0x09;
    // public static final int GF_RESET_P_C_PIN = 0x0A;
    // public static final int GF_CABLE_URLS = 0x0B;
    public static final int GF_EAS_LOCATION = 0x0C;

    public static final int GF_VCT_ID = 0x0D;
    // public static final int GF_TURN_ON_CHANNEL = 0x0E;
    // public static final int GF_TERM_ASSOC = 0x0F;
    // public static final int GF_DWNLD_GRP_ID = 0x10;
    // public static final int GF_ZIP_CODE = 0x11;
}

/**
 * Implementation of PODApplication interface.
 */
class PODAppImpl implements PODApplication
{
    int type;

    int versionNumber;

    String name;

    String url;

    /**
     * Constructor.
     * 
     * @param type
     *            <code>application_type</code>; one of PODApplication.TYPE_xxx
     * 
     * @param versionNumber
     *            16-bit value for <code>application_version_number</code> field
     * 
     * @param name
     *            String for <code>application_name</code> field
     * 
     * @param url
     *            String for <code>application_url</code> field
     **/
    public PODAppImpl(int type, int versionNumber, String name, String url)
    {
        this.type = type;
        this.versionNumber = versionNumber;
        this.name = name;
        this.url = url;
    }

    /**
     * returns the type of this application
     * 
     * @return application type; one of PODApplication.TYPE_xxx
     **/
    public int getType()
    {
        return type;
    }

    /**
     * returns the version number of this application
     **/
    public int getVersionNumber()
    {
        return versionNumber;
    }

    /**
     * returns the name of this application
     **/
    public String getName()
    {
        return name;
    }

    /**
     * returns the location of this application in URL
     **/
    public String getURL()
    {
        // Check with security manager to verify caller has MonAppPermission.
        SecurityUtil.checkPermission(new MonitorAppPermission("podApplication"));
        return url;
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}

/**
 * Class for persisting POD Feature Values
 */
class SavedPODFeature implements Serializable
{
    private int featureID;

    private byte[] data;

    public SavedPODFeature(int featureID, byte[] data)
    {
        this.featureID = featureID;
        this.data = data;
    }

    public int getFeatureID()
    {
        return this.featureID;
    }

    public byte[] getData()
    {
        return this.data;
    }
}
