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

package org.cablelabs.impl.storage;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SecurityUtil;
import org.dvb.application.AppID;
import org.ocap.dvr.storage.MediaStorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageProxy;

/**
 * The <code>MediaStorageOption</code> implementation.
 * 
 * @author Todd Earles
 */
public class MediaStorageOptionImpl implements MediaStorageOption
{
    private static final Logger log = Logger.getLogger(MediaStorageOptionImpl.class);

    private long playBackBandWidth;

    private long recordingBandWidth;

    private long totalMediaStorageCapacity;

    private boolean simultaneousPlayAndRecord;

    private boolean crossMsvTsbConversionSupported;

    // The storage proxy to which this option belongs
    private StorageProxy proxy;

    // Native storage handle
    private int nativeHandle;

    // Default MSV
    private MediaStorageVolume defaultMSV;

    /**
     * Constructs an instance of MediaStorageOptionImpl for the specified StorageProxy and caches any stable,
     * DVR-specific attributes of the StorageProxy retrieved from the native layer
     * 
     * @param proxy - Proxy to which the MSV should be Associated
     */
    public MediaStorageOptionImpl(StorageProxy proxy)
    {
        this.proxy = proxy;
        nativeHandle = ((DVRStorageProxyImpl) proxy).getNativeHandle();
        initializeCacheValues();
    }

    // Description copied from MediaStorageOption
    public MediaStorageVolume getDefaultRecordingVolume()
    {
        if (defaultMSV == null)
        {
            AppID defaultMSVAppid = new AppID(0, 0);

            LogicalStorageVolume[] avblLSVs = proxy.getVolumes();

            if (log.isDebugEnabled())
            {
                log.debug("Retrieved " + avblLSVs.length + " volumes");
            }
            for (int i = 0; i < avblLSVs.length; i++)
            {
                AppID appid = ((LogicalStorageVolumeImpl) avblLSVs[i]).getAppId();

                if (avblLSVs[i] instanceof MediaStorageVolume)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("  Found MSV at index " + i + " owned by app " + appid.toString());
                    }
                    // If appid and orgid is 0, then that MedisStorageVolume is
                    // DefaultRecordingVolume

                    if (appid.equals(defaultMSVAppid))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("  Found default MSV!");
                        }
                        defaultMSV = (MediaStorageVolume) avblLSVs[i];
                        break;
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("  Found LSV at index " + i + " owned by app " + appid.toString());
                    }
            }
            }

            // If we no longer have the implementation-created volume
            // The stack should return the 'first created' MSV 
            if (defaultMSV == null && avblLSVs.length != 0)
            {
                long createTime = -1;
                for (int i = 0; i < avblLSVs.length; i++)
                {
                    if (avblLSVs[i] instanceof MediaStorageVolumeImpl)
                    {
                        // If the implementation created MSV is not still around, then we determine
                        // the earliest created MSV
                        MediaStorageVolumeImpl msvi = (MediaStorageVolumeImpl)avblLSVs[i];
                        long thisCreateDate = msvi.getCreateDate();
                        if (createTime == -1 || thisCreateDate < createTime)
                        {
                            createTime = thisCreateDate;
                            defaultMSV = msvi;
                        }
                    }
                }
            }
        }
        return defaultMSV;
    }

    // Description copied from MediaStorageOption
    public long getPlaybackBandwidth()
    {
        return playBackBandWidth;
    }

    // Description copied from MediaStorageOption
    public long getRecordBandwidth()
    {
        return recordingBandWidth;
    }

    // Description copied from MediaStorageOption
    public long getTotalMediaStorageCapacity()
    {
        return totalMediaStorageCapacity;
    }

    // Description copied from MediaStorageOption
    public long getAllocatableMediaStorage()
    {
        return nGetAllocatableMediaStorage(nativeHandle);
    }

    // Description copied from MediaStorageOption
    public long getTotalGeneralStorageCapacity()
    {
        return (this.proxy.getTotalSpace() - getTotalMediaStorageCapacity());
    }

    // Description copied from MediaStorageOption
    public void initialize(long mediafsSize) throws IllegalArgumentException, IllegalStateException
    {
        if (getTotalGeneralStorageCapacity() + getTotalMediaStorageCapacity() < mediafsSize)
        {
            throw new IllegalArgumentException("Size passed in is larger than capacity");
        }
        try
        {
            nRepartition(nativeHandle, mediafsSize);
        }
        catch (UnsupportedOperationException e)
        {
            throw new IllegalStateException("Unable to repartition");
        }
    }

    // Description copied from MediaStorageOption
    public boolean simultaneousPlayAndRecord()
    {
        return simultaneousPlayAndRecord;
    }

    /**
     * 
     * @return
     */
    public boolean isCrossMsvTsbConversionSupported()
    {
        return crossMsvTsbConversionSupported;
    }

    // Description copied from MediaStorageOption
    public MediaStorageVolume allocateMediaVolume(String name, ExtendedFileAccessPermissions fap)
            throws IllegalArgumentException
    {
        if (!SecurityUtil.isSignedApp())
        {
            throw new SecurityException("caller app is not signed!");
        }
        AppID callerId = getCallerAppID();
        MediaStorageVolumeImpl newMsv = null;
        try
        {
            // To enfore the rules of MediaStorageVolume.removeAccess() and
            // allowAccess(),
            // we always store null org read/write access arrays with our EFAP
            // to indicate
            // that no other orgs have acesss 0-length arrays indicates that ALL
            // apps have
            // access
            int[] readOrgs = fap.getReadAccessOrganizationIds();
            int[] writeOrgs = fap.getWriteAccessOrganizationIds();
            boolean fixReadArray = (readOrgs != null && readOrgs.length == 0) ? true : false;
            boolean fixWriteArray = (writeOrgs != null && writeOrgs.length == 0) ? true : false;
            if (fixReadArray || fixWriteArray)
            {
                fap = new ExtendedFileAccessPermissions(fap.hasReadWorldAccessRight(), fap.hasWriteWorldAccessRight(),
                        fap.hasReadOrganisationAccessRight(), fap.hasWriteOrganisationAccessRight(),
                        fap.hasReadApplicationAccessRight(), fap.hasWriteApplicationAccessRight(),
                        (fixReadArray) ? null : fap.getReadAccessOrganizationIds(), (fixWriteArray) ? null
                                : fap.getWriteAccessOrganizationIds());
            }

            newMsv = new MediaStorageVolumeImpl((DVRStorageProxyImpl) proxy, name, callerId, fap);
            ((DVRStorageProxyImpl) proxy).addMediaVolume(newMsv);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return newMsv;
    }

    private AppID getCallerAppID()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cctx = ccm.getCurrentContext();
        return ((AppID) cctx.get(CallerContext.APP_ID));
    }

    /**
     * This method informs the MediaStorageOptionImpl object that its cached attributes may be stale and should be
     * updated immediately. The DVRStorageProxyImpl class will call this when the status changes to the READY state.
     * 
     */
    protected void refreshCachedData()
    {
        initializeCacheValues();
    }

    // private method to initialize the Attributes
    private void initializeCacheValues()
    {
        playBackBandWidth = nGetPlaybackBandwidth(nativeHandle);
        recordingBandWidth = nGetRecordingBandwidth(nativeHandle);
        totalMediaStorageCapacity = nGetTotalMediaCapacity(nativeHandle);
        simultaneousPlayAndRecord = nSimultaneousPlayAndRecord(nativeHandle);
        crossMsvTsbConversionSupported = nIsCrossMsvTsbConversionSupported(nativeHandle);
    }

    // Native method definitions

    private native long nGetTotalGeneralCapacity(int nativeStoragehandle);

    private native static void nInit();

    /**
     * This method returns the total capacity of the MEDIAFS on the specified storage device across all MSVs on that
     * device in bytes.
     * 
     * @param nativeStoragehandle -used by the MPE layer to identify the device
     * @return the total capacity of the MEDIAFS on the specified storage device
     */
    private native long nGetTotalMediaCapacity(int nativeStoragehandle);

    /**
     * This method returns the maximum recording bandwidth of the specified storage device in bits per second
     * 
     * @param nativeStoragehandle -used by the MPE layer to identify the device
     * @return eturns the maximum recording bandwidth of the specified storage device in bits per second
     */
    private native long nGetRecordingBandwidth(int nativeStoragehandle);

    /**
     * This Method returns the maximum playback bandwidth of the specified storage device in bits per second
     * 
     * @param nativeStoragehandle -used by the MPE layer to identify the device
     * @return returns the maximum playback bandwidth of the specified storage device in bits per second
     */
    private native long nGetPlaybackBandwidth(int nativeStoragehandle);

    /**
     * To Check if the specified storage device is capable of simultaneously recording and playing back previously
     * recorded media
     * 
     * @param nativeStoragehandle -used by the MPE layer to identify the device
     * @return true if is capable of simultaneously recording and playing back previously recorded media
     */
    private native boolean nSimultaneousPlayAndRecord(int nativeStoragehandle);

    /**
     * This Method returns the MEDIAFS space on the specified storage device that is not yet reserved by any MSV and is
     * not consumed by existing recordings.
     * 
     * @param nativeStorageHandle -used by the MPE layer to identify the device
     * @return - the MEDIAFS space on the specified storage device that is not yet reserved by any MSV and is not
     *         consumed by existing recordings.
     */
    private native long nGetAllocatableMediaStorage(int nativeStorageHandle);

    /**
     * 
     * This method redistributes the disk space on the specified storage device between MEDIAFS and GPFS usage. The
     * caller specifies the desired MEDIAFS size. The remainder of the capacity is designated for GPFS usage. The
     * specified mediafsSize must be less than StorageProxy.getTotalSpace(). This call may result in loss of content on
     * the storage device.
     * 
     * @param nativeStorageHandle -used by the MPE layer to identify the device
     * @param mediafsSize
     * @throws IllegalArgumentException - if the specified handle is invalid or the mediafsSize is greater than
     *         StorageProxy.getTotalCapacity() UnsupportedOperationException � if the storage device is not capable of
     *         repartitioning. IllegalStateException � if the device is busy, offline, or not present
     * 
     * 
     */
    private native void nRepartition(int nativeStorageHandle, long mediafsSize);

    /**
     * TO Check if the specified storage device supports the ability to convert TSB content across MediaStorageVolumes
     * 
     * @param nativeStorageHandle -used by the MPE layer to identify the device
     * @return if the specified storage device supports the ability to convert TSB content across MediaStorageVolumes
     */
    private native boolean nIsCrossMsvTsbConversionSupported(int nativeStorageHandle);

}
