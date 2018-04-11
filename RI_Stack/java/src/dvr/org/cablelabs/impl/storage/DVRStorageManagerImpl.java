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

import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.recording.NavigationManager;
import org.cablelabs.impl.manager.recording.RecordingDestinationFilter;
import org.cablelabs.impl.manager.recording.RecordingImpl;
import org.cablelabs.impl.manager.recording.RecordingListImpl;
import org.cablelabs.impl.recording.MediaStorageVolumeReference;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageProxy;

/**
 * A <code>StorageManager</code> implementation that supports DVR.
 * 
 * @author Todd Earles
 */
public class DVRStorageManagerImpl extends StorageManagerImpl
{

    private static final Logger log = Logger.getLogger(DVRStorageManagerImpl.class);

    public final static int FREE_SPACE_ALARM = 0x1100;

    /**
     * Default Constructor this object.
     */
    public DVRStorageManagerImpl()
    {
        super();
        if (log.isDebugEnabled())
        {
            log.debug("DVRStorageManagerImpl :Registering for FREE_ALLRAM EVENT");
        }

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        // Register to get FREE_ALARM event
        nRegisterForMediaVolumeEvents();
    }

    /**
     * Factory method for creating a StorageProxy object. If the specified
     * device is DVR-capable, creates an instance of DVRStorageProxyImpl.
     * Otherwise, returns result of createStorageProxy() method of the super
     * class
     * 
     * @param nativeStorageHandle
     *            native media volume handle
     * @param status
     *            Current Status of the Device
     * 
     */
    protected StorageProxy createStorageProxy(int nativeStorageHandle, int status)
    {
        StorageProxy storageProxy = null;
        if (nIsDvrCapable(nativeStorageHandle))
        {
            if (log.isDebugEnabled())
            {
                log.debug("DVRStorageManagerImpl:createStorageProxy:Device is DVR Capable");
            }
            storageProxy = new DVRStorageProxyImpl(nativeStorageHandle, status);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("DVRStorageManagerImpl:createStorageProxy:Device is Not DVR Capable");
            }
            storageProxy = super.createStorageProxy(nativeStorageHandle, status);
        }
        return storageProxy;
    }

    /**
     * Receive for events dispatched by EventDispatcher.
     * 
     * @param eventCode
     *            event code
     * @param eventData1
     *            native media volume handle
     * @param eventData2
     *            threshold level that was crossed
     * 
     * @see org.cablelabs.impl.manager.ed.EDListener#asyncEvent
     */
    public synchronized void asyncEvent(int eventCode, int eventData1, int eventData2)
    {

        StorageManagerEvent event = null;
        if (log.isDebugEnabled())
        {
            log.debug(" ASync Event : EventCode:    " + eventCode + "    Native Handle  " + eventData1 + "  Status  "
                    + eventData2);
        }
        switch (eventCode)
        {
            case DVRStorageManagerImpl.FREE_SPACE_ALARM:
            {
                Enumeration storageProxies = storageProxyHolder.elements();
                while (storageProxies.hasMoreElements())
                {
                    StorageProxyImpl storageProxy = (StorageProxyImpl) storageProxies.nextElement();
                    if (storageProxy instanceof DVRStorageProxyImpl)
                    {
                        if (((DVRStorageProxyImpl) storageProxy).onFreeSpaceAlarm(eventData1, eventData2))
                        {
                            break;
                        }
                    }
                }
            }
            case StorageManagerEvent.STORAGE_PROXY_ADDED:
            {
                super.asyncEvent(eventCode, eventData1, eventData2);
                return;
            }
            case StorageManagerEvent.STORAGE_PROXY_REMOVED:
            {
                // Removes the StorageProxy object for the Handle and Notifies
                // to all Listeners.
                StorageProxyImpl storageProxy = (StorageProxyImpl) storageProxyHolder.remove(new Integer(eventData1));

                // In case storageProxyHolder does not contain a value for the given value of eventData1.
                if (storageProxy != null)
                {
                    // Filter for the case that the volumes still exist i.e. when
                    // the device is online
                    if (storageProxy.getStatus() == StorageProxy.READY || storageProxy.getStatus() == StorageProxy.BUSY)
                    {
                        // Append affected recordings
                        RecordingList rl = getAffectedRecordings(storageProxy);
                        event = new DVRStorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_REMOVED, rl);
                    }
                    else
                    {
                        event = new StorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_REMOVED);
                    }

                    // Set the state in the proxy to OFFLINE to signify the device
                    // does not exist
                    storageProxy.onStatusChange(StorageProxy.OFFLINE);
                    if (log.isDebugEnabled())
                    {
                        log.debug("asyncEvent: New Device Removed Name :" + storageProxy.getDisplayName());
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DVRStorageManagerImpl:asyncEvent:StorageProxy object is null and hence cannot be removed");
                    }
                }
                break;
            }
            case StorageManagerEvent.STORAGE_PROXY_CHANGED:
            {
                // Informs the storage proxy the status has been changed via
                // onStatusChange()and Notifies all Listeners.
                StorageProxyImpl storageProxy = (StorageProxyImpl) storageProxyHolder.get(new Integer(eventData1));

                // In case storageProxyHolder does not contain a value for the given value of eventData1.
                if (storageProxy != null)
                {
                    // If the device state is going to an offline or online state
                    if (eventData2 == StorageProxy.READY || eventData2 == StorageProxy.OFFLINE
                            || eventData2 == StorageProxy.NOT_PRESENT)
                    {
                        // Update recordings with the proper MSV references if
                        // device is going online
                        if (eventData2 == StorageProxy.READY)
                        {
                            storageProxy.onStatusChange((byte) eventData2);
                            updateRecordingInfoDestination(storageProxy);
                        }
                        RecordingList rl = getAffectedRecordings(storageProxy);
                        // Send an event
                        event = new DVRStorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_CHANGED, rl);
                    }
                    else
                    {
                        event = new StorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_CHANGED);
                    }
                    if (eventData2 != StorageProxy.READY)
                    {
                        storageProxy.onStatusChange((byte) eventData2);
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug("asyncEvent: Device Status Changed :" + storageProxy.getDisplayName());
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("DVRStorageManagerImpl:asyncEvent:StorageProxy object is null and hence cannot be changed");
                    }
                }
                break;
            }
            default:
            {
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent:Not a Valid Event");
                }
            }
        }
        if (event != null)
        {
            postEvent(event);
        }
    }

    /**
     * Checks to see if there is older references to the storage device in in
     * the recordings. If so, this method will update the MSV references if the
     * volume still exists. If not, the stale references are left alone.
     * 
     * @param storageProxy
     *            - the new storage device reference.
     */
    private void updateRecordingInfoDestination(StorageProxyImpl storageProxy)
    {
        if (log.isDebugEnabled())
        {
            log.debug("entered updateRecordingInfoDestination");
        }
        // Get all recordings in the database
        RecordingList rl = NavigationManager.getInstance().getEntries(null);

        // Get all recordings matching with the device Name in storageProxy
        for (int i = 0; i < rl.size(); i++)
        {
            RecordingRequest rr = rl.getRecordingRequest(i);
            if (rr instanceof RecordingImpl)
            {
                RecordingImpl ri = (RecordingImpl) rr;
                if (log.isDebugEnabled())
                {
                    log.debug("Updating recording " + ri.toString());
                }
                RecordingInfo2 ri2 = ri.getRecordingInfo();
                MediaStorageVolumeReference msvr = ri2.getMSVReference();
                msvr.updateMSV();
            }
        }
    }

    /**
     * Gets a list of RecordingRequests that has the storage device as the
     * destination.
     * 
     * @return Recording List - the list of recordings with the specified
     *         desination
     */
    private RecordingList getAffectedRecordings(StorageProxyImpl storageProxy)
    {
        if (log.isDebugEnabled())
        {
            log.debug("entered getAffectedRecordings");
        }
        // Get the volumes
        LogicalStorageVolume[] lsvs = storageProxy.getVolumes();
        // Create the filter and insert the MSV
        RecordingDestinationFilter rdf = new RecordingDestinationFilter();
        for (int i = 0; i < lsvs.length; i++)
        {
            if (lsvs[i] instanceof MediaStorageVolume)
            {
                MediaStorageVolume msv = (MediaStorageVolume) lsvs[i];

                if (log.isDebugEnabled())
                {
                    log.debug("Adding MSV to list: " + msv.toString());
                }
                rdf.addDestination(msv);
            }
        }
        if (rdf.getFilterValues().size() == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getAffectedRecordings: no MSVs found for StorageProxy");
            }
            return new RecordingListImpl();
        }
        // Pass back the recording with matching MSVs
        return NavigationManager.getInstance().getEntries(rdf);
    }

    // Native Method Declarations
    /**
     * This Method is used to find device is DVR Caoable
     * 
     * @param nativeStorageHandle
     *            -used by the MPE layer to identify this device
     * @return returns true if the specified device is capable of storing and
     *         playing back recorded media files.
     */
    private native boolean nIsDvrCapable(int nativeStorageHandle);

    /**
     * Registers interest in receiving media volume events
     * (FREE_SPACE_ALARM)from the MPE layer. Events will be delivered to the
     * asyncEvent() method.
     * 
     */
    private native void nRegisterForMediaVolumeEvents();
}
