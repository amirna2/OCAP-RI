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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ocap.storage.AvailableStorageListener;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.io.WriteableFileSys;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.filesys.QuotaFileSysIntf;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;

/**
 * The <code>StorageManager</code> implementation.
 * 
 * @author Todd Earles
 */
public class StorageManagerImpl extends StorageManagerExt implements EDListener
{

    private static final Logger log = Logger.getLogger(StorageManagerImpl.class);

    /**
     * The place holder of storageProxy, to keep track by storage manager key is
     * Native handle and Value is the storage Proxy of that native handle
     */
    protected Hashtable storageProxyHolder = null;

    /**
     * Multicast list of caller context objects for tracking listeners for this
     * storage manager. At any point in time this list will be the complete list
     * of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    /**
     * An object private to this storage manager object. This object is used for
     * synchronizing access to the ccList and as a key for caller context data
     * (CCData).
     */
    private Object implObject = new Object();

    private static String DEFAULT_DIR = "/syscwd";

    /**
     * Percentage of persistent storage used the last time
     * updatePersistentStorageUsage() was called.
     */
    private int persistentStorageUse;

    /**
     * Per caller context data
     */
    class CCData implements CallbackData
    {
        /**
         * The listeners is used to keep track of all objects that have
         * registered to be notified of storage manager events.
         */
        public volatile StorageManagerListener listeners;

        /**
         * listeners registered to be notified when available persistent storage
         * reaches a certain percentage used.
         */
        public volatile Vector asListeners = new Vector();

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
            synchronized (implObject)
            {
                // Remove this caller context from the list then throw away
                // the CCData for it.
                if (log.isDebugEnabled())
                {
                    log.debug("Entering in to Caller context Destruction");
                }
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(implObject);
                listeners = null;
                asListeners = null;
            }
        }
    }

    /**
     * Construct StorageManager object.
     */
	// Added for findbugs issues fix
	// Synchronization block not need inside constructor
    public StorageManagerImpl()
    {
        if (log.isDebugEnabled())
        {
                log.debug("Creating Instance of StorageManagerImpl");
        }
            storageProxyHolder = new Hashtable();

            try
            {
                initializeStorageProxies();
                nRegister();
            }
            catch (Throwable throwable)
            {
            if (log.isErrorEnabled())
            {
                    log.error("Failed to create StorageManager object", throwable);
            }
                throwable.printStackTrace();
            }
    }

    /**
     * @see org.ocap.storage.StorageManager#getStorageProxies
     */
    public StorageProxy[] getStorageProxies()
    {
        StorageProxy[] proxiesToReturn = new StorageProxy[storageProxyHolder.size()];
        /*
         * if (Logging.LOGGING) {
         * log.debug("Size of Storage Proxy list is :    " +
         * storageProxyHolder.size()); }
         */
        Enumeration proxies = storageProxyHolder.elements();
        synchronized (implObject)
        {
            int proxyCount = 0;
            while (proxies.hasMoreElements())
            {
                proxiesToReturn[proxyCount++] = (StorageProxy) proxies.nextElement();
            }
        }
        
        // Sort the storage proxies by their names.  This is not required by the
        // spec, but it provides some consistency of behavior across stack modules
        // that use the API.  This makes for a better developer and user experience
        Arrays.sort(proxiesToReturn, new Comparator() {
            public int compare(Object o1, Object o2)
            {
                StorageProxy s1 = (StorageProxy)o1;
                StorageProxy s2 = (StorageProxy)o2;

                int retVal = s1.getName().compareTo(s2.getName());
                if (retVal == 0)
                {
                    retVal = s1.getDisplayName().compareTo(s2.getDisplayName());
                }
                return retVal;
            }
        });

        return proxiesToReturn;
    }

    /**
     * @see org.ocap.storage.StorageManager#addStorageManagerListener
     */
    public void addStorageManagerListener(StorageManagerListener listener) throws IllegalArgumentException
    {
        // Add the listener to the list of listeners for this caller context.
        if (log.isDebugEnabled())
        {
            log.debug(" Adding Storage Manager Listener");
        }

        if (listener == null)
            throw new IllegalArgumentException();

        synchronized (implObject)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cc = ccm.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = EventMulticaster.add(data.listeners, listener);
        }
        if (log.isDebugEnabled())
        {
            log.debug(" Added Storage Manager Listener Succes Fully");
        }
    }

    /**
     * @see org.ocap.storage.StorageManager#removeStorageManagerListener
     */
    public void removeStorageManagerListener(StorageManagerListener listener) throws IllegalArgumentException
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        if (log.isDebugEnabled())
        {
            log.debug(" Removeing Storage Manager Listener");
        }

        if (listener == null)
            throw new IllegalArgumentException();

        synchronized (implObject)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cc = ccm.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = EventMulticaster.remove(data.listeners, listener);
        }
        if (log.isDebugEnabled())
        {
            log.debug(" Removed Storage Manager Listener Successfully");
        }
    }

    /**
     * Receive for events dispatched by EventDispatcher.
     * 
     * @param eventCode
     *            event code
     * @param eventData1
     *            Device Native Handle
     * @param eventData2
     *            current Status of device
     * 
     * @see EDListener#asyncEvent
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
            case StorageManagerEvent.STORAGE_PROXY_ADDED:
            {
                // Creates the new StorageProxy object, stores the Object and
                // Notify
                // to all Listeners.
                StorageProxy storageProxy = createStorageProxy(eventData1, (byte) eventData2);
                storageProxyHolder.put(new Integer(eventData1), storageProxy);
                event = new StorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_ADDED);
                if (log.isDebugEnabled())
                {
                    log.debug(" asyncEvent:New Device added, Name :" + storageProxy.getDisplayName());
                }
                break;
            }
            case StorageManagerEvent.STORAGE_PROXY_REMOVED:
            {
                // Removes the StorageProxy object for the Handle and Notifies
                // to
                // all Listeners.
                StorageProxyImpl storageProxy = (StorageProxyImpl) storageProxyHolder.get(new Integer(eventData1));
                storageProxyHolder.remove(new Integer(eventData1));
                // Set the state in the proxy to OFFLINE to signify the device
                // does not exist
                storageProxy.onStatusChange(StorageProxy.OFFLINE);
                event = new StorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_REMOVED);
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent: New Device Removed Name :" + storageProxy.getDisplayName());
                }
                break;
            }
            case StorageManagerEvent.STORAGE_PROXY_CHANGED:
            {
                // Informs the storage proxy the status has been changed via
                // onStatusChange()and Notifies all Listeners.
                StorageProxyImpl storageProxy = (StorageProxyImpl) storageProxyHolder.get(new Integer(eventData1));
                storageProxy.onStatusChange((byte) eventData2);
                event = new StorageManagerEvent(storageProxy, StorageManagerEvent.STORAGE_PROXY_CHANGED);
                if (log.isDebugEnabled())
                {
                    log.debug("asyncEvent: Device Status Changed :" + storageProxy.getDisplayName());
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
     * Factory method for creating a StorageProxy object. Creates an instance of
     * StorageProxyImpl
     * 
     * @param nativeStorageHandle
     *            - native handle
     * @param status
     *            - status of device
     * @return StorageProxy Object
     */
    protected StorageProxy createStorageProxy(int nativeStorageHandle, int status)
    {
        return new StorageProxyImpl(nativeStorageHandle, status);
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
        synchronized (implObject)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(implObject);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, implObject);
                ccList = CallerContext.Multicaster.add(ccList, cc);
            }
            return data;
        }
    }

    /**
     * Post the specified event to all listeners.
     */
    void postEvent(final StorageManagerEvent event)
    {
        // Notify all listeners. Use a local copy of the ccList so it does not
        // change while we are using it.
        CallerContext ccList = this.ccList;
        if (ccList != null)
        {
            // Execute the runnable in each caller context in ccList
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    // Notify listeners. Use a local copy of data so that it
                    // does not change while we are using it.
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    CallerContext cc = ccm.getCurrentContext();
                    CCData data = getCCData(cc);
                    if ((data != null) && (data.listeners != null)) data.listeners.notifyChange(event);
                }
            });
        }
    }

    /**
     * Queries the List of Devices , Creates the StorageProxy for the Devices,
     * Store it in the internal DataStructure.
     * 
     */
    private void initializeStorageProxies()
    {
        Vector deviceList = new Vector();
        nGetDevices(deviceList);
        int handle = 0;
        int status = 0;
        int deviceCount = deviceList.size();
        if (log.isDebugEnabled())
        {
            log.debug(" Device List Count   :" + deviceCount);
        }
        for (int deviceListCount = 0; deviceListCount < deviceCount; deviceListCount++)
        {
            handle = ((Integer) deviceList.elementAt(deviceListCount)).intValue();
            status = nGetStatus(handle);
            StorageProxy storageProxy = createStorageProxy(handle, status);
            storageProxyHolder.put(new Integer(handle), storageProxy);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.storage.StorageManager#addAvailableStorageListener(org.ocap.
     * storage.AvailableStorageListener, int)
     */
    public void addAvailableStorageListener(AvailableStorageListener listener, int highWaterMark)
    {
        // Add the listener to the list of listeners for this caller context.
        if (listener == null) throw new IllegalArgumentException();

        synchronized (implObject)
        {
            int index = 0;
            // Get the data for the current caller context.
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CCData data = getCCData(ccm.getCurrentContext());

            // ensure that listener already isn't in the list
            for (int i = 0; i < data.asListeners.size(); i++)
            {
                ListenerData ld = (ListenerData) data.asListeners.elementAt(i);
                if (ld.listener == listener)
                {
                    // this listener already exists so return
                    // TODO: should the listener be re-registered if the
                    // highWaterMark is different?
                    return;
                }
                // maintain the list of listeners sorted by increasing
                // highWaterMark value
                if (highWaterMark > ld.highWaterMark)
                {
                    index = i + 1;
                }
            }

            ListenerData ld = new ListenerData(listener, highWaterMark);
            // Add caller's listener to list of listeners.
            data.asListeners.add(index, ld);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.storage.StorageManager#getAvailablePersistentStorage()
     */
    public long getAvailablePersistentStorage()
    {
        FileManager mgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        WriteableFileSys wfs = mgr.getWriteableFileSys(MPEEnv.getSystemProperty("dvb.persistent.root", DEFAULT_DIR));
        long usage = 0;

        if (wfs instanceof QuotaFileSysIntf)
        {
            QuotaFileSysIntf qfs = (QuotaFileSysIntf) wfs;
            usage = qfs.getQuota() - qfs.getUsage();
        }

        return usage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.storage.StorageManager#getTotalPersistentStorage()
     */
    public long getTotalPersistentStorage()
    {
        FileManager mgr = (FileManager) ManagerManager.getInstance(FileManager.class);
        WriteableFileSys wfs = mgr.getWriteableFileSys(MPEEnv.getSystemProperty("dvb.persistent.root", DEFAULT_DIR));
        long total = 0;

        if (wfs instanceof QuotaFileSysIntf)
        {
            QuotaFileSysIntf qfs = (QuotaFileSysIntf) wfs;
            total = qfs.getQuota();
        }
        return total;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.storage.StorageManager#removeAvailableStorageListener(org.ocap
     * .storage.AvailableStorageListener)
     */
    public void removeAvailableStorageListener(AvailableStorageListener listener)
    {
        if (listener == null) throw new IllegalArgumentException();

        // Remove the listener from the list of listeners for this caller
        // context.
        synchronized (implObject)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CCData data = getCCData(ccm.getCurrentContext());

            // Remove the caller from the list of listeners.
            for (int i = 0; i < data.asListeners.size(); i++)
            {
                // TODO: this doesn't need to iterate over the listeners!
                ListenerData ld = (ListenerData) data.asListeners.elementAt(i);
                if (ld.listener == listener)
                {
                    data.asListeners.removeElementAt(i);
                    break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.storage.StorageManagerExt#updatePersistentStorageUsage
     * (int)
     */
    public void updatePersistentStorageUsage(int highWaterValue)
    {
        // only attempt to notify listeners if the amount of persistent storage
        // use
        // increased. If the value decreased because of a purge, then listeners
        // will be notified when the usage starts to go up and the high water
        // mark for each
        // listener is reached.
        if (highWaterValue > persistentStorageUse)
        {
            notifyAvailableStorageListeners(highWaterValue);
        }

        persistentStorageUse = highWaterValue;
    }

    /**
     * Notify <code>AvailableStorageListeners</code> that a high water mark
     * value was reached.
     * 
     * @param highWaterValue
     *            value that is the percentage of persistent storage used.
     */
    void notifyAvailableStorageListeners(final int highWaterValue)
    {
        final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (ccList != null)
        {
            final int lastHighWaterValue = persistentStorageUse;
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    synchronized (implObject)
                    {
                        Vector listeners;
                        if ((listeners = getCCData(ccm.getCurrentContext()).asListeners) != null)
                        {
                            // Invoke listeners
                            for (int i = 0; i < listeners.size(); i++)
                            {
                                // we only invoke a listener once for a high
                                // water mark value. Listener A
                                // wants to be notified at 75% full and listener
                                // B wants to be notified at 90% full.
                                // So when we hit 80% full, the listener A is
                                // notified. On the next write we
                                // hit 90% full, only listener B is notified.
                                ListenerData ld = (ListenerData) listeners.elementAt(i);
                                if (ld.highWaterMark <= highWaterValue)
                                {
                                    if (ld.highWaterMark > lastHighWaterValue)
                                    {
                                        ld.listener.notifyHighWaterMarkReached();
                                    }
                                }
                                else
                                {
                                    // listeners are inserted in ascending order
                                    // so we don't have
                                    // to iterate the whole list.
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class ListenerData
    {
        public ListenerData(AvailableStorageListener listener, int hwm)
        {
            this.listener = listener;
            highWaterMark = hwm;
        }

        public boolean equals(Object other)
        {
            // verify a valid object was received
            if (other == null || !(other instanceof ListenerData))
            {
                return false;
            }

            // compare data fields
            ListenerData data = (ListenerData) other;
            return (this.listener == data.listener);
            // && this.highWaterMark == data.highWaterMark);
        }

        AvailableStorageListener listener;

        int highWaterMark;
    }

    /***************************************************************************
     * 
     * native methods
     * 
     **************************************************************************/

    /**
     * Register a new listener with the OCAP Event Dispatcher so that we may be
     * notified of storage devices being attached or detahced. Once we register
     * ourselves in the JNI layer we will be notified via the asyncEvent method.
     */
    private native void nRegister();

    /**
     * Retrieves list of storage devices as a vector of native storage device
     * handles.
     * 
     * @param deviceList
     *            the Vector to populate with device handles
     */
    private native void nGetDevices(Vector deviceList);

    /**
     * Returns the current status of the specified storage device. Possible
     * values are: StorageProxy.BUSY StorageProxy.DEVICE_ERROR
     * StorageProxy.NOT_PRESENT StorageProxy.OFFLINE StorageProxy.READY
     * StorageProxy.UNINITIALIZED StorageProxy.UNSUPPORTED_DEVICE
     * StorageProxy.UNSUPPORTED_FORMAT
     * 
     * @param nativeStorageHandle
     *            specifies the storage device to query
     * @return the current status
     */
    private native int nGetStatus(int nativeStorageHandle);
}
