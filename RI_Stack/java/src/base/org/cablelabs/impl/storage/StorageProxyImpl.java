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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.dvb.application.AppID;
import org.ocap.storage.DetachableStorageOption;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.RemovableStorageOption;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.security.PersistentStorageAttributes;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * The <code>StorageProxy</code> implementation.
 * 
 * @author Todd Earles
 */
public class StorageProxyImpl implements StorageProxy
{

    private static final Logger log = Logger.getLogger(StorageProxyImpl.class);

    private int status;

    private int nativeHandle;

    private String name;

    private String displayName;

    private long totalSpace;

    private boolean[] supportedAccessRights;

    private String rootPath;

    private CallerContext cc;

    private Object implObject = new Object();

    // TO track whether Status is READY for the first time.
    private boolean firstTimeReadyFlag = false;

    private static final String FILE_SEPARATOR = File.separator;// System.getProperty("file.separator");

    private static final String LSV_ROOT_DIRECTORY = "OCAP_LSV";

    // The Data Stucture to hold all Storage Options.
    protected Vector options = null;

    // The Data Stucture to hold all Volumes.
    protected Vector volumes = null;

    // Default Constructor
    protected StorageProxyImpl()
    {

    }

    /**
     * constructs a StorageProxyImpl object that represents the specified native
     * storage device On construction, the StorageProxyImpl object caches the
     * stable attributes of the device and creates any non-DVR StorageOption
     * objects pertinent to this storage device (e.g.
     * DetachableStorageOptionImpl, RemovableStorageOptionImpl). The storage
     * options are queryable via getOptions() method. If the device is ready,
     * the StorageProxy object then creates the �OCAP_LSV� directory if it
     * doesn�t already exist and initiates the process deserializing logical
     * storage volume objects already persisted on the device under this
     * directory. The LSVs are queryable via the getVolumes() method
     * 
     * @param handle
     *            - Native Handle for the Device
     * @param status
     *            - Indicates the initial status of the storage proxy
     */
    public StorageProxyImpl(int handle, int status)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Creating the StorageProxy Impl for Hnadle:" + handle + " Status:" + status);
        }
        this.nativeHandle = handle;
        options = new Vector();
        volumes = new Vector();
        createStorageOptions();
        onStatusChange(status);
    }

    // Description copied from StorageProxy
    public String getName()
    {
        return name;
    }

    // Description copied from StorageProxy
    public String getDisplayName()
    {
        return displayName;
    }

    // Description copied from StorageProxy
    public StorageOption[] getOptions()
    {
        StorageOption[] avlStorageOptions = null;
        final int size = options.size();
        if (log.isDebugEnabled())
        {
            log.debug("Available Options Size is :" + size);
        }
        if (size > 0)
        {
            avlStorageOptions = new StorageOption[size];
            options.copyInto(avlStorageOptions);
        }
        return avlStorageOptions;
    }

    // Description copied from StorageProxy
    public int getStatus()
    {
        return status;
    }

    // Description copied from StorageProxy
    public long getTotalSpace()
    {
        return totalSpace;
    }

    // Description copied from StorageProxy
    public long getFreeSpace()
    {
        return nGetFreeSpace(nativeHandle);
    }

    // Description copied from StorageProxy
    public LogicalStorageVolume[] getVolumes()
    {
        LogicalStorageVolumeImpl[] avlVolumes = null;
        final int size = volumes.size();
        if (log.isDebugEnabled())
        {
            log.debug("Available Volumes Size for " + displayName + " :" + size);
        }
        if (size > 0)
        {
            avlVolumes = new LogicalStorageVolumeImpl[size];
            volumes.copyInto(avlVolumes);
        }
        else
        {
            avlVolumes = new LogicalStorageVolumeImpl[0];
        }
        return avlVolumes;
    }

    // Description copied from StorageProxy
    public boolean[] getSupportedAccessRights()
    {
        return supportedAccessRights;
    }

    // Description copied from StorageProxy
    public LogicalStorageVolume allocateGeneralPurposeVolume(String name, ExtendedFileAccessPermissions fap)
            throws IOException
    {
        synchronized (implObject)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            cc = ccm.getCurrentContext();
        }
        if (log.isDebugEnabled())
        {
            log.debug("Creating LSV of Name:" + name + "    With Following Permissions:   ReadApplicationAccessRight:"
                    + fap.hasReadApplicationAccessRight() + "   ReadOrganisationAccessRight:"
                    + fap.hasReadOrganisationAccessRight() + "   ReadWorldAccessRight:" + fap.hasReadWorldAccessRight()
                    + "  WriteApplicationAccessRight:" + fap.hasWriteApplicationAccessRight()
                    + "  WriteOrganisationAccessRight:" + fap.hasWriteOrganisationAccessRight()
                    + "  WriteWorldAccessRight:" + fap.hasWriteWorldAccessRight());
        }

        // Check whether the application has permissions for LSV creation
        SecurityUtil.checkPermission(new PersistentStoragePermission());

        LogicalStorageVolumeImpl logicalStorageVolume = null;

        logicalStorageVolume = new LogicalStorageVolumeImpl(this, name, (AppID) cc.get(CallerContext.APP_ID), fap);

        if (log.isDebugEnabled())
        {
            log.debug("APPID Of LSV is :" + cc.get(CallerContext.APP_ID));
        }
        volumes.add(logicalStorageVolume);
        return logicalStorageVolume;
    }

    // Description copied from StorageProxy
    public void deleteVolume(final LogicalStorageVolume lsv)
    {
        // permission check
        if (log.isDebugEnabled())
        {
            log.debug("Start Deleting the Lsv name" + lsv.getPath());
        }

        try
        {
            checkMonitorPermission();
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    delete(lsv);
                    ((LogicalStorageVolumeImpl) lsv).delete();
                    return null;
                }
            });
        }
        catch (SecurityException e)
        {
            if (checkOwnership(lsv))
            {
                delete(lsv);
                ((LogicalStorageVolumeImpl) lsv).delete();
            }
            else
            {
                throw e;
            }
        }
    }

    // Description copied from StorageProxy
    public void initialize(boolean userAuthorized)
    {
        checkMonitorPermission();

        // Check to see if this is a detachable device, if not do nothing
        StorageOption[] options = getOptions();

        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof DetachableStorageOption)
            {
                // Delete the LSV objects containing content
                deleteAllVolumes();

                if (!nFormat(nativeHandle, userAuthorized))
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Format Initiated Failed........");
                    }
                    throw new IllegalStateException("FORMAT FAILUE!!!!!!");
                }

                if (log.isDebugEnabled())
                {
                    log.debug("Format Initiated.....");
                }
            }
        }
    }

    // Description copied from ResourceProxy
    public ResourceClient getClient()
    {
        return null;
    }

    /**
     * Returns the Native Handle for the StorageProxy
     * 
     * @return handle for the StroageProxy
     */
    public int getNativeHandle()
    {
        return nativeHandle;
    }

    public boolean volumeExistsInProxy(LogicalStorageVolume lsv)
    {
        return volumes.contains(lsv);
    }

    /**
     * This method is used by Storage Manager to informs the Storage Proxy that
     * its status or capacity has changed. Loads existing volume objects off of
     * the storage device the first time it enters the READY state.
     * 
     * @param status
     *            The status parameter specifies the new state of the storage
     *            proxy.
     */
    protected void onStatusChange(int status)
    {
        this.status = status;
        if (status == StorageProxy.READY)
        {
            if (log.isDebugEnabled())
            {
                log.debug("StorageProxy onStatusChange :Ready State");
            }
            // When the Status is READY For the first time loads the
            // LogicalVolumes
            if (!firstTimeReadyFlag)
            {
                rootPath = nGetPath(nativeHandle);
                if (log.isDebugEnabled())
                {
                    log.debug("onStatusChange:RootPath:" + rootPath);
                }
                initializeCache();
                final String lsvRoot = rootPath + FILE_SEPARATOR + LSV_ROOT_DIRECTORY;
                AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        if (!createLSVRootDir(lsvRoot))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("onStatusChange:ROOT_LSV already present:");
                            }
                        }

                        // Notify the persistent storage manager so that it can
                        // load the
                        // volume attributes
                        PersistentStorageAttributes.getInstance().addStorageProxyMount(StorageProxyImpl.this);

                        return null;
                    }
                });
                loadVolumes();
                firstTimeReadyFlag = true;
            }
        }
        else if ((status == StorageProxy.NOT_PRESENT) || (status == StorageProxy.OFFLINE))
        {
            if (log.isDebugEnabled())
            {
                log.debug("StorageProxy onStatusChange :Not Present State or Offline");
            }

            // Remove all volumes associated with the media
            volumes.removeAllElements();

            // Reset other fields and general options
            initializeCache();

            // Reset for reloading device
            firstTimeReadyFlag = false;
        }
        else if (!firstTimeReadyFlag) // if it is the first time, lets get info
                                      // from native
        {
            rootPath = nGetPath(nativeHandle);
            if (log.isDebugEnabled())
            {
                log.debug("onStatusChange:RootPath:" + rootPath);
            }
            initializeCache();
        }
    }

    /**
     * This Method returns the root path for all LSVs.
     * 
     * @return returns the root path for all LSVs.
     */
    public String getLogicalStorageVolumeRootPath()
    {
        if (rootPath == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getLogicalStorageVolumeRootPath:ROOT PATH NULL");
            }
            return rootPath;
        }
        /*
         * if (Logging.LOGGING) {
         * log.debug("getLogicalStorageVolumeRootPath:ROOT PATH:" + rootPath +
         * FILE_SEPARATOR + LSV_ROOT_DIRECTORY + FILE_SEPARATOR); }
         */
        return rootPath + FILE_SEPARATOR + LSV_ROOT_DIRECTORY;
    }

    /**
     * Loads existing logical storage volume objects off the storage device.
     */
    void loadVolumes()
    {
        LogicalStorageVolume[] logicalStorageVolumes = LogicalStorageVolumeImpl.loadVolumes(this);
        if (log.isDebugEnabled())
        {
            log.debug("loadVolumes: After Load Volume number of volumes present is:" + logicalStorageVolumes.length);
        }
        for (int logicalVolumeCount = 0; logicalVolumeCount < logicalStorageVolumes.length; logicalVolumeCount++)
        {
            volumes.add(logicalStorageVolumes[logicalVolumeCount]);
        }
    }

    // Checks the permission for storageManager
    protected void checkMonitorPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));
    }

    /**
     * Create LSV Root directory if Root Directory is not Created previously.
     */
    private boolean createLSVRootDir(String lsvRoot)
    {
        boolean createdLSVRootDir = false;
        File lsvRootDir = new File(lsvRoot);
        if (!lsvRootDir.exists())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating LSV root directory " + lsvRootDir.getPath() + "...");
            }
            createdLSVRootDir = lsvRootDir.mkdirs();
        }
        return createdLSVRootDir;
    }

    // initialize all the stable attributes
    private void initializeCache()
    {
        name = nGetName(nativeHandle);
        displayName = nGetDisplayName(nativeHandle);
        totalSpace = nGetTotalSpace(nativeHandle);
        supportedAccessRights = nGetSupportedAccessRights(nativeHandle);
        if (log.isDebugEnabled())
        {
            log.debug("initializeCache: Device Name:" + name + ", DisplayName:" + displayName);
        }
        if (log.isDebugEnabled())
        {
            log.debug("initializeCache: totalSpace: " + totalSpace + ", Supported Rights" + supportedAccessRights[0]
                    + " " + supportedAccessRights[1] + " " + supportedAccessRights[2] + " " + supportedAccessRights[3]
                    + " " + supportedAccessRights[4] + " " + supportedAccessRights[5] + " ");
        }
    }

    // Creates Detach and removable storage options
    private void createStorageOptions()
    {
        if (nIsDetachable(nativeHandle))
        {
            if (log.isDebugEnabled())
            {
                log.debug("createStorageOptions:Is a DetachableStorageOption");
            }
            DetachableStorageOption detachableOption = new DetachableStorageOptionImpl(this);
            options.add(detachableOption);
        }

        if (nIsRemovable(nativeHandle))
        {
            if (log.isDebugEnabled())
            {
                log.debug("createStorageOptions:Is a RemovableStorageOption");
            }
            RemovableStorageOption removeableOption = new RemovableStorageOptionImpl(this);
            options.add(removeableOption);
        }
    }

    /*
     * Check if the calling application owns this volume
     */
    protected boolean checkOwnership(LogicalStorageVolume lsv)
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cctx = ccm.getCurrentContext();
        AppID callerAppid = (AppID) cctx.get(CallerContext.APP_ID);
        AppID ownerAppid = ((LogicalStorageVolumeImpl) lsv).getAppId();
        if (callerAppid != null && ownerAppid != null && callerAppid.equals(ownerAppid))
        {
            return true;
        }
        return false;
    }

    private void deleteAllVolumes()
    {
        for (Enumeration e = volumes.elements(); e.hasMoreElements();)
        {
            LogicalStorageVolumeImpl lsv = (LogicalStorageVolumeImpl) e.nextElement();
            lsv.delete();
        }
        volumes.clear();
    }

    protected void delete(LogicalStorageVolume lsv)
    {
        if (volumes.contains(lsv))
        {
            if (log.isDebugEnabled())
            {
                log.debug("deleteVolume:The volume exists :" + lsv.toString());
            }
            volumes.remove(lsv);
        }
    }

    /***************************************************************************
     * 
     * native methods
     * 
     **************************************************************************/

    /**
     * Called in the constructor of the object and cached for the remainder of
     * the life of the object
     * 
     * @param nativeStorageHandle
     *            used by the MPE layer to identify this device
     * @return String that identifies this device or device partition
     */
    private native String nGetDisplayName(int nativeStorageHandle);

    /**
     * Queried for at runtime when the public Java API is called by an
     * application this calls to the native layer to get the free space on the
     * device
     * 
     * @param nativeStorageHandle
     *            used by the MPE layer to identify this device
     * @return the amount of free space in bytes as a long
     */
    private native long nGetFreeSpace(int nativeStorageHandle);

    /**
     * Called once in the constructor of this object and cached for the
     * remainder of the life of the object
     * 
     * @param nativeStorageHandle
     *            used by the MPE layer to identify this device
     * @return the name of the device
     */
    private native String nGetName(int nativeStorageHandle);

    /**
     * Called in the constructor of the object and cached for the remainder of
     * the life of the object
     * 
     * @param nativeStorageHandle
     *            used by the MPE layer to identify this device
     * @return the total space on the device in bytes as a long
     */
    private native long nGetTotalSpace(int nativeStorageHandle);

    /**
     * This method retrieves the supported access rights of the device from the
     * native layer.
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @return boolean array of supported access rights of the device
     * @see StorageProxyImpl.getSupportedAccessRights()
     */

    private native boolean[] nGetSupportedAccessRights(int nativeStorageHandle);

    /**
     * This method reformats the specified storage device. It is called by the
     * StorageProxyImpl.initialize() method. Calling this method will trigger
     * status changes for the storage device. The device will enter the BUSY
     * state during the reformat operation and will end up either in the READY
     * state or the DEVICE_ERROR state when complete.
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @param userAuthorized
     *            - userAuthorized Flag
     * @return -Returns true if successfully initiated the format operation,
     *         false if the storage handle is invalid or the storage device is
     *         not in a appropriate state for formatting.
     */

    private native boolean nFormat(int nativeStorageHandle, boolean userAuthorized);

    /**
     * This Method is used to find if the device is detatchable
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @return - true if the device is Detachable
     */
    private native boolean nIsDetachable(int nativeStorageHandle);

    /**
     * This Method is used to find if the device is Removable
     * 
     * @param nativeStorageHandle
     *            - used by the MPE layer to identify this device
     * @return true if the device is Removable
     */

    private native boolean nIsRemovable(int nativeStorageHandle);

    /**
     * This method returns the path for the specified storage device
     * 
     * @param nativeStorageHandle
     *            used by the MPE layer to identify this device
     * @return path for the specified storage device
     */

    private native String nGetPath(int nativeStorageHandle);

}
