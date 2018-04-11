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

package org.ocap.storage;

import org.ocap.storage.StorageProxy;
import org.ocap.storage.StorageManagerListener;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * This class represents the storage manager which keeps track of the storage
 * devices attached to the system.
 **/
public abstract class StorageManager
{
    /**
     * Protected default constructor.
     **/
    protected StorageManager()
    {
    }

    /**
     * Gets the singleton instance of the storage manager. The singleton MAY be
     * implemented using application or implementation scope.
     * 
     * @return The storage manager.
     **/
    public static StorageManager getInstance()
    {
        // Create the singleton instance of this manager if we have not done
        // so yet.
        if (self == null)
        {
            org.cablelabs.impl.manager.StorageManager mgr = (org.cablelabs.impl.manager.StorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
            self = mgr.getStorageManager();
        }

        return self;
    }

    /**
     * Gets the set of {@link StorageProxy} instances representing all of the
     * currently attached or embedded storage devices.
     * 
     * @return An array of StorageProxy objects. If no application accessible
     *         storage proxies are available, returns a 0 length array.
     **/
    public abstract StorageProxy[] getStorageProxies();

    /**
     * Adds a listener to receive StorageManagerEvents when a storage proxy is
     * added, removed or changes state.
     * 
     * @param listener
     *            The storage manager listener to be added.
     * 
     * @throws IllegalArgumentException
     *             if the listener parameter is null.
     **/
    public abstract void addStorageManagerListener(StorageManagerListener listener) throws IllegalArgumentException;

    /**
     * Removes a listener so that it no longer receives StorageManagerEvents
     * when storage proxies change. This method has no effect if the given
     * listener had not been added.
     * 
     * @param listener
     *            The storage manager listener to be removed.
     * 
     * @throws IllegalArgumentException
     *             if the listener parameter is null.
     **/
    public abstract void removeStorageManagerListener(StorageManagerListener listener) throws IllegalArgumentException;

    /**
     * Gets the total amount of persistent storage under the location indicated
     * by the dvb.persistent.root property and that is usable by all OCAP-J
     * applications. This value SHALL remain constant.
     * 
     * @return Amount of total persistent storage in bytes.
     */
    public abstract long getTotalPersistentStorage();

    /**
     * Gets the available amount of persistent storage under the location
     * indicated by the dvb.persistent.root property that is available to all
     * OCAP-J applications. The value returned by this method can be incorrect
     * as soon as this method returns and SHOULD be interpreted by applications
     * as an approximation.
     * 
     * @return Amount of available persistent storage in bytes.
     */
    public abstract long getAvailablePersistentStorage();

    /**
     * Adds a listener for high water mark reached in available persistent
     * storage indicated by the dvb.persistent.root property. This is a system
     * wide indication. Listeners are informed when a percentage of the total
     * persistent storage has been allocated for application use. Listeners are
     * only informed when the high water mark is reached or exceeded.
     * 
     * @param listener
     *            The listener to add.
     * @param highWaterMark
     *            Percentage of the available persistent storage remaining when
     *            the listener is to be informed. For instance, if the total
     *            available persistent storage is 1MB and the high water mark is
     *            75 then high water listeners will be informed when 750KB have
     *            been allocated for application use.
     * 
     * @throws IllegalArgumentException
     *             if the listener parameter could not be added or is null.
     */
    public abstract void addAvailableStorageListener(AvailableStorageListener listener, int highWaterMark);

    /**
     * Removes an available storage listener that was registered using the
     * <code>addAvailableStorageListener</code> method. If the parameter is not
     * currently registered this method does nothing successfully.
     * 
     * @param listener
     *            The listener to remove.
     * 
     * @throws IllegalArgumentException
     *             if the parameter is null.
     */
    public abstract void removeAvailableStorageListener(AvailableStorageListener listener);

    // The singleton instance of this manager
    private static StorageManager self = null;
}
