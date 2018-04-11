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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.storage.MediaStorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

/**
 * A <code>StorageProxy</code> implementation that supports DVR.
 * 
 * @author Todd Earles
 */

public class DVRStorageProxyImpl extends StorageProxyImpl
{
    private static final Logger log = Logger.getLogger(DVRStorageProxyImpl.class);

    private Object sync = new Object();

    /**
     * Private Constructor not allow others to create using Default constructor
     */
    private DVRStorageProxyImpl()
    {
    }

    /**
     * Constructs a DVRStorageProxyImpl object. Calls the matching constructor
     * of the super class. Creates a MediaStorageOptionImpl object and adds it
     * to the list of storage options. Retrieves the list of media volumes from
     * the storage device and creates corresponding MediaStorageVolumeImpl
     * objects for each. These volumes are added to the list of volumes returned
     * by getVolumes().
     * 
     * @param nativeStorageHandle
     *            - Native Handle
     * @param status
     *            - current status
     */

    public DVRStorageProxyImpl(int nativeStorageHandle, int status)
    {
        super(nativeStorageHandle, status);
        MediaStorageOption mediaStorageOption = createMediaStorageOption();
        addOption(mediaStorageOption);
        if (log.isDebugEnabled())
        {
            log.debug("DVRStorageProxyImpl :Constructing DVRStorageProxyImpl object");
        }
    }

    /**
     *  
     */
    public void deleteVolume(final LogicalStorageVolume volume)
    {
        if ((volume instanceof MediaStorageVolumeImpl))
        {
            if (log.isDebugEnabled())
            {
                log.debug("deleteVolume :Removing Media Storage Volume ");
            }

            try
            {
                checkMonitorPermission();
                AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        delete(volume);
                        ((MediaStorageVolumeImpl) volume).delete();
                        return null;
                    }
                });
            }
            catch (SecurityException e)
            {
                // If we don't have MonApp permission, then just make sure we
                // are
                // the owner before attempt to delete
                if (checkOwnership(volume))
                {
                    delete(volume);
                    ((MediaStorageVolumeImpl) volume).delete();
                }
                else
                {
                    throw e;
                }
            }
        }
        else
        {
            super.deleteVolume(volume);
        }
    }

    /**
     * This method adds the specified MediaStorageVolume object to the list of
     * volumes reported by StorageProxy.getVolumes().
     * 
     * @param volume
     *            - MSV to be added
     * @return Returns false if the object is already present in the list.
     */
    public boolean addMediaVolume(MediaStorageVolume volume)
    {
        if (volumes.contains(volume))
        {
            if (log.isDebugEnabled())
            {
                log.debug("addMediaVolume :Media Storage Volume Object Already Persent");
            }
            return false;
        }
        if (log.isDebugEnabled())
        {
            log.debug("addMediaVolume :Adding Media Storage Volume Object");
        }
        volumes.add(volume);
        return true;
    }

    /**
     * 
     * Overrides the onStatusChange() method of the super class to know when
     * state changes so that MSVs can be retrieved from native layer when status
     * becomes READY and so that cached attributes stored by
     * MediaStorageOptionsImpl and MediaStorageVolumeImpl objects can be
     * refreshed when a status change occurs and the resulting state is READY.
     * This function will call the onStatusChange() method of the super class
     * first before performing DVR-specific processing.
     * 
     * @see StorageProxyImpl#onStatusChange(byte)
     */
    protected void onStatusChange(byte status)
    {
        super.onStatusChange(status);
        if (status == StorageProxy.READY || status == StorageProxy.NOT_PRESENT)
        {
            if (status == StorageProxy.READY)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DvrStorageProxy onStatusChange :Ready State");
                }
            }
                else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("DvrStorageProxy onStatusChange :Not Present State");
                }
            }
            for (int mediaOptionCount = 0; mediaOptionCount < options.size(); mediaOptionCount++)
            {
                if (options.elementAt(mediaOptionCount) instanceof MediaStorageOption)
                {
                    ((MediaStorageOptionImpl) options.elementAt(mediaOptionCount)).refreshCachedData();
                }
            }
        }
    }

    /**
     * If the specified nativeMediaVolumeHandle corresponds to an MSV on that
     * StorageProxy, the method invokes the onFreeSpaceAlarm() method on the
     * associated MediaStorageVolumeImpl object
     * 
     * @param nativeMediaVolumeHandle
     *            Media Volume Handle
     * @param level
     *            threshold level
     * @return true - if any Media Volume has consumed FREE_ALARM event
     */
    protected boolean onFreeSpaceAlarm(int nativeMediaVolumeHandle, int level)
    {
        for (int mediaVolumeCount = 0; mediaVolumeCount < volumes.size(); mediaVolumeCount++)
        {
            if (volumes.elementAt(mediaVolumeCount) instanceof MediaStorageVolumeImpl)
            {
                MediaStorageVolumeImpl mediaStorageVolume = (MediaStorageVolumeImpl) volumes.elementAt(mediaVolumeCount);
                if (mediaStorageVolume.getNativeHandle() == nativeMediaVolumeHandle)
                {
                    mediaStorageVolume.onFreeSpaceAlarm(level);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Loads existing logical storage volume objects off the storage device.
     * Overrides the base class method.Calls
     * MediaStorageVolumeImpl.loadVolumes() and saves the objects returned in
     * member variable inherited from base class. The resulting list is
     * queryable via the public getVolumes() method implemented by the base
     * class.
     */
    void loadVolumes()
    {
        LogicalStorageVolume[] mediaStorageVolume = MediaStorageVolumeImpl.loadVolumes(this);
        int count = 0;

        if (mediaStorageVolume != null)
        {
            count = mediaStorageVolume.length;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Loaded " + count + " LSVs");
        }

        for (int index = 0; index < count; index++)
        {
            volumes.add(mediaStorageVolume[index]);
        }
    }

    // This is a package private method to add
    // other storage options being added to this storage proxy.
    private void addOption(StorageOption option)
    {
        synchronized (sync)
        {
            options.add(option);
        }
    }

    // private method to create MediaStorageOption
    private MediaStorageOption createMediaStorageOption()
    {
        return new MediaStorageOptionImpl(this);
    }
}
