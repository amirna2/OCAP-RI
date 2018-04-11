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
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageProxy;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.security.PersistentStorageAttributes;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The <code>LogicalStorageVolume</code> implementation.
 * 
 * @author Todd Earles
 */
public class LogicalStorageVolumeImpl implements LogicalStorageVolumeExt
{
    private static final Logger log = Logger.getLogger(LogicalStorageVolumeImpl.class);

    private static PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();

    private String volumeName;

    // The storage proxy to which this volume belongs
    private StorageProxy m_proxy;

    // The directory that is the LSV
    private File lsvPathName;
    
    private AppID owner;

    private static final int HEX_BASE = 16;

    /**
     * This is the constructor used to recreate an LSV object representing an
     * LSV that already exists on the hard drive.
     * 
     * @param proxy
     *            - Proxy to which the LSV should be associated
     * @param path
     *            - Path for the LSV
     * @throws IOException if the LSV owner could not be read from persistent
     *         sotrage attributes
     */
    LogicalStorageVolumeImpl(StorageProxyImpl proxy, String path)
        throws IOException
    {
        m_proxy = proxy;
        lsvPathName = new File(path);
        volumeName = lsvPathName.getName();
        
        owner = psa.getOwner(path);
    }

    /**
     * This constructor is used to create a new LSV that does not already exist
     * on the storage device. Uses org.dvb.persistent.io.FileAttributes to store
     * the EFAP for this directory. The path for the LSV will have the following
     * form: OCAP_LSV/<orgId>/<appId>/<volume_name>
     * 
     * @param proxy
     *            - Proxy to which the LSV should be associated
     * @param name
     *            - Name of the Volume
     * @param owner
     *            - Owner of the LSV
     * @param perms
     *            - Permission For the Files
     * 
     * @throws IllegalArgumentException
     *             if the name does not meet the form specified by the section
     *             in chapter 16 of [1] regarding Files and File Names, or if
     *             the name is not unique, or if the storage device does not
     *             support an access permission specified in the fap parameter.
     * 
     * @throws IOException
     *             if the storage device represented by the StorageProxy is
     *             read-only based on a hardware constraint.
     * 
     * @throws SecurityException
     */
    LogicalStorageVolumeImpl(StorageProxyImpl proxy, String name, final AppID owner,
            final ExtendedFileAccessPermissions perms) throws IOException, SecurityException
    {
        volumeName = name;
        m_proxy = proxy;

        final String rootPath = ((StorageProxyImpl) this.m_proxy).getLogicalStorageVolumeRootPath();
        final String oid = Integer.toString(owner.getOID(), HEX_BASE);
        final String aid = Integer.toString(owner.getAID(), HEX_BASE);
        final String path = rootPath + File.separator + oid + File.separator + aid + File.separator + name;

        if (log.isDebugEnabled())
        {
            log.debug("Creating new LSV with path " + path + "...");
        }

        lsvPathName = new File(path);

        // Make sure appID, orgID, and volume directories are created with
        // correct permissions
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                createLSVDirectories(rootPath, oid, aid, owner, perms);
                return null;
            }
        });
    }

    private void createLSVDirectories(String rootPath, String oid, String aid, AppID owner,
            ExtendedFileAccessPermissions perms)
    {
        // Does it exist already -- error
        if (lsvPathName.exists()) throw new IllegalArgumentException("LSV ALREADY CREATED");

        try
        {
            // Create Org ID dir and set Org as owner and org read only
            File orgDir = new File(rootPath + File.separator + oid);
            if (!orgDir.exists())
            {
                orgDir.mkdir();
                psa.setOwner(orgDir.getAbsolutePath(), new AppID(owner.getOID(),
                        PersistentStorageAttributes.IMPL_APP_ID));
                psa.setFileAttributes(orgDir.getCanonicalPath(), new FileAccessPermissions(false, false, true, false,
                        false, false), null, -1, true);
            }

            // Create App ID dir and set Org as owner and org read only
            File appDir = new File(rootPath + File.separator + oid + File.separator + aid);
            if (!appDir.exists())
            {
                appDir.mkdir();
                psa.setOwner(appDir.getAbsolutePath(), new AppID(owner.getOID(),
                        PersistentStorageAttributes.IMPL_APP_ID));
                psa.setFileAttributes(appDir.getCanonicalPath(), new FileAccessPermissions(false, false, true, false,
                        false, false), null, -1, true);
            }

            // Create LSV dir and set the app as owner and the perms it
            // specified
            if (!lsvPathName.exists())
            {
                lsvPathName.mkdir();
                psa.setOwner(lsvPathName.getCanonicalPath(), owner);
                psa.setFileAttributes(lsvPathName.getCanonicalPath(), perms, null, -1, true);
            }
            
            this.owner = owner;
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error setting attributes for appID/orgID/LSV directories for new LSV!");
            }
        }
    }

    // Description copied from LogicalStorageVolume
    public String getPath()
    {
        return lsvPathName.getPath();
    }

    // Description copied from LogicalStorageVolume
    public StorageProxy getStorageProxy()
    {
        return m_proxy;
    }

    // Description copied from LogicalStorageVolume
    public ExtendedFileAccessPermissions getFileAccessPermissions()
    {
        try
        {
            FileAccessPermissions perm = psa.getFilePermissions(lsvPathName.getCanonicalPath());
            if (perm instanceof ExtendedFileAccessPermissions) return (ExtendedFileAccessPermissions) perm;

            return new ExtendedFileAccessPermissions(perm.hasReadWorldAccessRight(), perm.hasWriteWorldAccessRight(),
                    perm.hasReadOrganisationAccessRight(), perm.hasWriteOrganisationAccessRight(),
                    perm.hasReadApplicationAccessRight(), perm.hasWriteApplicationAccessRight(), null, null);
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error getting file attributes for LSV: " + lsvPathName.getAbsolutePath());
            }
            return null;
        }
    }

    // Description copied from LogicalStorageVolume
    public void setFileAccessPermissions(ExtendedFileAccessPermissions fap)
    {
        boolean doPrivileged = true;
        try
        {
            SecurityUtil.checkPermission(new MonitorAppPermission("storage"));
        }
        catch (SecurityException e1)
        {
            doPrivileged = false;
        }

        try
        {
            psa.setFileAttributes(lsvPathName.getAbsolutePath(), fap, null, -1, doPrivileged);
        }
        catch (IOException e)
        {
            throw new SecurityException("Could not set file access permissions on LSV: "
                    + lsvPathName.getAbsolutePath());
        }
    }

    // Description copied from LogicalStorageVolumeExt
    public AppID getAppId()
    {
        return owner;
    }

    // Description copied from LogicalStorageVolumeExt
    public String getName()
    {
        return volumeName;
    }

    // Description copied from LogicalStorageVolumeExt
    public boolean isMediaStorageVolume()
    {
        return false;
    }

    // Description copied from LogicalStorageVolumeExt
    public void delete()
    {
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                FileManager.Util.deleteFiles(lsvPathName);
                return null;
            }
        });
    }

    /**
     * This is a static method for recreating LogicalStorageVolume objects for
     * volumes that are already present in persistent storage on the specified
     * device.
     * 
     * @param proxy
     *            - Proxy to which the LSV should be associated
     * @return - All available LSVs
     */
    protected static LogicalStorageVolume[] loadVolumes(StorageProxyImpl proxy)
    {
        LogicalStorageVolume[] lsvToReturn = new LogicalStorageVolume[0];

        if (log.isDebugEnabled())
        {
            log.debug("Loading LSVs...");
        }
        Vector lsvPaths = createAllLogicalVolumes(proxy.getLogicalStorageVolumeRootPath());
        int totLSVs = lsvPaths.size();
        if (totLSVs > 0)
        {
            // We could still fail to create these LSVs, so just consider them candidates
            // until we have actually loaded all data
            Vector lsvCandidates = new Vector(totLSVs);
            for (int pathCount = 0; pathCount < totLSVs; pathCount++)
            {
                String path = lsvPaths.elementAt(pathCount).toString();
                try
                {
                    lsvCandidates.add(new LogicalStorageVolumeImpl(proxy, path));
                }
                catch (IOException e)
                {
                    SystemEventUtil.logRecoverableError("Could not load existing LSV!", e);
                }
            }
            
            // Create our actual list from all valid candidates
            lsvToReturn = new LogicalStorageVolume[lsvCandidates.size()];
            lsvCandidates.copyInto(lsvToReturn);
        }
        return lsvToReturn;
    }

    // Create all Logical volumes
    private static Vector createAllLogicalVolumes(final String path)
    {
        return (Vector) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                Vector persistentPaths = new Vector();
                File lsvRoot = new File(path);
                File[] orgIds = lsvRoot.listFiles();
                if (orgIds != null)
                {
                    for (int ordIdCount = 0; ordIdCount < orgIds.length; ordIdCount++)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(" Scanning LSVs for org " + orgIds[ordIdCount] + "...");
                        }
                        File[] appIds = orgIds[ordIdCount].listFiles();
                        if (appIds != null)
                        {
                            for (int appIdCount = 0; appIdCount < appIds.length; appIdCount++)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("  Scanning LSVs for app " + appIds[appIdCount] + "...");
                                }
                                File[] volumes = appIds[appIdCount].listFiles();
                                if (volumes != null)
                                {
                                    for (int volumeCount = 0; volumeCount < volumes.length; volumeCount++)
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("   Found LSV " + volumes[volumeCount].getPath());
                                        }
                                        persistentPaths.add(volumes[volumeCount].getPath());
                                    }
                                }
                            }
                        }
                    }
                }
                return persistentPaths;
            }
        });
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof LogicalStorageVolumeImpl))
        {
            return false;
        }

        if (this.getPath().equals(((LogicalStorageVolumeImpl) o).getPath()))
        {
            return true;
        }
        return false;
    }

    public int hashCode()
    {
        return 19 + this.getPath().hashCode();
    }

    static
    {
        // Make sure the file manager framework is active.
        ManagerManager.getInstance(FileManager.class);
    }
}
