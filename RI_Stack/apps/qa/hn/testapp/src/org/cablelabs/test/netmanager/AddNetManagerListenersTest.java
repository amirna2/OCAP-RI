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

package org.cablelabs.test.netmanager;

import org.cablelabs.test.Test;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;

/**
 * AddNetManagerListenersTest - This test creates a
 * <code>TestNetManagerEventListener</code> object which is both a
 * <code>DeviceEventListener</code> and <code>NetModuleEventListener</code>.
 * These listener is then passed into
 * {@link org.ocap.hn.NetManager#addDeviceEventListener(DeviceEventListener)}
 * and
 * {@link org.ocap.hn.NetManager#addNetModuleEventListener(NetModuleEventListener)}
 * methods. Then the test verifies that listener received
 * <code>DeviceEvents</code> and <code>NetModuleEvents</code> which results in
 * the test passing otherwise it fails. The test will also fail if it is unable
 * to obtain a handle to the <code>NetManager</code>. The test will time out
 * after two minutes if there are not any <code>Devices</code> or
 * <code>NetModules</code> added to or removed from the network.
 * 
 * Note: In order to run this test, another UPnP device will need to be started
 * up on the network after the test begins executing (i.e. launch the simulator
 * from a second machine). A log message will appear in the trace window
 * indicating that the new device should be started.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 */
public class AddNetManagerListenersTest extends Test
{
    private String failMessage = "";

    private NetManager netManagerInstance = null;

    public int clean()
    {
        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {

        this.testLogger.setPrefix("AddNetManagerListenersTest: ");
        this.testLogger.log("Executing Test: " + this.getName());

        if (executeTest())
        {
            this.testLogger.log("test PASSED!");

            return Test.TEST_STATUS_PASS;
        }
        else
        {
            this.testLogger.log("test FAILED! - reason: " + failMessage);

            return Test.TEST_STATUS_FAIL;
        }
    }

    public int prepare()
    {
        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "netmanager.AddNetManagerListenersTest";
    }

    public String getDescription()
    {
        return "This is the NetManager AddNetManagerListenersTest";
    }

    private boolean executeTest()
    {
        this.testLogger.log("Obtaining an instance of the NetManager");
        netManagerInstance = NetManager.getInstance();

        if (netManagerInstance == null)
        {
            failMessage = "unable to get an instance of NetManager";
            return false;
        }

        this.testLogger.log("Creating TestNetManagerEventListener");

        TestNetManagerEventListener netManagerEventListener = new TestNetManagerEventListener(
                "TestNetManagerEventListener");

        this.testLogger.log("Adding " + netManagerEventListener.getName()
                + " as a listener for Device and NetModule events");

        netManagerInstance.addDeviceEventListener(netManagerEventListener);
        netManagerInstance.addNetModuleEventListener(netManagerEventListener);

        if (waitForDevicesAndNetModules("Time to add test device to network ...") <= 0)
        {
            failMessage = "device was never added to the network";
            return false;
        }
        else if (!netManagerEventListener.isNotifiedOfDeviceEvent())
        {
            failMessage = netManagerEventListener.getName() + " did NOT recieve DeviceEvent";
            return false;
        }
        else if (!netManagerEventListener.isNotifiedOfNetModuleEvent())
        {
            failMessage = netManagerEventListener.getName() + " did NOT recieve NetModuleEvent";
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * waitForDevicesAndNetModules - this method is used for monitoring the
     * <code>Devices</code> and <code>NetModules</code> on the network. The
     * method has a time-out period of two minutes. If the number of
     * <code>Devices</code> or <code>NetModules</code> does not change in that
     * time than the method times out and returns a result of 0. Otherwise a
     * positive value is returned.
     * 
     * @return value indicating the number of retries that were remaining when
     *         the method exited.
     */
    private int waitForDevicesAndNetModules(String message)
    {
        int retryCount = 8;

        int initialNumberOfDevices = netManagerInstance.getDeviceList(null).size();
        int intiialNumberofNetModules = netManagerInstance.getNetModuleList(null).size();

        while (retryCount > 0)
        {
            this.testLogger.log(message);

            try
            {
                Thread.sleep(15000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            // Indicates that the number of Devices and NetModules on the
            // network has changed which means that Device and NetModule events
            // have been generated.
            if (netManagerInstance.getDeviceList(null).size() != initialNumberOfDevices
                    || netManagerInstance.getNetModuleList(null).size() != intiialNumberofNetModules)
            {
                break;
            }

            retryCount--;
        }

        return retryCount;
    }

    /**
     * TestNetManagerEventListener - inner class that is a DeviceEventListener
     * and NetModuleEventListener. This class enables the test to create
     * multiple listener instances.
     */
    class TestNetManagerEventListener implements DeviceEventListener, NetModuleEventListener
    {
        private String listenerName;

        private boolean isNotifiedOfDeviceEvent;

        private boolean isNotifiedOfNetModuleEvent;

        TestNetManagerEventListener(String listenerName)
        {
            this.listenerName = listenerName;

            isNotifiedOfDeviceEvent = false;
            isNotifiedOfNetModuleEvent = false;

            testLogger.log("Creating TestNetManagerEventListener: " + listenerName);
        }

        public void notify(DeviceEvent event)
        {
            testLogger.log("TestNetManagerEventListener.notify() for DeviceEvent called on " + listenerName);
            testLogger.log("DeviceEvent - " + event.getType());

            isNotifiedOfDeviceEvent = true;
        }

        public void notify(NetModuleEvent event)
        {
            testLogger.log("TestNetManagerEventListener.notify() for NetModuleEvent called on " + listenerName);
            testLogger.log("NetModuleEvent - " + event.getType());

            isNotifiedOfNetModuleEvent = true;
        }

        /**
         * getName - returns the name assigned to this listener
         * 
         * @return name of the listener
         */
        String getName()
        {
            return listenerName;
        }

        /**
         * isNotifiedOfDeviceEvent - indicates if the listener has been notified
         * of a <code>DeviceEvent</code>.
         */
        boolean isNotifiedOfDeviceEvent()
        {
            return isNotifiedOfDeviceEvent;
        }

        /**
         * isNotifiedOfNetModuleEvent - indicates if the listener has been
         * notified of a <code>NetModuleEvent</code>.
         */
        boolean isNotifiedOfNetModuleEvent()
        {
            return isNotifiedOfNetModuleEvent;
        }

        /**
         * resetNotifications - reset <code>DeviceEvent</code> and
         * <code>NetModuleEvent</code> notification flags.
         */
        void resetNotifications()
        {
            isNotifiedOfDeviceEvent = false;
            isNotifiedOfNetModuleEvent = false;
        }
    }
}
