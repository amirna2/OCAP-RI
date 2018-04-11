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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.manager.recording.RecordingManagerInterface;
import org.cablelabs.impl.security.PersistentStorageAttributes;
import org.cablelabs.impl.util.DVREventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.FreeSpaceListener;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.system.MonitorAppPermission;

/**
 * The <code>MediaStorageVolume</code> implementation.
 * 
 * @author Todd Earles
 */
public class MediaStorageVolumeImpl extends LogicalStorageVolumeImpl implements MediaStorageVolumeExt
{
    private static final Logger log = Logger.getLogger(MediaStorageVolumeImpl.class);

    volatile CallerContext ccList;

    private static final String DEFAULT_MSV_NAME = "default";

    // Hashtable to hold FreeSpaceAlarms for which there are registered
    // listeners.
    // Keyed on Integer "level"
    private Hashtable freeSpaceAlarms;

    // FIXME: ECR929 disable state
    private boolean m_enabled = true;

    private static CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();

    // Native media storage volume handle
    private int nativeHandle = 0;

    private static final int MAXIMUM_ORGID_NAME_LENGTH = 8;

    private static final int HEX_BASE = 16;

    /**
     * This is the constructor used to create a new MSV that has not yet been
     * allocated on the storage proxy.
     * 
     * @param proxy
     *            - The Storage proxy to which MSV is associated
     * @param name
     *            - Name of the MSV
     * @param appid
     *            - Owner of the MSV
     * @param perms
     *            - Permissions for the MSV
     * 
     * @throws IOException
     */
    MediaStorageVolumeImpl(DVRStorageProxyImpl proxy, String name, AppID appid, ExtendedFileAccessPermissions perms)
            throws IOException, IllegalArgumentException
    {
        super(proxy, name, appid, perms);

        if (log.isDebugEnabled())
        {
            log.debug("Creating new native MSV for LSV with path " + getPath() + "...");
        }
        nativeHandle = nNewVolume(proxy.getNativeHandle(), getPath());
        freeSpaceAlarms = new Hashtable();
    }

    // Gets the current allowed organisations to access by both read and write
    private String[] getAllowedOrgNameList()
    {
        String[] allowedOrgNameList = null;

        ExtendedFileAccessPermissions efap;
        try
        {
            efap = (ExtendedFileAccessPermissions) psa.getFilePermissions(getPath());
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Could not get file access permissions for MSV: " + getPath());
            }
            return null;
        }

        int[] readOrgIds = efap.getReadAccessOrganizationIds();
        int[] writeOrgIds = efap.getWriteAccessOrganizationIds();

        if (readOrgIds != null && writeOrgIds != null)
        {
            ArrayList allowedList = new ArrayList();
            for (int i = 0; i < writeOrgIds.length; i++)
            {
                for (int j = 0; j < readOrgIds.length; j++)
                {
                    if (writeOrgIds[i] == readOrgIds[j])
                    {
                        allowedList.add(encodeOrgIdAsHexString(writeOrgIds[i]));
                        break;
                    }
                }
            }
            allowedOrgNameList = new String[allowedList.size()];
            for (int i = 0; i < allowedOrgNameList.length; i++)
            {
                allowedOrgNameList[i] = (String) allowedList.get(i);
            }
        }
        return allowedOrgNameList;
    }

    private String encodeOrgIdAsHexString(int value)
    {
        String hexaValue = Integer.toString(value, HEX_BASE);
        if (hexaValue.length() < MAXIMUM_ORGID_NAME_LENGTH)
        {
            int appendZeroCount = MAXIMUM_ORGID_NAME_LENGTH - hexaValue.length();
            for (int i = 0; i < appendZeroCount; i++)
            {
                hexaValue = "0" + hexaValue;
            }
        }
        return hexaValue;
    }

    // Construct MediaStorageVolumeImpl Object
    private MediaStorageVolumeImpl(DVRStorageProxyImpl proxy, int nativeMediaHandle)
        throws IOException
    {
        super(proxy, nGetPath(nativeMediaHandle));

        if (log.isDebugEnabled())
        {
            log.debug("Recreating MSV object for LSV with path " + getPath() + "...");
        }
        this.nativeHandle = nativeMediaHandle;
        freeSpaceAlarms = new Hashtable();
    }

    private MediaStorageVolumeImpl(LogicalStorageVolumeImpl lsv)
        throws IOException
    {
        super((StorageProxyImpl) lsv.getStorageProxy(), lsv.getPath());
        if (log.isDebugEnabled())
        {
            log.debug("Recreating MSV object for LSV with path " + getPath() + "...");
        }
        try
        {
            nativeHandle = nNewVolume(((StorageProxyImpl) lsv.getStorageProxy()).getNativeHandle(), lsv.getPath());
        }
        catch (Throwable exception)
        {
            if (log.isErrorEnabled())
            {
                log.error("Failed to recreate MSV object due to exception " + exception.getClass().getName() + "("
                        + exception.getMessage() + ")");
            }
            exception.printStackTrace();
        }
        freeSpaceAlarms = new Hashtable();
    }

    // Description copied from MediaStorageVolume
    public void allocate(long bytes) throws IllegalArgumentException, SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Attempting allocation of " + bytes + " with msv :" + this.toString());
        }
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));
        long current_bytes = getAllocatedSpace();
        //bypass this check if allocate has never been called
        if (current_bytes > 0 && (bytes < current_bytes))
        {
            if (current_bytes - bytes > getFreeSpace())
            {
                throw new IllegalArgumentException("Too small for existing recordings");
            }
        }

        // SpaceAllocationHandler
        RecordingManagerInterface rm = (RecordingManagerInterface) ((RecordingManager) ManagerManager.getInstance(RecordingManager.class)).getRecordingManager();
        long allocated_bytes = rm.checkAllocation(this, getAppId(), bytes);
        if (allocated_bytes < bytes)
        {
            throw new IllegalArgumentException(
                    "Requested amount of storage exceeds the amount available for allocation");
        }
        //nAllocate is responsible for ensuring an IllegalArgumentException is thrown if new size is too small to contain existing recordings
        // Call native method to reserve space for the MSV.
        nAllocate(nativeHandle, allocated_bytes);
        
        if (current_bytes > 0 && ((bytes == 0) || (bytes > current_bytes)))
        { // The re-allocation is increasing the available size
            DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
            
            dsm.notifyMediaVolumeSpaceAvailable(this);
        }
    }

    // Description copied from MediaStorageVolume
    public long getAllocatedSpace()
    {
        return nGetAllocatedSpace(nativeHandle);
    }

    // Description copied from MediaStorageVolume
    public long getFreeSpace()
    {
        return nGetFreeSpace(nativeHandle);
    }

    // Description copied from MediaStorageVolumeExt
    public synchronized boolean hasAccess(AppID appId)
    {
        // Volume is not enabled
        if (!m_enabled) return false;

        ExtendedFileAccessPermissions efap = getFileAccessPermissions();

        // When both "other org" lists are 0-length (not null), then this MSV is
        // open to all apps
        if (efap.getReadAccessOrganizationIds() != null && efap.getReadAccessOrganizationIds().length == 0
                && efap.getWriteAccessOrganizationIds() != null && efap.getWriteAccessOrganizationIds().length == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("hasAccess() -- all apps have access to this MSV: " + getPath());
            }
            return true;
        }

        try
        {
            psa.checkReadAccess(getPath(), appId);
            psa.checkWriteAccess(getPath(), appId);
            return true;
        }
        catch (SecurityException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("hasAccess() -- App (" + appId + ") does not have access to this MSV: " + getPath());
            }
            return false;
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("hasAccess() -- Error getting attributes for this MSV: " + getPath());
            }
            return false;
        }
    }

    /**
     * Returns the native media volume handle.
     * 
     * @return nativeHandle
     */
    public int getNativeHandle()
    {
        return nativeHandle;
    }

    /**
     * This method notifies the MediaStorageVolumeImpl object that a free space
     * alarm has fired for this MSV. FreeSpaceListeners of this MSV that were
     * registered to be notified when the free space drops below the specified
     * level are invoked notifying them of this occurrence.
     * 
     * @param level
     *            - Theshold Level
     */
    public void onFreeSpaceAlarm(int level)
    {
        notifyListeners(level);
    }

    // Description copied from MediaStorageVolume
    public void allowAccess(String[] organizations) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));

        ExtendedFileAccessPermissions perms = getFileAccessPermissions();

        synchronized (this)
        {
            DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);

            // If null is passed in and
            // access to this volume has been removed by a call to the
            // code>removeAccess</code> method, then access is restored to the
            // organizations that had access before all access was removed
            if (organizations == null)
            {
                if (m_enabled == false)
                {
                    m_enabled = true;
                    dsm.notifyMediaVolumeAccessStateChanged(this);
                }
            }
            // If an array of length 0 is passed,
            // any application can use this volume.
            // Per ECR 929 - a length of zero means a reenable
            else if (organizations.length == 0)
            {
                ExtendedFileAccessPermissions newFap = new ExtendedFileAccessPermissions(true, true,
                        perms.hasReadOrganisationAccessRight(), perms.hasWriteOrganisationAccessRight(),
                        perms.hasReadApplicationAccessRight(), perms.hasWriteApplicationAccessRight(), null, null);
                try
                {
                    psa.setFileAttributes(getPath(), newFap, null, -1, false);
                    dsm.notifyMediaVolumeAccessStateChanged(this);
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Could not set new file access permissions on MSV: " + getPath());
                    }
                }
            }
            // Allow access to the valid organizations.
            else
            {
                int[] newOrgIds = getOrgIdsFromOrgName(organizations);
                ExtendedFileAccessPermissions newFap = new ExtendedFileAccessPermissions(
                        perms.hasReadWorldAccessRight(), perms.hasReadWorldAccessRight(),
                        perms.hasReadOrganisationAccessRight(), perms.hasWriteOrganisationAccessRight(),
                        perms.hasReadApplicationAccessRight(), perms.hasWriteApplicationAccessRight(), union(
                                perms.getReadAccessOrganizationIds(), newOrgIds), union(
                                perms.getWriteAccessOrganizationIds(), newOrgIds));

                try
                {
                    psa.setFileAttributes(getPath(), newFap, null, -1, false);

                    // Only notify the listeners if the msv is enabled.
                    if (m_enabled == true)
                    {
                        dsm.notifyMediaVolumeAccessStateChanged(this);
                    }
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Could not set new file access permissions on MSV: " + getPath());
                    }
                }
            }
        }
    }

    // Create a union of the two arrays
    private int[] union(int[] array1, int[] array2)
    {
        HashSet set = new HashSet();
        if (array1 == null || array1.length == 0) return array2;
        if (array2 == null || array2.length == 0) return array1;

        // Add all of our original orgIds to a set
        for (int i = 0; i < array1.length; i++)
            set.add(new Integer(array1[i]));

        // Add our new orgIds to the set (Set will not allow duplicates
        for (int i = 0; i < array2.length; i++)
            set.add(new Integer(array2[i]));

        int[] retVal = new int[set.size()];
        int i = 0;
        for (Iterator iter = set.iterator(); iter.hasNext();)
        {
            Integer orgId = (Integer) iter.next();
            retVal[i] = orgId.intValue();
        }

        return retVal;
    }

    private int[] getOrgIdsFromOrgName(String orgNames[])
    {
        int orgIds[] = new int[orgNames.length];
        for (int i = 0; i < orgNames.length; i++)
        {
            orgIds[i] = getOrgIdsFromOrgName(orgNames[i]);
        }
        return orgIds;
    }

    private int getOrgIdsFromOrgName(String orgName)
    {
        orgName = orgName.trim();
        int orgId = Integer.parseInt(orgName, HEX_BASE);
        return orgId;
    }

    private int[] removeOrgId(int[] orgIds, String organization)
    {
        // If the allowed list doesn't exist currently then
        // keep it that way.
        if (orgIds == null) return null;

        // Is the org in our list?
        boolean found = false;
        int orgId = Integer.parseInt(organization, HEX_BASE);
        for (int i = 0; i < orgIds.length; i++)
        {
            if (orgIds[i] == orgId)
            {
                found = true;
                break;
            }
        }

        // No change
        if (!found) return orgIds;

        // Build the new list
        int[] newList = new int[orgIds.length - 1];
        for (int i = 0, j = 0; i < orgIds.length; i++)
        {
            if (orgIds[i] != orgId)
            {
                newList[j] = orgIds[i];
                j++;
            }
        }

        // If the new list is empty return null;
        if (newList.length == 0) return null;

        return newList;
    }

    // Description copied from MediaStorageVolume
    public void removeAccess(String organization) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));

        synchronized (this)
        {
            ExtendedFileAccessPermissions fap = getFileAccessPermissions();

            // Get the storage manager.
            DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);

            // Passing in null removes all application access to this volume.
            if (organization == null)
            {
                if (m_enabled == true)
                {
                    m_enabled = false;
                    dsm.notifyMediaVolumeAccessStateChanged(this);
                }
            }
            // Passing in a valid organization will remove privileges for the
            // organization
            // and notify the listeners.
            else
            {
                ExtendedFileAccessPermissions newEFAP = new ExtendedFileAccessPermissions(
                        fap.hasReadWorldAccessRight(), fap.hasWriteWorldAccessRight(),
                        fap.hasReadOrganisationAccessRight(), fap.hasWriteOrganisationAccessRight(),
                        fap.hasReadApplicationAccessRight(), fap.hasWriteApplicationAccessRight(), removeOrgId(
                                fap.getReadAccessOrganizationIds(), organization), removeOrgId(
                                fap.getWriteAccessOrganizationIds(), organization));
                try
                {
                    psa.setFileAttributes(getPath(), newEFAP, null, -1, false);
                    dsm.notifyMediaVolumeAccessStateChanged(this);
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Could not set new file access permissions on MSV: " + getPath());
                    }
                }
            }
        }
    }

    // Description copied from MediaStorageVolume
    public String[] getAllowedList()
    {
        String allowedList[] = null;
        synchronized (this)
        {
            if (m_enabled == false)
            {
                // Null is returned when all access has
                // been removed from this volume.
                return null;
            }

            // Get the allowed list.
            allowedList = getAllowedOrgNameList();
            if (null == allowedList)
            {
                // zero length array is returned when all
                // organizations have access.
                allowedList = new String[0];
            }
            // An array of strings representing organizations
            // that are allowed use this volume.
            return allowedList;
        }
    }

    // Description copied from LogicalStorageVolumeExt
    public boolean isMediaStorageVolume()
    {
        return true;
    }

    /*
     * Adds a listener that is notified when available free space is less than a
     * specified level. The parameter level is a percentage of the total
     * available space in the volume. For example, a level of 10 would cause the
     * listener to be notified when less than 10% of the volume is available for
     * use. Determination of the level is implementation specific and the
     * listener is notified whenever the threshold indicated by the level is
     * crossed and available storage is less than the level parameter
     * 
     * @param listener The listener to be added. @param level The level of free
     * space remaining at which to notify the listener.
     */
    public void addFreeSpaceListener(FreeSpaceListener listener, int level)
    {
        // Don't support 0 or 100
        if (level < 1)
        {
            level = 1;
        }
        else if (level > 99)
        {
            level = 99;
        }

        CallerContext cc = ccm.getCurrentContext();
        synchronized (freeSpaceAlarms)
        {
            FreeSpaceAlarm alarm;

            // Only register one native alarm per level
            if ((alarm = (FreeSpaceAlarm) freeSpaceAlarms.get(new Integer(level))) == null)
            {
                alarm = new FreeSpaceAlarm();
                alarm.ccList = cc;
                freeSpaceAlarms.put(new Integer(level), alarm);
                if (log.isDebugEnabled())
                {
                    log.debug("addFreeSpaceListener() Adding native alarm for level " + level + "...");
                }
                nAddAlarm(nativeHandle, level);
            }
            else
            {
                alarm.ccList = CallerContext.Multicaster.add(alarm.ccList, cc);
                alarm.refCount++;
                if (log.isDebugEnabled())
                {
                    log.debug("addFreeSpaceListener() there are now " + alarm.refCount + " listeners for level "
                            + level);
                }
            }

            CCData data = getCCData(cc, alarm, level, true);
            data.listeners = DVREventMulticaster.add(data.listeners, listener);
        }
    }

    /**
     * Removes a free space listener. If the parameter listener was not
     * previously added or has already been removed this method does nothing
     * successfully.
     * 
     * @param listener
     *            The listener to remove.
     */
    public void removeFreeSpaceListener(FreeSpaceListener listener)
    {
        CallerContext cc = ccm.getCurrentContext();
        synchronized (freeSpaceAlarms)
        {
            // Iterate over all alarms looking for one that is associated with
            // this
            // CallerContext
            for (Enumeration e = freeSpaceAlarms.keys(); e.hasMoreElements();)
            {
                Integer level = (Integer) e.nextElement();
                FreeSpaceAlarm alarm = (FreeSpaceAlarm) freeSpaceAlarms.get(level);

                CCData data;
                // Does this CallerContext have data associated with this alarm?
                if ((data = getCCData(cc, alarm, level.intValue(), false)) != null)
                {
                    // Remove the listener
                    data.listeners = DVREventMulticaster.remove(data.listeners, listener);

                    // If this was the last listener for this CallerContext at
                    // this level,
                    // remove the CallerContext from the level and remove the
                    // callback data
                    if (data.listeners == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("No listeners remaining for this CallerContext " + cc);
                        }
                        cc.removeCallbackData(alarm);
                        alarm.ccList = CallerContext.Multicaster.remove(alarm.ccList, cc);

                        if (alarm.ccList == null)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("No listeners remaining for level " + level + ".  Removing native alarm.");
                            }

                            // If this was the last CallerContext associated
                            // with this level, we can
                            // unregister the native alarm for this level
                            nRemoveAlarm(nativeHandle, level.intValue());
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the minimum storage space size for time-shift buffer use.  If the
     * <code>setMinimumTSBSize</code> method has been called then the value
     * set SHALL be returned.  If the <code>setMinimumTSBSize</code> method
     * has not been called but a minimum time-shift buffer size has been
     * configured by the implementation then that value SHALL be returned.
     * Otherwise, if neither an application nor the implementation has set the
     * value then 0 SHALL be returned.  The implementation SHALL NOT override
     * a value set by an application.
     *
     * @return The minimum time-shift buffer size.
     */
    public long getMinimumTSBSize()
    {
        return nGetMinimumTSBSize(nativeHandle);
    }

    /**
     * Sets the minimum storage space for time-shift buffer use.  The
     * implementation SHALL make at least the minimum storage set by this
     * method available to satisfy the requirements of
     * <code>TimeShiftProperties.setMinimumDuration</code>.  Storage
     * allocated by a call to this method SHALL NOT be used for scheduled
     * recordings.  This method SHALL NOT affect any existing recorded content.
     * If the specified size is too large for the MSV to accommodate existing permanent
     * recordings, an IllegalArgumentException is thrown and the minimum TSB allocation
     * is not changed.
     *
     * @param size The size in bytes of the minimum time-shift buffer storage
     *      to set.
     *
     * @throws IllegalArgumentException if size > getFreeSpace() + current TSB size.
     * @throws SecurityException if the calling application does not have
     *      MonitorAppPermission("storage") permission.
     */
    public void setMinimumTSBSize(long size)
    {
        if (log.isInfoEnabled())
        {
            log.info("setMinimumTSBSize: " + size);
        }
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));
        long currentTSBSize = getMinimumTSBSize();
        long freeSpace = getFreeSpace();
        if (size > freeSpace + currentTSBSize)
        {
            throw new IllegalArgumentException("specified size is too large to accommodate existing recordings - free space: " + freeSpace);
        }
        nSetMinimumTSBSize(nativeHandle, size);
    }

    /**
     * This is a static method for recreating LogicalStorageVolume and
     * MediaStorageVolume objects for volumes that are already present in
     * persistent storage on the specified device. This method also created the
     * default media volume for the specified storage proxy if one does not
     * already exist. This method is part of the generic LogicalStorageVolumeExt
     * interface which is why the r eturn type is LogicalStorageVolume[] instead
     * of MediaStorageVolume[]. This method overrides the base class method.
     * 
     * This method should be called one time at startup or when a storage device
     * is added and has entered the ready state.Returns an array of
     * LogicalStorageVolumeImpl and MediaStorageVolumeImpl objects. The
     * MediaStorageVolumeImpl class does not retain a reference to this array.
     * 
     * proxy - Proxy to which this MSV associated
     */
    protected static LogicalStorageVolume[] loadVolumes(StorageProxyImpl proxy)
    {
        // Initializing Variables
        DVRStorageProxyImpl dvrStorageProxyImpl = (DVRStorageProxyImpl) proxy;
        Vector volumeList = new Vector();
        LogicalStorageVolumeImpl[] msvs = null;

        if (log.isDebugEnabled())
        {
            log.debug("Loading MSVs...");
        }
        // Retreives List of Media Volume Handle and Creates the MSVs
        nGetVolumes(proxy.getNativeHandle(), volumeList);

        if (log.isDebugEnabled())
        {
            log.debug("Found " + volumeList.size() + " native MSV handles");
        }
        if (volumeList.size() > 0)
        {
            int handle = 0;

            Vector msvCandidates = new Vector(volumeList.size());

            for (int volumeCount = 0; volumeCount < volumeList.size(); volumeCount++)
            {
                handle = ((Integer) volumeList.elementAt(volumeCount)).intValue();
                try
                {
                    msvCandidates.add(new MediaStorageVolumeImpl(dvrStorageProxyImpl, handle));
                }
                catch (IOException e)
                {
                    SystemEventUtil.logRecoverableError("Could not load existing MSV!", e);
                }
            }
            
            msvs = new LogicalStorageVolumeImpl[msvCandidates.size()];
            msvCandidates.copyInto(msvs);
        }
        // Loading LSVs
        LogicalStorageVolume[] lsvs = LogicalStorageVolumeImpl.loadVolumes(proxy);

            int count = 0;

            if (lsvs != null)
            {
                count = lsvs.length;
            }
        if (log.isDebugEnabled())
        {
            log.debug("Found " + count + " LSVs on the storage device");
        }

        // Creating the Default MSV if one not Present
        if (findDefaultMSV(msvs) == null)
        {
            try
            {
                MediaStorageVolumeImpl defaultMSV = null;
                LogicalStorageVolume tempLSV = findDefaultMSV(lsvs);

                // the native layer does not persist MSV meta-data across
                // reboots until phase
                // 3 of the StorageManager development and integration plan so
                // it necessary to
                // check for the default MSV path in the list of LSVs if not
                // found in the list
                // of MSVs. If the default MSV is found among the list of LSVs,
                // then create an
                // MSV object to represent it.
                if (tempLSV != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Recreating default MSV from LSV...");
                    }
                    defaultMSV = new MediaStorageVolumeImpl((LogicalStorageVolumeImpl) tempLSV);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Creating default MSV...");
                    }
                    ExtendedFileAccessPermissions defMSVPerms = new ExtendedFileAccessPermissions(true, true, true,
                            true, true, true, null,// OtherOrgRead
                            null);// OtherOrgwrite
                    defaultMSV = new MediaStorageVolumeImpl((DVRStorageProxyImpl) proxy, DEFAULT_MSV_NAME, new AppID(0,
                            0),// For Defult MSV appid and ORGID is 0
                            defMSVPerms);
                }
                
                // +1 is to add Default msv
                LogicalStorageVolumeImpl[] tempMSVs = new LogicalStorageVolumeImpl[volumeList.size() + 1];
                for (int i = 0; i < volumeList.size(); i++)
                {
                    tempMSVs[i] = msvs[i];
                }
                tempMSVs[volumeList.size()] = defaultMSV;
                msvs = tempMSVs;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (msvs != null && lsvs != null && msvs.length > 0 && lsvs.length > 0)
        {
            // Reconcile the List of MSV and LSV to eleminate the Duplicate
            if (log.isDebugEnabled())
            {
                log.debug("Reconciling MSV and LSV lists...");
            }
            for (int i = 0; i < msvs.length; i++)
            {
                for (int j = 0; j < lsvs.length; j++)
                {
                    if (isLSVaMSV((LogicalStorageVolumeImpl) lsvs[j], msvs[i]))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Replacing LSV object at index " + j + " with MSV named " + msvs[i].getName());
                        }
                        lsvs[j] = msvs[i];
                        break;
                    }
                }
            }
        }
        else
        {
            lsvs = msvs;
        }
        return lsvs;
    }

    private static boolean isLSVaMSV(LogicalStorageVolumeImpl lsv, LogicalStorageVolumeImpl msv)
    {
        if (lsv.getAppId().equals(msv.getAppId()))
        {
            if (lsv.getName().equals(msv.getName()))
            {
                return true;
            }
        }
        return false;
    }

    // returns object corresponding to to default MSV if present
    private static LogicalStorageVolume findDefaultMSV(LogicalStorageVolume[] msvs)
    {
        LogicalStorageVolume defaultMsv = null;

        if (msvs != null)
        {
            AppID defaultMSVAppid = new AppID(0, 0);
            for (int msvsCount = 0; msvsCount < msvs.length; msvsCount++)
            {
                if (((LogicalStorageVolumeExt) msvs[msvsCount]).getAppId().equals(defaultMSVAppid))
                {
                    defaultMsv = msvs[msvsCount];
                    break;
                }
            }
        }
        return defaultMsv;
    }

    /**
     * Notifies Free Space Listener
     */
    private void notifyListeners(final int level)
    {
        synchronized (freeSpaceAlarms)
        {
            final FreeSpaceAlarm alarm = (FreeSpaceAlarm) freeSpaceAlarms.get(new Integer(level));

            if (alarm == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("No alarm associated with level " + level + ".  This should not happen!");
                }
                return;
            }

            CallerContext ccList = alarm.ccList;
            if (ccList != null)
            {
                ccList.runInContext(new Runnable()
                {
                    public void run()
                    {
                        // Notify listeners. Use a local copy of data so that it
                        // does not change while we are using it.
                        CallerContext cc = ccm.getCurrentContext();
                        CCData data = getCCData(cc, alarm, 0, false);
                        if (data != null && data.listeners != null)
                        {
                            data.listeners.notifyFreeSpace();
                        }
                    }
                });
            }
        }
    }

    private class FreeSpaceAlarm
    {
        public CallerContext ccList = null;

        public int refCount = 1;
    }

    /**
     * Per caller context data
     */
    private class CCData implements CallbackData
    {
        public FreeSpaceListener listeners;

        public FreeSpaceAlarm alarm;

        int alarmLevel;

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
            synchronized (freeSpaceAlarms)
            {
                Integer level = new Integer(alarmLevel);

                // If we are the only caller registered to this free space
                // level,
                // remove the level and disable the native alarm
                if ((alarm.ccList = CallerContext.Multicaster.remove(alarm.ccList, cc)) == null)
                {
                    freeSpaceAlarms.remove(level);
                    nRemoveAlarm(nativeHandle, alarmLevel);

                    // Remove the CCData associated with this alarm
                    cc.removeCallbackData(alarm);
                }

                listeners = null;
            }
        }
    }

    public void delete()
    {
        deleteAllRecordings();
        super.delete();
        try
        {
            nDeleteVolume(nativeHandle);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void deleteAllRecordings()
    {
        try
        {
            OcapRecordingManager recMgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList recList = recMgr.getEntries();
            RecordingRequest recReq = null;
            MediaStorageVolumeImpl recMSV = null;
            for (int index = 0; index < recList.size(); index++)
            {
                recReq = recList.getRecordingRequest(index);
                if (recReq != null && recReq instanceof OcapRecordingRequest)
                {
                    OcapRecordingRequest orr = (OcapRecordingRequest) recReq;
                    OcapRecordingProperties ocapRecProp = (OcapRecordingProperties) orr.getRecordingSpec()
                            .getProperties();
                    recMSV = (MediaStorageVolumeImpl) ocapRecProp.getDestination();
                    if (recMSV != null)
                    {
                        if ((this.getStorageProxy().getName()).equals(recMSV.getStorageProxy().getName()))
                        {
                            if (((this.getName()).equals(recMSV.getName()))
                                    && ((this.getAppId()).equals(recMSV.getAppId()))) recReq.delete();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context -- specific to a free space alarm level.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @param alarm
     *            the free space alarm object this data will be associated with
     *            in the CallerContext
     * @param level
     *            the level associated with the given alarm
     * @param create
     *            true if we should create the data if it does not exist
     * @return the data object for the specified caller context (or null if
     *         create==false and data object was not found)
     */
    private CCData getCCData(CallerContext cc, FreeSpaceAlarm alarm, int level, boolean create)
    {
        synchronized (freeSpaceAlarms)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(alarm);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null && create)
            {
                data = new CCData();
                data.alarm = alarm;
                data.alarmLevel = level;
                cc.addCallbackData(data, alarm);
            }
            return data;
        }
    }
    
    long getCreateDate()
    {
        return nGetCreateDate(nativeHandle);
    }

    /**
     * Native method definitions
     */
    /**
     * Returns the amount of free space available on the specified media storage
     * volume in bytes.
     * 
     * @param nativehandle
     * @return - amount of free space available on the specified media storage
     *         volume in bytes.
     */
    private native long nGetFreeSpace(int nativehandle);

    /**
     * Retrieves list of media volumes as a vector of native media volume
     * handles
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @param volumeList
     *            - data structure to be filled by native media volume handles
     */
    private native static void nGetVolumes(int nativeStorageHandle, Vector volumeList);

    /**
     * This method creates a new native media volume for the specified
     * application on the specifed storage device.
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @param lsvPath
     *            - Path for lsv
     * @return - Returns the native media volume handle
     * 
     * @throws IllegalArgumentException
     *             if an invalid parameter was specified or the specified MSV
     *             already exists
     * @throws IOException
     * 
     * @throws UnsupportedOperationException
     *             UnsupportedOperationException if the storage device does not
     *             support creation of media storage volumes.
     */
    private native int nNewVolume(int nativeStorageHandle, String lsvPath) throws IllegalArgumentException,
            IOException, UnsupportedOperationException;

    /**
     * This method deletes the native media volume.
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     */
    private native void nDeleteVolume(int nativeVolumeHandle);

    /**
     * This method returns the amount of space allocated on this volume in bytes
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     * @return - returns the amount of space allocated on this volume in bytes
     */
    private native long nGetAllocatedSpace(int nativeVolumeHandle);

    /**
     * Modifies the allocation for the specified volume.
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     * @param sizeBytes
     *            - size to be allocated
     */
    private native void nAllocate(int nativeVolumeHandle, long sizeBytes);

    /**
     * returns the path for the media storage volume or null if not supported
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     * @return - path for the media storage volume or null if not supported
     */
    private native static String nGetPath(int nativeVolumeHandle);

    /**
     * Registers free space alarm for the specified volume with the native
     * layer. The level is specified as a percentage.
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     * @param level
     *            - Level at which the User is notified
     */
    private native void nAddAlarm(int nativeVolumeHandle, int level);

    /**
     * Unregisters a free space alarm from the native layer.
     * 
     * @param nativeVolumeHandle
     *            - used by the MPE layer to identify this device
     * @param level
     *            - For a paticular MSV which Level is to be removed
     */
    private native void nRemoveAlarm(int nativeVolumeHandle, int level);
    
    /**
     * Returns the create date (UTC time in seconds) of the given volume
     * 
     * @param nativeVolumeHandle 
     * @return create date
     */
    private native long nGetCreateDate(int nativeVolumeHandle);

    /**
     * Returns the minimum TSB size of the given volume
     *
     * @param nativeVolumeHandle
     * @return minimum TSB size
     */
    private native long nGetMinimumTSBSize(int nativeVolumeHandle);

    /**
     * Allow the minimum TSB size to be set for a given volume
     *
     * @param nativeVolumeHandle
     * @param bytes size in bytes
     */
    private native void nSetMinimumTSBSize(int nativeVolumeHandle, long bytes);

}
