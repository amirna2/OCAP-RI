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

package org.cablelabs.impl.recording;

import org.dvb.application.AppID;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import org.apache.log4j.Logger;
import org.cablelabs.impl.storage.LogicalStorageVolumeExt;

/*
 * This object shall allow recordings to store data located in the MediaStorageVolume
 * that is pertinent for lookup of the MediaStorageVolumes in detachment or in system startup
 */
public class MediaStorageVolumeReference
{
    AppID m_appId;

    String m_volumeName;

    String m_deviceName;

    MediaStorageVolume m_msv;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(MediaStorageVolumeReference.class.getName());

    public MediaStorageVolumeReference()
    {
        m_appId = null;
        m_volumeName = null;
        m_deviceName = null;
        m_msv = null;
    }

    public MediaStorageVolumeReference(MediaStorageVolume msv)
    {
        if (msv == null)
        {
            m_appId = null;
            m_volumeName = null;
            m_deviceName = null;
            m_msv = null;
        }
        else
        {
            m_appId = ((LogicalStorageVolumeExt) msv).getAppId();
            m_volumeName = ((LogicalStorageVolumeExt) msv).getName();
            m_deviceName = msv.getStorageProxy().getName();
            m_msv = msv;
        }
    }

    public MediaStorageVolumeReference(AppID appId, String volumeName, String deviceName)
    {
        m_appId = appId;
        m_volumeName = volumeName;
        m_deviceName = deviceName;
        m_msv = null;
    }

    public MediaStorageVolume getMSV()
    {
        return m_msv;
    }

    public String getDeviceName()
    {
        return m_deviceName;
    }

    public String getVolumeName()
    {
        return m_volumeName;
    }

    public AppID getAppID()
    {
        return m_appId;
    }

    public void clearMSV()
    {
        m_msv = null;
    }

    /**
     * Based on the stored device name and volume name, the
     * <code>MediaStorageVolume</code> object will be refreshed. This is
     * unconditional and will write over the previous reference. If no
     * associated volume is present in <code>StorageManager</code>, the
     * <code>MediaStorageVolume</code> is set to null
     */
    public void updateMSV()
    {
        StorageProxy[] storageProxyList = StorageManager.getInstance().getStorageProxies();
        for (int i = 0; i < storageProxyList.length; i++)
        {
            // Match with a device in the list
            if (storageProxyList[i].getName().equals(m_deviceName))
            {
                LogicalStorageVolume[] lsvS = storageProxyList[i].getVolumes();
                for (int j = 0; j < lsvS.length; j++)
                {
                    if (lsvS[j] instanceof MediaStorageVolume)
                    {
                        // Match the volume Name with one of the MSVs in
                        // storageProxy
                        if (m_volumeName.equals(((LogicalStorageVolumeExt) lsvS[j]).getName()))
                        {
                            m_msv = (MediaStorageVolume) lsvS[j];
                            if (log.isDebugEnabled())
                            {
                                log.debug("Updated MSV reference: " + m_msv.toString());
                            }
                            return;
                        }
                    }
                }
            }
        }
        // If no match is found, null out the reference
        if (log.isDebugEnabled())
        {
            log.debug("Setting MSV reference to NULL");
        }
        m_msv = null;
    }

    /**
     * Overrides {@link Object#equals}. This is mostly in-place for testing.
     * 
     * @return <code>true</code> if all fields match, <code>false</code>
     *         otherwise
     */
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof MediaStorageVolumeReference)) return false;

        MediaStorageVolumeReference other = (MediaStorageVolumeReference) obj;

        if (m_appId == null ? other.m_appId != null : !m_appId.equals(other.m_appId)) return false;
        if (m_volumeName == null ? other.m_volumeName != null : !m_volumeName.equals(other.m_volumeName)) return false;
        if (m_deviceName == null ? other.m_deviceName != null : !m_deviceName.equals(other.m_deviceName)) return false;
        if (m_msv == null ? other.m_msv != null : !m_msv.equals(other.m_msv)) return false;

        return true;
    }
    
    //Not likely that hashCode is needed for this class...
    //Quiets down findbugs.
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (m_appId == null ? 0 : m_appId.hashCode());
        hash = 31 * hash + (m_volumeName == null ? 0 : m_volumeName.hashCode());
        hash = 31 * hash + (m_deviceName == null ? 0 : m_deviceName.hashCode());
        hash = 31 * hash + (m_msv == null ? 0 : m_msv.hashCode());
        return hash;
    }

} // END class MediaStorageVolumeReference
