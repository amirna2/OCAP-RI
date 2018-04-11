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

package org.cablelabs.impl.media.presentation;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceCallback;
import org.davic.net.tuning.NotOwnerException;

/**
 * Registers for changes tune lock acquired/lost and SDV remap updates.
 * <p>
 * Does not support -initial- tune pending/tunecomplete notifications, only retune pending/retune complete and sync notifications.
 * <p/>
 * Notifies the registered {@link NetworkConditionListener} of any updates.
 */
public class NetworkConditionMonitor
{
    private static final Logger log = Logger.getLogger(NetworkConditionMonitor.class);

    private NetworkConditionListener networkConditionListener;

    private ExtendedNetworkInterface networkInterface;

    private NetworkInterfaceCallbackImpl networkInterfaceCallback;

    private final Object lock;

    private boolean initialized;
    private boolean syncNotificationsOnly;
    private Object tuneToken;

    /**
     * Constructor
     * 
     * @param lock
     *            the shared lock
     * @param networkConditionListener
     *            the listener that will receive notifications of network
     *            condition changes
     * @param syncNotificationsOnly if true, only report sync lost/acquired NetworkConditionEvents
     */
    public NetworkConditionMonitor(Object lock, NetworkConditionListener networkConditionListener, boolean syncNotificationsOnly)
    {
        this.lock = lock;
        this.networkConditionListener = networkConditionListener;
        this.syncNotificationsOnly = syncNotificationsOnly;
        networkInterfaceCallback = new NetworkInterfaceCallbackImpl();
    }

    /**
     * Initialize the networkConditionMonitor
     *
     *
     * @param networkInterface
     *
     * @throws IllegalArgumentException if NetworkInterface is null
     */
    public void initialize(ExtendedNetworkInterface networkInterface)
    {
        if (networkInterface == null)
        {
            throw new IllegalArgumentException("NetworkConditionMonitor provided a null NetworkInterface");
        }
        synchronized (lock)
        {
            this.networkInterface = networkInterface;
            tuneToken = this.networkInterface.getCurrentTuneToken();
            // add these listeners once
            if (!initialized)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initialize - first initialization");
                }
                // register tune lock listener
                networkInterface.addNetworkInterfaceCallback(networkInterfaceCallback, 30);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initialize - subsequent initialization");
                }
            }
            initialized = true;
        }
    }

    /**
     * Release resources. Safe to be called multiple times.
     */
    public void cleanup()
    {
        synchronized (lock)
        {
            // don't clean up if not initialized
            if (!initialized)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("cleanup called when not initialized - ignoring");
                }
                return;
            }
            if (log.isDebugEnabled())
            {
                log.debug("cleanup: " + this);
            }
            networkInterface.removeNetworkInterfaceCallback(networkInterfaceCallback);

            initialized = false;
            networkInterface = null;
        }
    }

    public boolean isNetworkSyncLost()
    {
        synchronized (lock)
        {
            if (!initialized)
            {
                throw new IllegalStateException("isNetworkSyncLost called when not initialized");
            }
            //NI may not even be reserved...just trigger altcontent if not 'synced'
            try {
                return !networkInterface.isSynced(tuneToken);
            } catch (NotOwnerException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
            return true;
        }
    }

    public String toString()
    {
        return "Network condition monitor - initialized: " + initialized
                + " - " + super.toString();
    }

    private class NetworkInterfaceCallbackImpl implements NetworkInterfaceCallback
    {
        private boolean tunePending = false;

        public void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            //no-op - will already be tuned prior to NetworkConditionMonitor initialization
        }

        public void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean isSynced)
        {
            //no-op - will already be tuned prior to NetworkConditionMonitor initialization
        }

        public void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            if (log.isInfoEnabled())
            {
                log.info("received notifyRetunePending");
            }
            NetworkConditionListener tempNetworkConditionListener;
            
            synchronized (lock)
            {
                if (!initialized || syncNotificationsOnly)
                {
                    return;
                }
                tempNetworkConditionListener = networkConditionListener;

            }
            
            // processing must occur synchronously (release NI)
            tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.RETUNE_PENDING);
        }

        public void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success,
                boolean isSynced)
        {
            // we will also receive this when recovering from service
            // remap-based retune
            NetworkConditionListener tempNetworkConditionListener;
            synchronized (lock)
            {
                if (!initialized || syncNotificationsOnly)
                {
                    return;
                }
                tempNetworkConditionListener = networkConditionListener;
                if (log.isInfoEnabled())
                {
                    log.info("received notifyRetuneComplete - success: " + success + ", sync: " + isSynced);
                }
            }
            if (success)
            {
                if (isSynced)
                {
                    tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.TUNE_SYNC_ACQUIRED);
                }
                else
                {
                    tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.TUNE_SYNC_LOST);
                }
            }
            else
            {
                tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.RETUNE_FAILED);
            }
        }

        public void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            NetworkConditionListener tempNetworkConditionListener;
            synchronized (lock)
            {
                if (!initialized || syncNotificationsOnly)
                {
                    return;
                }
                tempNetworkConditionListener = networkConditionListener;
                if (log.isInfoEnabled())
                {
                    log.info("received notifyUntuned");
                }
            }

            tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.UNTUNED);
        }

        public void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            NetworkConditionListener tempNetworkConditionListener;
            synchronized (lock)
            {
                if (!initialized)
                {
                    return;
                }
                tempNetworkConditionListener = networkConditionListener;
                if (log.isInfoEnabled())
                {
                    log.info("received notifySyncAcquired");
                }
            }

            tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.TUNE_SYNC_ACQUIRED);
        }

        public void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance)
        {
            NetworkConditionListener tempNetworkConditionListener;
            synchronized (lock)
            {
                if (!initialized)
                {
                    return;
                }
                tempNetworkConditionListener = networkConditionListener;
                if (log.isInfoEnabled())
                {
                    log.info("received notifySyncLost");
                }
            }

            tempNetworkConditionListener.networkConditionEvent(NetworkConditionEvent.TUNE_SYNC_LOST);
        }
    }
}
