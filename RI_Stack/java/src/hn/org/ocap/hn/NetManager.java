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

package org.ocap.hn;

import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * The NetManager is a singleton class that registers all the Devices and
 * NetModules within a home network. It maintains an implementation dependent
 * database of devices and NetModules.
 * <p>
 * The NetManager may be used to retrieve list of <code>NetModule</code> and
 * <code>
 * Device</code> in the network. The application can filter the list by
 * specifying a name or by applying filtering rules. For example,
 * "modelNumber = h6315, location = LivingRoom". Application can monitor
 * availability of NetModules by registering as a listener to NetManager
 * instance.
 */
public abstract class NetManager
{

    /**
     * Returns the singleton NetManager. This is the entry point for home
     * network. If the calling application is unsigned, this method SHALL return
     * null.
     * 
     * @return Singleton instance of NetManager or null if the calling
     *         application is unsigned.
     */
    public synchronized static NetManager getInstance()
    {
        /**
         * The implementation (i.e. ManagerManager) is a privileged caller so it
         * is allowed to get a handle on the singleton instance of the
         * NetManager when initializing all of the Managers.
         */
        if (!SecurityUtil.isPrivilegedCaller())
        {
            if (!SecurityUtil.isSignedApp())
            {
                return null;
            }
        }
        return NetManagerImpl.instance();
    }

    /**
     * Returns NetModules that match all properties set by a given filter.
     * Passing a null filter will return a NetList with all known NetModules.
     * 
     * @param filter
     *            Filter to select out NetModules from all available NetModules
     * @return List of NetModules satisfying filter
     */
    public abstract NetList getNetModuleList(PropertyFilter filter);

     /**
      * Returns all NetModules that match the specified device name and module identifier.
      * Passing a null or empty device name with a non-null module identifier
      * will result in a <code>NetList</code>
      * containing all NetModules whose module ids match the non-null module identifier.
      *
      * Passing a null or empty module identifier with a non-null device name
      * will result in a <code>NetList</code> containing all NetModules whose
      * devices match the non-null device name.
      *
      * Passing a null or empty module identifer and null or empty device
      * name will return a
      * <code>NetList</code> containing all known <code>NetModule</code>s.
      *
      * @param deviceName name of the device hosting the module to retrieve
      * @param moduleID module identifier
      *
      * @return         List of NetModules satisfying device name and module identifier
      */
     public abstract NetList getNetModuleList(String deviceName, String moduleID);

    /**
     * Returns NetModule by device and module ID. If multiple devices have the same device name
     *  and share the same module identifier, then the value returned by this method is
     *  implementation specific.
     * 
     * @param deviceName
     *            name of the device hosting the module to retrieve
     * @param moduleID
     *            Device unique module ID
     * 
     * @return NetModule with the specified identifier
     */
    public abstract NetModule getNetModule(String deviceName, String moduleID);

    /**
     * Returns devices that match all properties set by a given filter. All
     * known devices and sub-devices are passed through the given filter.
     * Passing a null filter will return a NetList with all known devices and
     * sub-devices.
     * 
     * @param filter
     *            Filter to select out devices from all connected devices
     * @return List of devices satisfying filter
     */
    public abstract NetList getDeviceList(PropertyFilter filter);

     /**
      *  Returns all devices that match the specified device name.
      *  Passing a null or empty device name will result in an
      *  empty <code>NetList</code>.
      *
      *      @param name
      *                              Device name.
      *      @return         List of devices satisfying device name
      */
     public abstract NetList getDeviceList(String name);

    /**
     *  Returns device by name, for example, "BallRoom:DVD_PLAYER1".
     *  If multiple devices have the same name, then the value returned
     *  by this method is implementation specific.
     * 
     * @param name
     *            Device name
     * @return Device matching the specified name
     */
    public abstract Device getDevice(String name);

    /**
     * Adds a NetModule event listener to NetManager. Listener will receive a
     * NetModuleEvent when a new NetModule is registered or an old NetModule is
     * removed from home network. If listener is already registered, no action
     * is performed.
     * 
     * @param listener
     *            Listener which listens to NetModule change events on home
     *            network
     * @see #removeNetModuleEventListener
     */
    public abstract void addNetModuleEventListener(NetModuleEventListener listener);

    /**
     * Removes a NetModule event listener from NetManager. If the listener is
     * not registered yet, no action is performed.
     * 
     * @param listener
     *            Listener which listens to NetModule change events on home
     *            network
     * @see #addNetModuleEventListener
     */
    public abstract void removeNetModuleEventListener(NetModuleEventListener listener);

    /**
     * Adds a Device event listener to NetManager. Listener will receive a
     * DeviceEvent when a new Device is registered, an existing Device is
     * removed from home network, or a Device's internal state has changed. If
     * the listener passed in is already registered, no action is performed.
     * 
     * When a device listener is registered, the implementation SHALL NOT
     * generate DEVICE_ADDED events for devices previously discovered by the
     * implementation.
     * 
     * @param listener
     *            Listener which listens to Device change events on the home
     *            network
     * @see #removeDeviceEventListener
     */
    public abstract void addDeviceEventListener(DeviceEventListener listener);

    /**
     * Removes a Device event listener from NetManager. If the listener is not
     * registered yet, no action is performed.
     * 
     * @param listener
     *            Listener which listens to Device change events on home network
     * @see #addDeviceEventListener
     */
    public abstract void removeDeviceEventListener(DeviceEventListener listener);

    /**
     * Requests that the NetManager proactively refresh its local database of
     * connected devices. This operation will be performed asynchronously. Any
     * listeners registered with the NetManager changes to connected Devices or
     * NetModules will be notified of any changes discovered during this
     * process.
     */
    public abstract void updateDeviceList();
}
