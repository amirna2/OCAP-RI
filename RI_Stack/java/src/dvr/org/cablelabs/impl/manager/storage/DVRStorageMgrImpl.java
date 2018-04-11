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

package org.cablelabs.impl.manager.storage;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.storage.DVRStorageManagerImpl;

import org.ocap.dvr.storage.MediaStorageVolume;

/**
 * The <code>StorageManager</code> implementation for platforms that support
 * DVR.
 * 
 * @author Todd Earles
 */
public class DVRStorageMgrImpl implements DVRStorageManager
{
    private final CallerContextManager m_ccm;

    /**
     * Construct this object.
     */
    public DVRStorageMgrImpl()
    {
        // TODO(Todd): Determine storage devices in the system. Requires
        // native support.
        m_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    }

    /**
     * Returns an instance of this <code>StorageManager</code>. Intended to be
     * called by the {@link org.cablelabs.impl.manager.ManagerManager
     * ManagerManager} only and not called directly. The singleton instance of
     * this manager is maintained by the <code>ManagerManager</code>.
     * 
     * @return an instance of the <code>StorageManager</code>.
     * 
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public static synchronized Manager getInstance()
    {
        return new DVRStorageMgrImpl();
    }

    /**
     * Destroys this manager, causing it to release any and all resources.
     * Should only be called by the <code>ManagerManager</code>.
     */
    public void destroy()
    {
        // TODO(Todd): Release all resources
    }

    /**
     * Adds a listener to receive notification upon recording state changes and
     * disables.
     * 
     * @param listener
     */
    public void addMediaStorageVolumeUpdateListener(MediaStorageVolumeUpdateListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("adding MediaStorageVolumeUpdateListener: " + listener);
        }

        synchronized (m_sync)
        {
            if (!m_msvListeners.contains(listener))
            {
                m_msvListeners.add(listener);
            }
        }
    }

    /**
     * Removes a recording change listener
     * 
     * @param listener
     */
    public void removeMediaStorageVolumeUpdateListener(MediaStorageVolumeUpdateListener listener)
    {
        synchronized (m_sync)
        {
            m_msvListeners.remove(listener);
        }
    }

    /**
     * Notifies the StorageManager that a media storage volume has been enabled
     * or disabled
     * 
     * @param msv
     *            the MediaStorageVolume
     */
    public void notifyMediaVolumeAccessStateChanged(MediaStorageVolume msv)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyMediaVolumeAccessStateChanged: " + msv);
        }
        synchronized (m_sync)
        {
            // notify all listeners that this MSV is being disabled
            for (int i = 0; i < m_msvListeners.size(); i++)
            {
                final MediaStorageVolumeUpdateListener msvul = (MediaStorageVolumeUpdateListener) m_msvListeners.elementAt(i);
                final MediaStorageVolume volume = msv;
                m_ccm.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("calling MediaStorageVolumeUpdateListener: " + msvul);
                        }
                        msvul.notifyVolumeAccessStateChanged(volume);
                    }
                });
            }
        }
    }
    
    /**
     * Notifies MediaStorageVolumeUpdateListeners that the available space has changed on the
     * MSV.
     * 
     * @param msv
     *            the MediaStorageVolume
     */
    public void notifyMediaVolumeSpaceAvailable(MediaStorageVolume msv) 
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyMediaVolumeSpaceAvailable: " + msv);
        }
        synchronized (m_sync)
        {
            // notify all listeners that space availability has changed on this MSV
            for (int i = 0; i < m_msvListeners.size(); i++)
            {
                final MediaStorageVolumeUpdateListener msvul = (MediaStorageVolumeUpdateListener) 
                                                               m_msvListeners.elementAt(i);
                final MediaStorageVolume volume = msv;
                m_ccm.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("calling MediaStorageVolumeUpdateListener: " + msvul);
                        }
                        msvul.notifySpaceAvailable(volume);
                    }
                });
            }
        }
    }

    // Description copied from StorageManager
    public org.ocap.storage.StorageManager getStorageManager()
    {
        return new DVRStorageManagerImpl();
    }

    private Object m_sync = new Object();

    private Vector m_msvListeners = new Vector();

    /** logging object */
    private static final Logger log = Logger.getLogger(DVRStorageMgrImpl.class);

}
